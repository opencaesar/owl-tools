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
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.Incremental;

public abstract class OwlShaclFusekiTask extends DefaultTask {

	@Input
	public abstract Property<String> getEndpointURL();

	private File queryPath;

	@Input
	public File getQueryPath() { return queryPath; }

	public void setQueryPath(File path) {
		queryPath = path;
		calculateInputOutputFiles();
	}

	private File resultPath;

	@Internal
	public File getResultPath() { return resultPath; }

	@SuppressWarnings("unused")
	public void setResultPath(File p) {
		resultPath = p;
		calculateInputOutputFiles();
	}

	@Incremental
	@InputFiles
	protected abstract ConfigurableFileCollection getInputFiles();

	@Incremental
	@OutputFiles
	protected abstract ConfigurableFileCollection getOutputFiles();

	protected void calculateInputOutputFiles() {
		if (null != queryPath && null != resultPath) {
			final List<File> inputFiles = new ArrayList<>();
			final List<File> outputFiles = new ArrayList<>();
			if (queryPath.isFile()) {
				inputFiles.add(queryPath);
			} else {
				// See https://docs.oracle.com/javase/8/docs/api/java/nio/file/DirectoryStream.html
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(queryPath.toPath(), "*.shacl")) {
					for (Path entry : stream) {
						inputFiles.add(entry.toFile());
					}
				} catch (DirectoryIteratorException|IOException ex) {
					// Ignore: no input.
				}
			}
			getInputFiles().setFrom(inputFiles);
			Path outputFolder = resultPath.toPath();
			inputFiles.forEach(f -> {
				String ifn = f.getName();
				int i = ifn.lastIndexOf('.');
				String ofn = ((i> 0) ? ifn.substring(0, i+1) : ifn+'.') + OwlShaclFusekiApp.OUTPUT_FORMAT;
				outputFiles.add(outputFolder.resolve(ofn).toFile());
			});
			getOutputFiles().setFrom(outputFiles);
		}
	}

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
		if (null != resultPath) {
			args.add("-r");
			args.add(resultPath.getAbsolutePath());
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
