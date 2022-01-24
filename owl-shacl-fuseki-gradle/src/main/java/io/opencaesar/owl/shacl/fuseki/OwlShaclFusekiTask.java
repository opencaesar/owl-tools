package io.opencaesar.owl.shacl.fuseki;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.Incremental;

public abstract class OwlShaclFusekiTask extends DefaultTask {

	@Input
	public abstract Property<String> getEndpointURL();

	private File queryPath;

	@InputDirectory
	public File getQueryPath() {
		return queryPath;
	}

	public void setQueryPath(File path) throws IOException {
		if (null == path || !path.exists() || !path.isDirectory() || !path.canExecute() || !path.canRead())
			throw new GradleException("queryPath must be an existing, executable and readable input directory, got: " + path);
		queryPath = path;
		final List<File> files = new ArrayList<>();
		// See https://docs.oracle.com/javase/8/docs/api/java/nio/file/DirectoryStream.html
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path.toPath(), "*.shacl")) {
			for (Path entry : stream) {
				files.add(entry.toFile());
			}
		} catch (DirectoryIteratorException ex) {
			throw ex.getCause();
		}
		getInputFiles().setFrom(files);
	}

	@Incremental
	@InputFiles
	public abstract ConfigurableFileCollection getInputFiles();

	@OutputDirectory
	public abstract RegularFileProperty getResultPath();

	@Input
	@Optional
	public abstract Property<Boolean> getDebug();
    
    @TaskAction
    public void run() {
		final ArrayList<String> args = new ArrayList<>();
		if (getEndpointURL().isPresent()) {
			args.add("-e");
			args.add(getEndpointURL().get());
		}
		if (null != queryPath) {
			args.add("-q");
			args.add(queryPath.getAbsolutePath());
		}
		if (getResultPath().isPresent()) {
			args.add("-r");
			args.add(getResultPath().get().getAsFile().getAbsolutePath());
		}
		if (getDebug().isPresent() && getDebug().get()) {
			args.add("-d");
		}
		try {
			OwlShaclFusekiApp.main(args.toArray(new String[0]));
		} catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
		}
    }
}