package io.opencaesar.owl.reason;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.opencaesar.oml.util.OmlCatalog;
import org.eclipse.emf.common.util.URI;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.Incremental;

public abstract class OwlReasonTask extends DefaultTask {

	private File catalogPath;

	@InputFile
	public File getCatalogPath() {
		return catalogPath;
	}

	public void setCatalogPath(File f) throws IOException, URISyntaxException {
		catalogPath = f;
		calculateInputFiles();
	}

	private List<String> inputFileExtensions;

	@Input
	public List<String> getInputFileExtensions() {
		return inputFileExtensions;
	}

	public void setInputFileExtensions(List<String> fes) throws IOException, URISyntaxException {
		inputFileExtensions = fes;
		calculateInputFiles();
	}

	private void calculateInputFiles() throws IOException, URISyntaxException {
		if (null != catalogPath && null != inputFileExtensions) {
			OmlCatalog inputCatalog = OmlCatalog.create(URI.createFileURI(catalogPath.getAbsolutePath()));
			final ArrayList<File> owlFiles = new ArrayList<>();
			for (URI uri : inputCatalog.getFileUris(inputFileExtensions)) {
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
	public abstract Property<String> getInputOntologyIri();

	@Input
	public abstract ListProperty<String> getSpecs();

	@OutputFile
	public abstract RegularFileProperty getReportPath();

	@Input
	public abstract Property<String> getOutputFileExtension();

	@Input
	@Optional
	public abstract Property<Boolean> getRemoveUnsats();

	@Input
	@Optional
	public abstract Property<Boolean> getRemoveBackbone();

	@Input
	@Optional
	public abstract Property<String> getBackboneIri();

	@Input
	@Optional
	public abstract Property<Integer> getIndent();

	@Input
	@Optional
	public abstract Property<Boolean> getDebug();

    @TaskAction
    public void run() {
		final ArrayList<String> args = new ArrayList<>();
		if (catalogPath != null) {
			args.add("-c");
			args.add(catalogPath.getAbsolutePath());
		}
		if (getInputOntologyIri().isPresent()) {
			args.add("-i");
			args.add(getInputOntologyIri().get());
		}
		getSpecs().get().forEach((String spec) -> {
			args.add("-s");
			args.add(spec);
		});
		if (getReportPath().isPresent()) {
			args.add("-r");
			args.add(getReportPath().get().getAsFile().getAbsolutePath());
		}
		if (null != inputFileExtensions) {
			inputFileExtensions.forEach((String ext) -> {
				args.add("-if");
				args.add(ext);
			});
		}
		if (getOutputFileExtension().isPresent()) {
			args.add("-of");
			args.add(getOutputFileExtension().get());
		}
		if (getRemoveUnsats().isPresent() && getRemoveUnsats().get()) {
			args.add("-ru");
		}
		if (getRemoveBackbone().isPresent() && getRemoveBackbone().get()) {
			args.add("-rb");
		}
		if (getBackboneIri().isPresent()) {
			args.add("-b");
			args.add(getBackboneIri().get());
		}
		if (getIndent().isPresent()) {
			args.add("-n");
			args.add(getIndent().get().toString());
		}
		if (getDebug().isPresent() && getDebug().get()) {
			args.add("-d");
		}
		try {
			OwlReasonApp.main(args.toArray(new String[0]));
		} catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
		}
	}

}
