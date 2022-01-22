package io.opencaesar.owl.query;

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
import org.gradle.api.tasks.*;
import org.gradle.work.Incremental;

/**
 * Gradle incremental build support is available for an internal input file collection
 * derived from the queryPath property.
 */
public abstract class OwlQueryTask extends DefaultTask {

	@Input
	public String endpointURL;

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
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path.toPath(), "*.sparql")) {
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
	public String resultPath;

	@Input
	public String format;

	public boolean debug;
    
    @TaskAction
    public void run() {
		final ArrayList<String> args = new ArrayList<>();
		if (endpointURL != null) {
			args.add("-e");
			args.add(endpointURL);
		}
		if (queryPath != null) {
			args.add("-q");
			args.add(queryPath.getAbsolutePath());
		}
		if (resultPath != null) {
			args.add("-r");
			args.add(resultPath);
		}
		if (format != null) {
			args.add("-f");
			args.add(format);
		}
		if (debug) {
			args.add("-d");
		}
		try {
			OwlQueryApp.main(args.toArray(new String[0]));
		} catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
		}
    }
}