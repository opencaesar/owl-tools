package io.opencaesar.owl.shacl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.Incremental;

/**
 * Gradle task for running SHACLE rules on a dataset
 */
public abstract class OwlShaclTask extends DefaultTask {

	/**
	 * Creates a new OwlShaclTask object
	 */
	public OwlShaclTask() {
	}

	/**
	 * The required gradle task input endpoint URL.
	 * 
	 * @return String Property
	 */
	@Input
	public abstract Property<String> getEndpointURL();

	/**
	 * The required gradle task input query path where to search for *.shacl files.
	 * 
	 * @return File Property
	 */
	@Input
	public abstract Property<File> getQueryPath();

	/**
	 * The required gradle task output result path where to save query results.
	 * 
	 * @return File Property
	 */
	@Input
	public abstract Property<File> getResultPath();

	/**
	 * The optional gradle task debug property (default is false).
	 * 
	 * @return Boolean Property
	 */
	@Input
	@Optional
	public abstract Property<Boolean> getDebug();

	/**
	 * The collection of *.shacl files found in the query folder.
	 * 
	 * @return ConfigurableFileCollection.
	 * @throws IOException error
	 */
	@Incremental
	@InputFiles
	@SuppressWarnings("deprecation")
	protected ConfigurableFileCollection getInputFiles() throws IOException {
		if (getQueryPath().isPresent()) {
			final List<File> inputFiles = new ArrayList<>();
			if (getQueryPath().get().isFile()) {
				inputFiles.add(getQueryPath().get());
			} else {
				for (Path entry : Files.newDirectoryStream(getQueryPath().get().toPath(), "*.shacl")) {
					inputFiles.add(entry.toFile());
				}
			}
			return getProject().files(inputFiles);
		}
		return getProject().files(Collections.EMPTY_LIST);
	}

	/**
	 * The collection of query result files corresponding to each *.shacl file found in the query folder.
	 * 
	 * @return ConfigurableFileCollection
	 * @throws IOException error
	 */
	@OutputFiles
	@SuppressWarnings("deprecation")
	protected ConfigurableFileCollection getOutputFiles() throws IOException {
		if (getQueryPath().isPresent() && getResultPath().isPresent()) {
			String extension = OwlShaclApp.OUTPUT_FORMAT;
			final List<File> outputFiles = new ArrayList<>();
			if (getQueryPath().get().isFile()) {
				outputFiles.add(replacePathAndExtension(getQueryPath().get(), getResultPath().get(), extension));
			} else {
				for (Path entry : Files.newDirectoryStream(getQueryPath().get().toPath(), "*.sparql")) {
					outputFiles.add(replacePathAndExtension(entry.toFile(), getResultPath().get(), extension));
				}
			}
			return getProject().files(outputFiles);
		}
		return getProject().files(Collections.EMPTY_LIST);
	}

	private File replacePathAndExtension(File file, File newPath, String newExt) {
		String name = file.getName();
		int index = name.lastIndexOf('.');
		String newName = name.substring(0, index) + "." + newExt;
		return new File(newPath + File.separator + newName);
	}

	/**
	 * The gradle task action logic.
	 */
    @TaskAction
    public void run() {
		final ArrayList<String> args = new ArrayList<>();
		if (getEndpointURL().isPresent()) {
			args.add("-e");
			args.add(getEndpointURL().get());
		}
		if (getQueryPath().isPresent()) {
			args.add("-q");
			args.add(getQueryPath().get().getAbsolutePath());
		}
		if (getResultPath().isPresent()) {
			args.add("-r");
			args.add(getResultPath().get().getAbsolutePath());
		}
		if (getDebug().isPresent() && getDebug().get()) {
			args.add("-d");
		}
		try {
			OwlShaclApp.main(args.toArray(new String[0]));
		} catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
		}
    }
}
