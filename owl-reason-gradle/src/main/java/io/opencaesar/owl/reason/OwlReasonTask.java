package io.opencaesar.owl.reason;

import java.io.File;
import java.io.IOException;
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
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.Incremental;

public abstract class OwlReasonTask extends DefaultTask {

	private final static Logger LOGGER = Logger.getLogger(OwlReasonTask.class);

	static {
		DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("owlreason.log4j2.properties"));
	}

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

	private List<String> specs;

	@Input
	public List<String> getSpecs() { return specs; }

	public void setSpecs(List<String> s) throws IOException, URISyntaxException {
		specs = s;
		calculateInputFiles();
	}

	@Input
	public abstract Property<String> getOutputFileExtension();

	private static final Comparator<File> fileComparator = Comparator.comparing(File::getAbsolutePath);

	/**
	 * For gradle incremental task support, it is necessary to exclude the reasoner output files from the calculation.
	 */
	private void calculateInputFiles() throws IOException, URISyntaxException {
		if (null != catalogPath && null != inputFileExtensions && null != specs && getOutputFileExtension().isPresent()) {
			final String outputFileExtension = getOutputFileExtension().get();
			final URI catalogURI = URI.createFileURI(catalogPath.getAbsolutePath());
			final OmlCatalog inputCatalog = OmlCatalog.create(catalogURI);
			URI catalogDir = catalogURI.trimSegments(1);
			if (!catalogDir.hasTrailingPathSeparator())
				catalogDir=catalogDir.appendSegment("");
			LOGGER.info("catalog dir: "+catalogDir);
			final ArrayList<File> owlFiles = new ArrayList<>();
			for (URI uri : inputCatalog.getFileUris(inputFileExtensions)) {
				final File file = new File(new URL(uri.toString()).toURI().getPath());
				if (file.isFile()) {
					boolean add = true;
					if (outputFileExtension.equals(uri.fileExtension())) {
						final URI rel = uri.deresolve(catalogDir, false, false, true).trimFileExtension();
						final String entailment = "http://" + rel.toString();
						// Appending a suffix prevents matching on a prefix of the entailment IRI.
						final String entailment1 = entailment+"=";
						final String entailment2 = entailment+" =";
						if (specs.stream().anyMatch(s -> s.startsWith(entailment1) || s.startsWith(entailment2))) {
							LOGGER.info("skip: " + rel);
							add = false;
						} else
							LOGGER.info(" add: " + rel);
					}
					if (add)
						owlFiles.add(file);
				}
			}
			owlFiles.sort(fileComparator);
			LOGGER.info("calculateInputFiles found: "+owlFiles.size());
			for (File owlFile : owlFiles) {
				LOGGER.info("input: "+owlFile);
			}
			getInputFiles().setFrom(owlFiles);
		}
	}

	@Incremental
	@InputFiles
	public abstract ConfigurableFileCollection getInputFiles();

	@Input
	public abstract Property<String> getInputOntologyIri();

	@OutputFile
	public abstract RegularFileProperty getReportPath();

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
		if (null != catalogPath) {
			args.add("-c");
			args.add(catalogPath.getAbsolutePath());
		}
		if (getInputOntologyIri().isPresent()) {
			args.add("-i");
			args.add(getInputOntologyIri().get());
		}
		if (null != specs) {
			specs.forEach((String spec) -> {
				args.add("-s");
				args.add(spec);
			});
		}
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
