package io.opencaesar.owl.load;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.*;
import org.gradle.work.Incremental;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

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
     * The optional gradle task load-to-default-graph property (default is false).
     *
     * @return Boolean Property
     */
    @Optional
    @Input
    public abstract Property<Boolean> getLoadToDefaultGraph();

    /**
     * The optional username for authenticating the SPARQL endpoint.
     *
     * @return String Property
     */
    @Optional
    @Input
    public abstract Property<String> getAuthenticationUsername();

    /**
     * The optional password for authenticating the SPARQL endpoint.
     *
     * @return String Property
     */
    @Optional
    @Input
    public abstract Property<String> getAuthenticationPassword();

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
        try {
            Map<String, String> env = new HashMap<>();
            if (getAuthenticationUsername().isPresent()) {
                String username = getAuthenticationUsername().get();
                env.put("OWL_LOAD_USERNAME", username);
            }
            if (getAuthenticationPassword().isPresent()) {
                String password = getAuthenticationPassword().get();
                env.put("OWL_LOAD_PASSWORD", password);
            }

            // Prepare the command to start a new JVM with OwlLoadApp
            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-cp");
            command.add(System.getProperty("java.class.path")); // current classpath
            command.add("io.opencaesar.owl.load.OwlLoadApp"); // main class

            getIris().get().forEach(iri -> {
                command.add("-i");
                command.add(iri);
            });
            if (getCatalogPath().isPresent()) {
                command.add("-c");
                command.add(getCatalogPath().get().getAbsolutePath());
            }
            if (getLoadToDefaultGraph().isPresent() && getLoadToDefaultGraph().get()) {
                command.add("--default");
            }
            if (getEndpointURL().isPresent()) {
                command.add("-e");
                command.add(getEndpointURL().get());
            }
            if (getFileExtensions().isPresent()) {
                getFileExtensions().get().forEach(ext -> {
                    command.add("-f");
                    command.add(ext);
                });
            }
            if (getDebug().isPresent() && getDebug().get()) {
                command.add("-d");
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.environment().putAll(env); // Set environment variables

            // Start a new process and wait for it to finish
            Process process = processBuilder.start();
            process.waitFor();

            // Check the process's exit value and handle errors if necessary
            if (process.exitValue() != 0) {
                // handle the error or throw an exception
                throw new GradleException("Execution of OwlLoadApp failed.");
            }

        } catch (IOException | InterruptedException e) {
            // handle exception
            throw new GradleException("Execution of OwlLoadApp failed", e);
        }
    }
}
