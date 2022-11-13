package io.opencaesar.owl.load;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.Incremental;

/**
 * Gradle incremental build support is available for an internal input file collection
 * derived from the catalogPath and fileExtension properties.
 */
public abstract class OwlLoadTask extends DefaultTask {

	/**
	 * Creates a new OwlLoadTask object
	 */
	public OwlLoadTask() {
	}
	
    /**
     * The required gradle task list of ontology IRIs property.
     * 
     * @return List of Strings
     */
    @Input
    public abstract ListProperty<String> getIris();

    /**
     * The required gradle task Fuseki server endpoint URL property.
     * 
     * @return String Property
     */
    @Input
    public abstract Property<String> getEndpointURL();

    /**
     * The required gradle task OASIS XML catalog path property.
     * 
     * @return File Property
     */
	@Input
    public abstract Property<File> getCatalogPath();

    /**
     * The optional gradle task ontology file extensions property (default list is owl and ttl).
     * 
     * @return List of Strings Property
     */
    @Optional
	@Input
    public abstract ListProperty<String> getFileExtensions();

    /**
     * The optional gradle task debug property (default is false).
     * 
     * @return Boolean Property
     */
    @Optional
    @Input
    public abstract Property<Boolean> getDebug();

    /**
     * The list of gradle task input files derived from all the files in
     *         the input catalog that have one of the file extensions.
     *         
     * @return ConfigurableFileCollection
     * @throws IOException error
     * @throws URISyntaxException error
     */
    @Incremental
    @InputFiles
	@SuppressWarnings("deprecation")
    protected ConfigurableFileCollection getInputFiles() throws IOException, URISyntaxException {
		if (getCatalogPath().isPresent() && getCatalogPath().get().exists()) {
			final var catalogURI = getCatalogPath().get().toURI();
			final var inputCatalog = OwlCatalog.create(catalogURI);

			final var inputFileExtensions = !getFileExtensions().get().isEmpty() ? getFileExtensions().get() : Arrays.asList(OwlLoadApp.DEFAULT_EXTENSIONS);
			final var inputFiles = inputCatalog.getFileUris(inputFileExtensions).stream().map(f-> new File(f)).collect(Collectors.toList());

			return getProject().files(inputFiles);
        }
		return getProject().files(Collections.EMPTY_LIST);
    }

    /**
     * The gradle output file property derived from the task name with a `.log` suffix in the gradle build folder.
     * 
     * @return RegularFile Provider
     */
	@OutputFile
    @SuppressWarnings("deprecation")
    protected Provider<RegularFile> getOutputFile() {
        return getProject()
                .getLayout()
                .getBuildDirectory()
                .file("log/" + getTaskIdentity().name + ".log");
    }

    /**
     * The gradle task action logic.
     */
    @TaskAction
    public void run() {
        final ArrayList<String> args = new ArrayList<>();
        getIris().get().forEach(iri -> {
            args.add("-i");
            args.add(iri);
        });
        if (getCatalogPath().isPresent()) {
            args.add("-c");
            args.add(getCatalogPath().get().getAbsolutePath());
        }
        if (getEndpointURL().isPresent()) {
            args.add("-e");
            args.add(getEndpointURL().get());
        }
        if (getFileExtensions().isPresent()) {
        	getFileExtensions().get().forEach((String ext) -> {
                args.add("-f");
                args.add(ext);
            });
        }
        if (getDebug().isPresent() && getDebug().get()) {
            args.add("-d");
        }
        try {
            OwlLoadApp.main(args.toArray(new String[0]));
            // Generate a unique output for gradle incremental execution support.
            generateLog();
        } catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
        }
    }
    
    private void generateLog() throws IOException, URISyntaxException {
        if (getOutputFile().isPresent()) {
            File output = getOutputFile().get().getAsFile();
            try (PrintStream ps = new PrintStream(new FileOutputStream(output))) {
                for (File file : getInputFiles().getFiles()) {
                    ps.println(file.getAbsolutePath());
                }
            }
        }
    }
}
