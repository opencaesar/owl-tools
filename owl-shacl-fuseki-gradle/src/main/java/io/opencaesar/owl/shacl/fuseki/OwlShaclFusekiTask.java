package io.opencaesar.owl.shacl.fuseki;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.Incremental;

public abstract class OwlShaclFusekiTask extends DefaultTask {

	private final static Logger LOGGER = Logger.getLogger(OwlShaclFusekiTask.class);

	static {
		DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("owlshacl.log4j2.properties"));
	}

	@Input
	public abstract Property<String> getEndpointURL();

    // contributes to the input files
	public File queryPath;

	@SuppressWarnings({ "unused", "deprecation" })
	public void setQueryPath(File path) throws IOException {
		queryPath = path;
		final List<File> files = new ArrayList<>();
		if (null == queryPath || !queryPath.exists() || !queryPath.canRead())
			LOGGER.warn("OwlShacl("+getName()+"): queryPath is not an existing, readable input file or directory got: " + path);
		else if (queryPath.isFile())
			files.add(queryPath);
		else
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
	protected abstract ConfigurableFileCollection getInputFiles();

	@OutputDirectory
	public abstract DirectoryProperty getResultPath();

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
