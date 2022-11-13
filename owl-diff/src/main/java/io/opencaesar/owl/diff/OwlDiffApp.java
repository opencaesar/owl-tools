package io.opencaesar.owl.diff;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.HasIRI;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * Ontology comparison utility to print differences in terms of ontology import declaration and other axioms
 * between two ontologies.
 */
public class OwlDiffApp {

	/**
	 * Creates a new OwlDiffApp object
	 */
	public OwlDiffApp() {
	}

	@Parameter(
		names = { "--catalog1", "-c1" },
		description = "Path to the OWL catalog 1 file (Required)",
		validateWith = CatalogPath.class,
		required = true,
		order = 1)
	private String catalogPath1 = null;

	@Parameter(
		names = { "--catalog2", "-c2" },
		description = "Path to the OWL catalog 2 file (Required)",
		validateWith = CatalogPath.class, 
		required = true,
		order = 2)
	private String catalogPath2 = ".";

	@Parameter(
		names = { "--ignore", "-i" },
		description = "List of comma-separated partial IRIs to ignore reporting on",
		required = false,
		converter = SetOfIris.class,
		order = 3)
	private Set<String> ignoreSet = new HashSet<String>();

	@Parameter(
		names = { "-d", "--debug" },
		description = "Shows debug logging statements",
		order = 4)
	private boolean debug;

	@Parameter(
		names = { "--help", "-h" },
		description = "Displays summary of options",
		help = true,
		order =5)
	private boolean help;
	
	private final static Logger LOGGER = Logger.getLogger(OwlDiffApp.class);
	{
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
	}

	/**
	 * Application for the ontology comparison tool.
	 * @param args Application arguments.
	 * @throws Exception Error
	 */
	public static void main(final String... args) throws Exception {
		final OwlDiffApp app = new OwlDiffApp();
		final JCommander builder = JCommander.newBuilder().addObject(app).build();
		builder.parse(args);
		if (app.help) {
			builder.usage();
			return;
		}
		if (app.debug) {
			final Appender appender = Logger.getRootLogger().getAppender("stdout");
			((AppenderSkeleton) appender).setThreshold(Level.DEBUG);
		}
		app.run();
	}

	/**
	 * Runs the ontology comparison utility.
	 * @throws Exception Error
	 */
	public void run() throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                       OWL Diff " + getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info(("OWL Catalog 1 = " + catalogPath1));
		LOGGER.info(("OWL Catalog 2 = " + catalogPath2));

		Map<String, Pair> index = new HashMap<String, Pair>();

		// OWL catalog 1
		final File folder1 = new File(catalogPath1).getParentFile();
		final Collection<File> files1 = collectOwlFiles(folder1);
		final OWLOntologyManager manager1 = OWLManager.createOWLOntologyManager();
		manager1.getIRIMappers().add((new XMLCatalogIRIMapper(new File(catalogPath1))));
        for(File file :	files1) {
   			String relativePath = folder1.toURI().relativize(file.toURI()).getPath();
   			index.put(relativePath, new Pair(file, null));
        }

		// OWL catalog 2
		final File folder2 = new File(catalogPath2).getParentFile();
		final Collection<File> files2 = collectOwlFiles(folder2);
		final OWLOntologyManager manager2 = OWLManager.createOWLOntologyManager();
		manager2.getIRIMappers().add((new XMLCatalogIRIMapper(new File(catalogPath2))));
        for(File file :	files2) {
   			String relativePath = folder2.toURI().relativize(file.toURI()).getPath();
   			Pair pair = index.get(relativePath);
			if (pair == null) {
				index.put(relativePath, new Pair(null, file));
			} else {
				pair.file2 = file;
			}
        }
		
		// Compare catalogs 1 and 2
		List<Pair> pairs = index.values().stream().collect(Collectors.toList());
		for (Pair pair : pairs) {
			if (pair.file2 == null) {
				final OWLOntology ontology1 = manager1.loadOntologyFromOntologyDocument(pair.file1);
				LOGGER.info("Ontology "+ontology1.getOntologyID().getOntologyIRI().get()+':');
				LOGGER.info("\tOntology in catalog 1 only");
			} else if (pair.file1 == null) {
				final OWLOntology ontology2 = manager2.loadOntologyFromOntologyDocument(pair.file2);
				LOGGER.info("Ontology "+ontology2.getOntologyID().getOntologyIRI().get()+':');
				LOGGER.info("\tOntology in catalog 2 only");
			} else {
				final OWLOntology ontology1 = manager1.loadOntologyFromOntologyDocument(pair.file1);
				final OWLOntology ontology2 = manager2.loadOntologyFromOntologyDocument(pair.file2);
				LOGGER.info("Ontology "+ontology1.getOntologyID().getOntologyIRI().get()+':');
				check(ontology1, ontology2);
			}
		};

		LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}

	/**
	 * Print the differences between two ontologies (1 and 2) in terms of:
	 * - import declaration axioms in 2 but not in 1
	 * - axioms in 2 but not in 1
	 * @param ontology1 an ontology 1
	 * @param ontology2 an ontology 2
	 */
	public void check(final OWLOntology ontology1, final OWLOntology ontology2) {
		Set<OWLImportsDeclaration> imports1 = ontology1.importsDeclarations().collect(Collectors.toSet());
		Set<OWLImportsDeclaration> imports2 = ontology2.importsDeclarations().collect(Collectors.toSet());
		
		Set<OWLAxiom> axioms1 = ontology1.axioms().collect(Collectors.toSet());
		Set<OWLAxiom> axioms2 = ontology2.axioms().collect(Collectors.toSet());

		printImportsInRightButNotLeft(imports1, imports2, "Axioms in ontology 1 only:");
		printAxiomsInRightButNotLeft(axioms1, axioms2, "Axioms in ontology 1 only:");

		printImportsInRightButNotLeft(imports2, imports1, "Axioms in ontology 2 only:");				
		printAxiomsInRightButNotLeft(axioms2, axioms1, "Axioms in ontology 2 only:");			
	}

	/**
	 * Utility for printing import declaration axioms in the right set that are not in the left set.
	 *
	 * @param left a set of import declaration axioms
	 * @param right a set of import declaration axioms
	 * @param label A label prefixed for each import declaration axiom to print.
	 */
	public void printImportsInRightButNotLeft(final Set<OWLImportsDeclaration> left, final Set<OWLImportsDeclaration> right, final String label) {
		left.stream().
			sorted().
			filter(it -> !right.contains(it)).
			forEach(it -> LOGGER.info('\t'+label+' '+it.toString().replace('\n', ' ')));
	}

	/**
	 * Utility for printing axioms in the right set that are not in the left set.
	 *
	 * @param left a set of axioms
	 * @param right a set of axioms
	 * @param label A label prefixed for each axiom to print.
	 */
	public void printAxiomsInRightButNotLeft(final Set<OWLAxiom> left, final Set<OWLAxiom> right, final String label) {
		left.stream().
			sorted().
			filter(it -> !right.contains(it)).
			filter(it -> !shouldIgnore(it)).
			forEach(it -> LOGGER.info('\t'+label+' '+it.toString().replace('\n', ' ')));
	}

	private boolean shouldIgnore(OWLAxiom axiom) {
		return axiom.components().
			filter(it -> it instanceof HasIRI).
			map(it -> ((HasIRI) it).getIRI()).
			anyMatch(it -> shouldIgnore(it));
	}

	private boolean shouldIgnore(IRI iri) {
		final String str = iri.getIRIString();
		return ignoreSet.stream().anyMatch(it -> str.startsWith(it));
	}
	
	private Collection<File> collectOwlFiles(final File directory) {
		ArrayList<File> owlFiles = new ArrayList<File>();
		for (File file : directory.listFiles()) {
			if (file.isFile()) {
				if (getFileExtension(file).equals("owl")) {
					owlFiles.add(file);
				}
			} else if (file.isDirectory()) {
				owlFiles.addAll(collectOwlFiles(file));
			}
		}
		return owlFiles;
	}

	private String getFileExtension(final File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1)
        	return fileName.substring(fileName.lastIndexOf(".")+1);
        else 
        	return "";
	}

	private String getAppVersion() throws Exception {
    	var version = this.getClass().getPackage().getImplementationVersion();
    	return (version != null) ? version : "<SNAPSHOT>";
    }

	/**
	 * A parameter validator for an OASIS XML catalog path.
	 */
	public static class CatalogPath implements IParameterValidator {
		
		/**
		 * Creates a new CatalogPath object
		 */
		public CatalogPath() {
		}
		
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File file = new File(value);
			if (!file.exists() || !file.getName().endsWith("catalog.xml")) {
				throw new ParameterException("Parameter " + name + " should be a valid OWL catalog path");
			}
		}
	}

	/**
	 * A parameter validator for an output file path.
	 */
	public static class OutputFilePath implements IParameterValidator {

		/**
		 * Creates a new OutputFilePath object
		 */
		public OutputFilePath() {
		}

		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File folder = new File(value).getParentFile();
			if (!folder.exists()) {
				folder.mkdir();
			}
		}
	}

	/**
	 * A converter for a set of IRIs.
	 */
	public static class SetOfIris implements IStringConverter<Set<String>>  {

		/**
		 * Creates a new SetOfIris object
		 */
		public SetOfIris() {
		}

		@Override
		public Set<String> convert(final String value) {
			return new HashSet<String>(Arrays.asList(value.split(",")));
		}
	}

	private class Pair {
		public File file1;
		public File file2;
		public Pair(File file1, File file2) {
			this.file1 = file1;
			this.file2 = file2;
		}
	}
}
