package io.opencaesar.owl.diff;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.OWL;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

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
     * Allowed input file extensions for ontologies.
     */
	public static String[] DEFAULT_EXTENSIONS = { "owl", "ttl" };

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
        names = {"--file-extensions", "-f"},
        description = "File extensions of files that will be uploaded. Default is owl and ttl, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss (Optional)",
    	validateWith = FileExtensionValidator.class,
        order = 3)
    private List<String> fileExtensions = new ArrayList<>();
    {
        fileExtensions.addAll(Arrays.asList(DEFAULT_EXTENSIONS));
    }

    @Parameter(
		names = { "--ignore", "-i" },
		description = "List of comma-separated partial IRIs to ignore reporting on",
		required = false,
		converter = SetOfIris.class,
		order = 4)
	private Set<String> ignoreSet = new HashSet<String>();

	@Parameter(
		names = { "-d", "--debug" },
		description = "Shows debug logging statements",
		order = 5)
	private boolean debug;

	@Parameter(
		names = { "--help", "-h" },
		description = "Displays summary of options",
		help = true,
		order =6)
	private boolean help;
	
    private final static Logger LOGGER = Logger.getLogger(OwlDiffApp.class);

    static {
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

		Map<String, Pair> index = new HashMap<String, Pair>();

		// Identify files in OWL catalog 1
		OwlCatalog catalog1 = OwlCatalog.create(new File(catalogPath1).toURI());
		final URI folder1 = URI.create(catalog1.getBaseUri().toString());
		final Collection<URI> files1 = catalog1.getFileUris(fileExtensions);
        for(URI file :	files1) {
   			String relativePath = folder1.relativize(file).getPath();
   			index.put(relativePath, new Pair(new File(file), null));
        }

		// Identify files in OWL catalog 2
		OwlCatalog catalog2 = OwlCatalog.create(new File(catalogPath2).toURI());
		final URI folder2 = URI.create(catalog2.getBaseUri().toString());
		final Collection<URI> files2 = catalog2.getFileUris(fileExtensions);
        for(URI file :	files2) {
   			String relativePath = folder2.relativize(file).getPath();
   			Pair pair = index.get(relativePath);
			if (pair == null) {
				index.put(relativePath, new Pair(null, new File(file)));
			} else {
				pair.file2 = new File(file);
			}
        }
		
		List<Pair> pairs = index.values().stream().collect(Collectors.toList());
		for (Pair pair : pairs) {
			final Model model1 = (pair.file1 != null) ? loadModel(pair.file1) : ModelFactory.createDefaultModel();
			final Model model2 = (pair.file2 != null) ? loadModel(pair.file2) : ModelFactory.createDefaultModel();
			compare(model1, model2);
		};

		LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}

	private Model loadModel(File file) {
		String filePath = file.getAbsolutePath();
        ContentType ct = RDFLanguages.guessContentType(filePath) ;
		Model model = ModelFactory.createDefaultModel();
		model.read(filePath, ct.getContentTypeStr());
		return model;
	}
	
	/**
	 * Print the differences between two RDF models (1 and 2)
	 * @param model1 an model 1
	 * @param model2 an model 2
	 */
	public void compare(final Model model1, final Model model2) {
		var deleted = getStatementsInLeftButNotRight(model1, model2);
		var added = getStatementsInLeftButNotRight(model2, model1);
		if (!deleted.isEmpty() || !added.isEmpty()) {
			if (getModelURI(model1) != null) {
				System.out.println("* "+getModelURI(model1));
			} else {
				System.out.println("* "+getModelURI(model2));
			}
			deleted.forEach(it -> System.out.println("\t- "+ it.toString()));
			added.forEach(it -> System.out.println("\t+ "+ it.toString()));
		}
	}

	/**
	 * Utility for determining statements that exist in the left model but not the right model .
	 *
	 * @param left the left model
	 * @param right the right model
	 * @return a list of statements that exist in the left model but not the right model
	 */
	public List<Statement> getStatementsInLeftButNotRight(final Model left, final Model right) {
		var statements = new ArrayList<Statement>();
		StmtIterator i = left.listStatements();
		while (i.hasNext()) {
			var statement = i.next();
			if (!right.contains(statement)) {
				if (!statement.getSubject().isAnon() &&
					!statement.getObject().isAnon() &&
					!statement.getPredicate().hasURI(OWL.versionInfo.getURI())) {
					statements.add(statement);
				}
			}
		}
		return statements;
	}

	private String getModelURI(Model m) {
		return m.getGraph().getPrefixMapping().getNsPrefixURI("");
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
     * A parameter validator for a file extension for an RDF language syntax.
     */
	public static class FileExtensionValidator implements IParameterValidator {
		/**
		 * Creates a new FileExtensionValidator object
		 */
		public FileExtensionValidator() {
		}
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			Lang lang = RDFLanguages.fileExtToLang(value);
			if (lang == null) {
				throw new ParameterException("File extension " + name + " is not a valid one");
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
