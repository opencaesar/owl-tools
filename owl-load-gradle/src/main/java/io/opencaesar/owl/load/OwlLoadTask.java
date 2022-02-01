package io.opencaesar.owl.load;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.opencaesar.oml.util.OmlCatalog;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.emf.common.util.URI;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.Incremental;

/**
 * Gradle incremental build support is available for an internal input file collection
 * derived from the catalogPath and fileExtension properties.
 */
public abstract class OwlLoadTask extends DefaultTask {

    private final static Logger LOGGER = Logger.getLogger(OwlLoadTask.class);

    static {
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("owlload.log4j2.properties"));
    }

    @Input
    public abstract ListProperty<String> getIris();

    @Input
    public abstract Property<String> getEndpointURL();

    @InputFiles
    @Optional
    public abstract Property<Task> getInferredTaskDependency();

    private File catalogPath;

    @Internal
    public File getCatalogPath() { return catalogPath; }

    @SuppressWarnings("unused")
    public void setCatalogPath(File f) throws IOException, URISyntaxException {
        catalogPath = f;
        calculateInputFiles();
    }

    private List<String> fileExtensions;

    @Internal
    public List<String> getFileExtensions() { return fileExtensions; }

    @SuppressWarnings("unused")
    public void setFileExtensions(List<String> fes) throws IOException, URISyntaxException {
        fileExtensions = fes;
        calculateInputFiles();
    }

    private static final Comparator<File> fileComparator = Comparator.comparing(File::getAbsolutePath);

    private void calculateInputFiles() throws IOException, URISyntaxException {
        if (null != getCatalogPath() && null != getFileExtensions()) {
            OmlCatalog inputCatalog = OmlCatalog.create(URI.createFileURI(getCatalogPath().getAbsolutePath()));
            final ArrayList<File> owlFiles = new ArrayList<>();
            for (URI uri : inputCatalog.getFileUris(getFileExtensions())) {
                File file = new File(new URL(uri.toString()).toURI().getPath());
                owlFiles.add(file);
            }
            owlFiles.sort(fileComparator);
            LOGGER.debug("OwlLoad("+getName()+") calculateInputFiles found: "+owlFiles.size());
            for (File owlFile : owlFiles) {
                LOGGER.debug("OwlLoad("+getName()+") input: "+owlFile);
            }
            getInputFiles().setFrom(owlFiles);
        }
    }

    @Incremental
    @InputFiles
    public abstract ConfigurableFileCollection getInputFiles();

    /**
     * Since this Gradle property is configured by the task constructor, it is not publicly exposed to users.
     * @return The configured output file.
     */
    @OutputFile
    protected abstract RegularFileProperty getOutputFile();

    @Input
    @Optional
    public abstract Property<Boolean> getDebug();

    /**
     * Use the task name as part of the output filename
     * to ensure that each instance of OwlLoadTask
     * has a corresponding unique output file
     */
    public OwlLoadTask() {
        RegularFile f = getProject()
                .getLayout()
                .getBuildDirectory()
                .file("owl-load." + getTaskIdentity().name + ".log")
                .get();
        LOGGER.info("OwlLoad("+getName()+") Configure outputFile = "+f.getAsFile());
        getOutputFile().value(f);
    }

    @TaskAction
    public void run() {
        final ArrayList<String> args = new ArrayList<>();
        getIris().get().forEach(iri -> {
            args.add("-i");
            args.add(iri);
        });
        if (null != catalogPath) {
            args.add("-c");
            args.add(catalogPath.getAbsolutePath());
        }
        if (getEndpointURL().isPresent()) {
            args.add("-e");
            args.add(getEndpointURL().get());
        }
        if (null != fileExtensions) {
            fileExtensions.forEach((String ext) -> {
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
            if (getOutputFile().isPresent()) {
                File output = getOutputFile().get().getAsFile();
                LOGGER.info("OwlLoad("+getName()+") Generate output file: " + output);
                try (PrintStream ps = new PrintStream(new FileOutputStream(output))) {
                    for (File file : getInputFiles().getFiles()) {
                        ps.println(file.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
        }
    }
}
