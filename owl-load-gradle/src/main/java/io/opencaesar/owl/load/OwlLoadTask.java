package io.opencaesar.owl.load;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import io.opencaesar.oml.util.OmlCatalog;
import org.eclipse.emf.common.util.URI;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.Incremental;

/**
 * Gradle incremental build support is available for an internal input file collection
 * derived from the catalogPath and fileExtension properties.
 */
public abstract class OwlLoadTask extends DefaultTask {

    @Input
    public abstract ListProperty<String> getIris();

    @Input
    public abstract Property<String> getEndpointURL();

    private File catalogPath;

    @InputFile
    public File getCatalogPath() {
        return catalogPath;
    }

    public void setCatalogPath(File f) throws IOException, URISyntaxException {
        catalogPath = f;
        calculateInputFiles();
    }

    private ListProperty<String> fileExtensions;

    @Input
    public ListProperty<String> getFileExtensions() {
        return fileExtensions;
    }

    public void setFileExtensions(ListProperty<String> fes) throws IOException, URISyntaxException {
        fileExtensions = fes;
        calculateInputFiles();
    }

    private void calculateInputFiles() throws IOException, URISyntaxException {
        if (null != catalogPath) {
            OmlCatalog inputCatalog = OmlCatalog.create(URI.createFileURI(catalogPath.getAbsolutePath()));
            final ArrayList<File> owlFiles = new ArrayList<>();
            for (URI uri : inputCatalog.getFileUris(fileExtensions.get())) {
                File file = new File(new URL(uri.toString()).toURI().getPath());
                owlFiles.add(file);
            }
            getInputFiles().setFrom(owlFiles);
        }
    }

    @Incremental
    @InputFiles
    public abstract ConfigurableFileCollection getInputFiles();

    @Input
    public abstract Property<Boolean> getDebug();

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
            fileExtensions.get().forEach((String ext) -> {
                args.add("-f");
                args.add(ext);
            });
        }
        if (getDebug().isPresent() && getDebug().get()) {
            args.add("-d");
        }
        try {
            OwlLoadApp.main(args.toArray(new String[0]));
        } catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
        }
    }
}