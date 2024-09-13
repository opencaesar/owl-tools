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

/**
 * Gradle task for reasoning about an input ontology and all of the ontologies whose import IRIs
 * can be resolved using an OASIS XML catalog.
 */
public abstract class OwlReasonTask extends DefaultTask {

	/**
	 * Creates a new OwlReasonTask object
	 */
	public OwlReasonTask() {
	}

	/**
	 * Path to an OASIS XML catalog for OWL files (Optional)
	 * 
	 * @return File Property
	 */
	@Input
    public abstract Property<File> getCatalogPath();

	/**
	 * List of input file extensions (Optional, owl by default
	 * 		 options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss)
	 *         
	 * @return List of Strings Proprty
	 */
	@Input
	public abstract ListProperty<String> getInputFileExtensions();

	/**
	 * List of specifications for output ontologies having specific entailment statement types (format is IRI=statement-types)
	 * 
	 * @return List of Strings Property
	 */
	@Input
	public abstract ListProperty<String> getSpecs();

	/**
	 * File extension for the output files containing ontology entailment (default is ttl).
	 *         options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss
	 *         
	 * @return String Property
	 */
    @Optional
    @Input
    public abstract Property<String> getOutputFileExtension();

	/**
	 * The required gradle task input string property for the ontology IRI (Optional)
	 * 
	 * @return String Property
	 */
	@Input
	public abstract Property<String> getInputOntologyIri();

	/**
	 * Reasoner explanation format (Optional, default is owl).
	 *         options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss
	 *         
	 * @return String Property
	 */
	@Optional
	@Input
	public abstract Property<String> getExplanationFormat();

	/**
	 * Use unique name assumption while reasoning (Optional, false by default)
	 * 
	 * @return Boolean Property
	 */
	@Optional
	@Input
	public abstract Property<Boolean> getUniqueNames();

	/**
	 * Path of reasoner reports (Required).
	 * 
	 * @return RegularFile Property
	 */
	@OutputFile
	public abstract RegularFileProperty getReportPath();

	/**
	 * Path of an output (txt) file containing all ontology IRIs in the dataset (one per line) (Optional).
	 * 
	 * @return RegularFile Property
	 */
	@Optional
	@OutputFile
	public abstract Property<File> getOutputOntologyIrisPath();

	/**
	 * Whether to remove entailments due to unsatisfiability (Optional, default is true).
	 * 
	 * @return Boolean Property
	 */
	@Optional
	@Input
	public abstract Property<Boolean> getRemoveUnsats();

	/**
	 * Whether to check minimum cardinality restrictions (default is true).
	 * 
	 * @return Boolean Property
	 */
	@Optional
	@Input
	public abstract Property<Boolean> getCheckMinimumCardinality();

	/**
	 * Whether to remove the backbone ontology (default is true).
	 * 
	 * @return Boolean Proeprty
	 */
	@Optional
	@Input
	public abstract Property<Boolean> getRemoveBackbone();

	/**
	 * IRI of the backbone ontology (Optional, default is http://opencaesar.io/oml).
	 * 
	 * @return String Property
	 */
	@Optional
	@Input
	public abstract Property<String> getBackboneIri();

	/**
	 * Length of the indent property (Optional, default is 2).
	 * 
	 * @return Integer Property
	 */
	@Optional
	@Input
	public abstract Property<Integer> getIndent();

	/**
	 * Whether to omit explanations (Optional, default is false).
	 * 
	 * @return Integer Property
	 */
	@Optional
	@Input
	public abstract Property<Boolean> getOmitExplanations();

	/**
	 * Whether to enable debugging (Optional, default is false).
	 * 
	 * @return Boolean Property
	 */
	@Optional
	@Input
	public abstract Property<Boolean> getDebug();

	/**
	 * Returns the reasoner input files based on the OASIS XML catalog.
	 * 
	 * @return ConfigurableFileCollection
	 * @throws IOException error
	 * @throws URISyntaxException error
	 */
	@Incremental
	@InputFiles
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

	/**
	 * Returns the output files that the reasoner will generate.
	 * 
	 * @return ConfigurableFileCollection
	 * @throws IOException error
	 * @throws URISyntaxException error
	 */
	@OutputFiles
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

	/**
	 * The gradle task action logic.
	 */
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
		if (getOutputOntologyIrisPath().isPresent()) {
			args.add("-oi");
			args.add(getOutputOntologyIrisPath().get().getAbsolutePath());
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
		if (getUniqueNames().isPresent() && getUniqueNames().get()) {
			args.add("-un");
		}
		if (getRemoveUnsats().isPresent()) {
			args.add("-ru");
			args.add(getRemoveUnsats().get() ? "true" : "false");
		}
		if (getCheckMinimumCardinality().isPresent()) {
			args.add("-min");
			args.add(getCheckMinimumCardinality().get() ? "true" : "false");
		}
		if (getRemoveBackbone().isPresent()) {
			args.add("-rb");
			args.add(getRemoveBackbone().get() ? "true" : "false");
		}
		if (getBackboneIri().isPresent()) {
			args.add("-b");
			args.add(getBackboneIri().get());
		}
		if (getIndent().isPresent()) {
			args.add("-n");
			args.add(getIndent().get().toString());
		}
		if (getOmitExplanations().isPresent() && getOmitExplanations().get()) {
			args.add("-oe");
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
