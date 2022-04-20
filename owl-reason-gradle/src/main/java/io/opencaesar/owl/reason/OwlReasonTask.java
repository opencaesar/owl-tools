package io.opencaesar.owl.reason;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.Incremental;

public abstract class OwlReasonTask extends DefaultTask {

	@Input
    public abstract Property<File> getCatalogPath();

	@Input
	public abstract ListProperty<String> getInputFileExtensions();

	@Input
	public abstract ListProperty<String> getSpecs();

    @Optional
    @Input
    public abstract Property<String> getOutputFileExtension();

	@Input
	public abstract Property<String> getInputOntologyIri();

	@Optional
	@Input
	public abstract Property<String> getExplanationFormat();

	@OutputFile
	public abstract RegularFileProperty getReportPath();

	@Optional
	@Input
	public abstract Property<Boolean> getRemoveUnsats();

	@Optional
	@Input
	public abstract Property<Boolean> getRemoveBackbone();

	@Optional
	@Input
	public abstract Property<String> getBackboneIri();

	@Optional
	@Input
	public abstract Property<Integer> getIndent();

	@Optional
	@Input
	public abstract Property<Boolean> getDebug();

	@Incremental
	@InputFiles
	@SuppressWarnings("deprecation")
	protected ConfigurableFileCollection getInputFiles() throws IOException, URISyntaxException {
		if (getCatalogPath().isPresent() && getCatalogPath().get().exists() && !getSpecs().get().isEmpty()) {
			final var catalogURI = getCatalogPath().get().toURI();
			final var inputCatalog = OwlCatalog.create(catalogURI);
			
			final var inputFileExtensions = !getInputFileExtensions().get().isEmpty() ? getInputFileExtensions().get() : Collections.singletonList(OwlReasonApp.DEFAULT_INPUT_FILE_EXTENSION);
			final var inputFiles = inputCatalog.getFileUris(inputFileExtensions).stream().map(f-> new File(f)).collect(Collectors.toList());
			
			final var outputFileExtension = getOutputFileExtension().getOrElse(OwlReasonApp.DEFAULT_OUTPUT_FILE_EXTENSION);
			final var outputIris = getSpecs().get().stream().map(s-> s.split("=")[0].trim()+"."+outputFileExtension).collect(Collectors.toList());
			final var outputFiles = outputIris.stream().map(i-> new File(inputCatalog.resolve(i))).collect(Collectors.toList());
			
			inputFiles.removeAll(outputFiles);
			inputFiles.add(new File(catalogURI));
			
			return getProject().files(inputFiles);
		}
		return getProject().files(Collections.EMPTY_LIST);
	}

	@OutputFiles
	@SuppressWarnings("deprecation")
	protected ConfigurableFileCollection getOutputFiles() throws IOException, URISyntaxException {
		if (getCatalogPath().isPresent() && getCatalogPath().get().exists() && !getSpecs().get().isEmpty()) {
			final var catalogURI = getCatalogPath().get().toURI();
			final var inputCatalog = OwlCatalog.create(catalogURI);
			
			final var outputFileExtension = getOutputFileExtension().getOrElse(OwlReasonApp.DEFAULT_OUTPUT_FILE_EXTENSION);
			final var outputIris = getSpecs().get().stream().map(s-> s.split("=")[0].trim()+"."+outputFileExtension).collect(Collectors.toList());
			final var outputFiles = outputIris.stream().map(i-> new File(inputCatalog.resolve(i))).collect(Collectors.toList());
			
			return getProject().files(outputFiles);
		}
		return getProject().files(Collections.EMPTY_LIST);
	}

	@TaskAction
    public void run() {
		final ArrayList<String> args = new ArrayList<>();
		if (getCatalogPath().isPresent()) {
			args.add("-c");
			args.add(getCatalogPath().get().getAbsolutePath());
		}
		if (getInputOntologyIri().isPresent()) {
			args.add("-i");
			args.add(getInputOntologyIri().get());
		}
		if (getSpecs().isPresent()) {
			getSpecs().get().forEach((String spec) -> {
				args.add("-s");
				args.add(spec);
			});
		}
		if (getReportPath().isPresent()) {
			args.add("-r");
			args.add(getReportPath().get().getAsFile().getAbsolutePath());
		}
		if (getInputFileExtensions().isPresent()) {
			getInputFileExtensions().get().forEach((String ext) -> {
				args.add("-if");
				args.add(ext);
			});
		}
		if (getOutputFileExtension().isPresent()) {
			args.add("-of");
			args.add(getOutputFileExtension().get());
		}
		if (getExplanationFormat().isPresent()) {
			args.add("-ef");
			args.add(getExplanationFormat().get());
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
