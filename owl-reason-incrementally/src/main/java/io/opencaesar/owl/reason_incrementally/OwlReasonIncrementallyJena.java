package io.opencaesar.owl.reason_incrementally;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.jena.graph.Graph;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.xml.DOMConfigurator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import openllet.jena.PelletInfGraph;
import openllet.jena.PelletReasonerFactory;
import openllet.shared.tools.Log;

/**
 * Experiments with incremental reasoning.
 */
public class OwlReasonIncrementallyJena {

	/**
	 * The default OWL file extensions
	 */
	public static final String[] DEFAULT_EXTENSIONS = { "owl", "ttl" };

	private static final List<String> extensions = Arrays.asList("fss", "owl", "rdf", "xml", "n3", "ttl", "rj", "nt",
			"jsonld", "trig", "trix", "nq");

	private static class Options {
		@Parameter(names = { "--input-catalog-path",
				"-c" }, description = "path to the input OWL catalog (Required)", validateWith = CatalogPathValidator.class, required = true, order = 1)
		private String inputCatalogPath;

		@Parameter(names = { "--input-ontology-iri",
				"-i" }, description = "iri of input OWL ontology (Optional, by default all ontologies in catalog)", order = 2)
		private List<String> inputOntologyIris = new ArrayList<>();

		@Parameter(names = { "--input-file-extension",
				"-e" }, description = "input file extension (owl and ttl by default, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss)", validateWith = FileExtensionValidator.class, order = 3)
		private List<String> inputFileExtensions = new ArrayList<>(Arrays.asList(DEFAULT_EXTENSIONS));

		@Parameter(names = { "--debug", "-d" }, description = "Shows debug logging statements", order = 4)
		private boolean debug;

		@Parameter(names = { "--help", "-h" }, description = "Displays summary of options", help = true, order = 5)
		private boolean help;
	}

	private final Options options = new Options();

	private final static Logger LOGGER = Log.getLogger(OwlReasonIncrementallyJena.class, Level.ALL);
	static {
		DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
	}

	private static class StdErrHandler extends Handler {
		public StdErrHandler() {
		}

		@Override
		public void publish(LogRecord record) {
			String message = record.getMessage();
			System.err.print(message);
			this.flush();
		}

		@Override
		public void flush() {
			System.err.flush();
		}

		/**
		 * Override {@code StreamHandler.close} to do a flush but not to close the
		 * output stream. That is, we do <b>not</b> close {@code System.err}.
		 */
		@Override
		public void close() {
			flush();
		}
	}

	public static void main(String[] args) throws Exception {
		final OwlReasonIncrementallyJena app = new OwlReasonIncrementallyJena();
		final JCommander builder = JCommander.newBuilder().addObject(app.options).build();
		builder.parse(args);
		if (app.options.help) {
			builder.usage();
			return;
		}
		if (app.options.debug) {
			LOGGER.addHandler(new StdErrHandler());
		}
		app.run1();
	}

	public OwlReasonIncrementallyJena() {
	}

	// This version tries to use datasets, SPARQL Update on named graphs (not
	// working yet)
	private void run1() throws Exception {

		final OwlCatalog catalog = OwlCatalog.create(new File(options.inputCatalogPath).toURI());
		final Map<String, URI> fileMap = catalog.getFileUriMap(options.inputFileExtensions);

		final OntDocumentManager mgr = new OntDocumentManager();
		final FileManager fm = mgr.getFileManager();
		for (var entry : fileMap.entrySet()) {
			mgr.addAltEntry(entry.getKey(), entry.getValue().toString());
		}

		if (options.inputOntologyIris.isEmpty()) {
			options.inputOntologyIris.addAll(fileMap.keySet());
		}

		final Dataset dataset = DatasetFactory.createTxnMem();
		for (var iri : options.inputOntologyIris) {
			Model m = ModelFactory.createDefaultModel();
			fm.readModelInternal(m, iri);
			dataset.addNamedModel(iri, m);
		}

		PelletReasonerFactory.THE_SPEC.setDocumentManager(mgr);
		final OntModel ontModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		ontModel.addSubModel(dataset.getUnionModel());

		final String base = "http://imce.jpl.nasa.gov/foundation/base#";
		final String mission = "http://imce.jpl.nasa.gov/foundation/mission#";
		final OntClass Component = ontModel.getOntClass(mission + "Component");
		final OntClass Function = ontModel.getOntClass(mission + "Function");

		System.out.println("\nstatements = " + ontModel.getGraph().size());
		System.out.println("valid = " + ontModel.validate().isValid());
		query(ontModel, "http://example.com#c1");

		// final Individual c1 = ontModel.createIndividual("http://example.com#c1",
		// Component);
		UpdateRequest request = UpdateFactory.create();
		request.add(
				"INSERT DATA { GRAPH <http://imce.jpl.nasa.gov/foundation/mission#> { <http://example.com#c1> a <http://imce.jpl.nasa.gov/foundation/mission#Component> } }");
		UpdateAction.execute(request, dataset);
		Individual c1 = ontModel.getIndividual("http://example.com#c1");

		final Graph g = ontModel.getGraph();
		assert g instanceof PelletInfGraph;
		final PelletInfGraph ig = (PelletInfGraph)g;

		ig.prepare();
		ig.getKB().realize();

		System.out.println("\nstatements = " + ontModel.getGraph().size());
		System.out.println("valid = " + ontModel.validate().isValid());
		query(ontModel, "http://example.com#c1");
		query(ontModel, "http://srl.jpl.nasa.gov/efse/assemblies#Subsystem_2000");

		// ontModel.remove(c1, RDF.type, Component);
		request = UpdateFactory.create();
		request.add(
				"DELETE DATA { GRAPH <http://imce.jpl.nasa.gov/foundation/mission#> { <http://example.com#c1> a <http://imce.jpl.nasa.gov/foundation/mission#Component> } }");
		UpdateAction.execute(request, dataset);

		ig.prepare();
		ig.getKB().realize();

		System.out.println("\nstatements = " + ontModel.getGraph().size());
		System.out.println("valid = " + ontModel.validate().isValid());
		query(ontModel, "http://example.com#c1");
		query(ontModel, "http://srl.jpl.nasa.gov/efse/assemblies#Subsystem_2000");

		// ontModel.add(c1, RDF.type, Function);
		// request = UpdateFactory.create() ;
		// request.add("INSERT DATA { <http://example.com#c1> a
		// <http://imce.jpl.nasa.gov/foundation/mission#Function> }");
		// UpdateAction.execute(request, dataset) ;

		// System.out.println("\nstatements = "+ontModel.getGraph().size());
		// System.out.println("valid = "+ontModel.validate().isValid());
		// query(ontModel);
	}

	// This version demonstrates SPARQL Update in the default graph
	private void run2() throws Exception {
		final OwlCatalog catalog = OwlCatalog.create(new File(options.inputCatalogPath).toURI());
		final Map<String, URI> fileMap = catalog.getFileUriMap(options.inputFileExtensions);

		final OntDocumentManager mgr = new OntDocumentManager();
		final FileManager fm = mgr.getFileManager();
		for (var entry : fileMap.entrySet()) {
			mgr.addAltEntry(entry.getKey(), entry.getValue().toString());
		}

		if (options.inputOntologyIris.isEmpty()) {
			options.inputOntologyIris.addAll(fileMap.keySet());
		}

		PelletReasonerFactory.THE_SPEC.setDocumentManager(mgr);
		final OntModel ontModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		for (var iri : options.inputOntologyIris) {
			Model m = ModelFactory.createDefaultModel();
			fm.readModelInternal(m, iri);
			ontModel.addSubModel(m);
		}

		final String base = "http://imce.jpl.nasa.gov/foundation/base#";
		final String mission = "http://imce.jpl.nasa.gov/foundation/mission#";
		final OntClass Component = ontModel.getOntClass(mission + "Component");
		final OntClass Function = ontModel.getOntClass(mission + "Function");

		System.out.println("\nstatements1 = " + ontModel.getGraph().size());
		System.out.println("valid = " + ontModel.validate().isValid());
		query(ontModel, "http://example.com#c1");

		// final Individual c1 = ontModel.createIndividual("http://example.com#c1",
		// Component);
		UpdateRequest request = UpdateFactory.create();
		request.add(
				"INSERT DATA { <http://example.com#c1> a <http://imce.jpl.nasa.gov/foundation/mission#Component> }");
		UpdateAction.execute(request, ontModel);
		Individual c1 = ontModel.getIndividual("http://example.com#c1");

		final Graph g = ontModel.getGraph();
		assert g instanceof PelletInfGraph;
		final PelletInfGraph ig = (PelletInfGraph)g;

		ig.prepare();
		ig.getKB().realize();

		System.out.println("\nstatements2 = " + ontModel.getGraph().size());
		System.out.println("valid = " + ontModel.validate().isValid());
		query(ontModel, "http://example.com#c1"); // triggers reasoning.

		// ontModel.remove(c1, RDF.type, Component);
		request = UpdateFactory.create();
		request.add(
				"DELETE DATA { <http://example.com#c1> a <http://imce.jpl.nasa.gov/foundation/mission#Component> }");
		UpdateAction.execute(request, ontModel);

		System.out.println("\nstatements3 = " + ontModel.getGraph().size());
		System.out.println("valid = " + ontModel.validate().isValid());
		query(ontModel, "http://example.com#c1");

		// ontModel.add(c1, RDF.type, Function);
		// request = UpdateFactory.create() ;
		// request.add("INSERT DATA { <http://example.com#c1> a
		// <http://imce.jpl.nasa.gov/foundation/mission#Function> }");
		// UpdateAction.execute(request, dataset) ;

		// System.out.println("\nstatements = "+ontModel.getGraph().size());
		// System.out.println("valid = "+ontModel.validate().isValid());
		// query(ontModel);
	}

	// This version demonstrates updates with API, and also inconsistency detection
	private void run3() throws Exception {
		final OwlCatalog catalog = OwlCatalog.create(new File(options.inputCatalogPath).toURI());
		final Map<String, URI> fileMap = catalog.getFileUriMap(options.inputFileExtensions);

		final OntDocumentManager mgr = new OntDocumentManager();
		final FileManager fm = mgr.getFileManager();
		for (var entry : fileMap.entrySet()) {
			mgr.addAltEntry(entry.getKey(), entry.getValue().toString());
		}

		if (options.inputOntologyIris.isEmpty()) {
			options.inputOntologyIris.addAll(fileMap.keySet());
		}

		PelletReasonerFactory.THE_SPEC.setDocumentManager(mgr);
		final OntModel ontModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		for (var iri : options.inputOntologyIris) {
			Model m = ModelFactory.createDefaultModel();
			fm.readModelInternal(m, iri);
			ontModel.addSubModel(m);
		}

		final String base = "http://imce.jpl.nasa.gov/foundation/base#";
		final String mission = "http://imce.jpl.nasa.gov/foundation/mission#";
		final OntClass Component = ontModel.getOntClass(mission + "Component");
		final OntClass Function = ontModel.getOntClass(mission + "Function");

		System.out.println("\nstatements = " + ontModel.getGraph().size());
		System.out.println("valid = " + ontModel.validate().isValid());
		query(ontModel, "http://example.com#c1");

		final Individual c1 = ontModel.createIndividual("http://example.com#c1", Component);

		System.out.println("\nstatements = " + ontModel.getGraph().size());
		System.out.println("valid = " + ontModel.validate().isValid());
		query(ontModel, "http://example.com#c1");

		ontModel.add(c1, RDF.type, Function);
		System.out.println("\nstatements = " + ontModel.getGraph().size());
		System.out.println("valid = " + ontModel.validate().isValid());
		query(ontModel, "http://example.com#c1");
	}

	private void query(Model ontModel, String iri) {
		String queryString = String
				.format("PREFIX fse:   <http://opencaesar.io/examples/firesat/disciplines/fse/fse#>\n"
						+ "PREFIX rd:   <http://imce.jpl.nasa.gov/foundation/base#>\n" + "SELECT ?y\n" + "WHERE {\n"
						+ "	<%s> a ?y\n" + "}", iri);

		System.out.println("Types of c1 are:");
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
			ResultSet results = qexec.execSelect();
			for (; results.hasNext();) {
				QuerySolution soln = results.nextSolution();
				Resource r = soln.getResource("y");
				System.out.println(r);
			}
		}
	}
    //--------
    
	/**
	 * Sort resources by label.
	 * @param resources resources
	 * @param getLabel label function
	 * @return sorted resources by their labels.
	 * @param <T> resource type.
	 */
	public static <T extends Resource> List<T> sortResourcesi(List<T> resources, Function<RDFNode, String> getLabel) {
		var filtered = resources.stream().filter(i -> !i.isAnon()).collect(Collectors.toList());
		filtered.sort((x1, x2) -> getLabel.apply(x1).compareTo(getLabel.apply(x2)));
		return filtered;
	}

	/**
	 * sort resources by iri.
	 * @param resources resources
	 * @return sorted resources by iri.
	 * @param <T> resource type.
	 */
	public static <T extends Resource> List<T> sortByIri(List<T> resources) {
		return sortResourcesi(resources, i -> ((Resource)i).getURI());
	}

	/**
	 * sort resources by name
	 * @param resources resources.
	 * @return sorted resources by name.
	 * @param <T> resource type.
	 */
	public static <T extends Resource> List<T> sortByName(List<T> resources) {
		return sortResourcesi(resources, i -> localName((Resource)i));
	}
    
	private static String localName(Resource resource) {
		var iri = resource.getURI();
		int index = iri.lastIndexOf("#");
		if (index == -1) {
			index = iri.lastIndexOf("/");
		}
		return (index != -1) ? iri.substring(index+1) : resource.getLocalName();
	}
    
	/**
	 * A parameter validator for an OASIS XML catalog path.
	 */
	public static class CatalogPathValidator implements IParameterValidator {
		/**
		 * Creates a new CatalogPath object
		 */
		public CatalogPathValidator() {
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
	 * A parameter validator for a file with one of the supported extensions
	 */
	public static class FileExtensionValidator implements IParameterValidator {
		/**
		 * Creates a new FileExtensionValidator object
		 */
		public FileExtensionValidator() {
		}
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			if (!extensions.contains(value)) {
				throw new ParameterException("Parameter " + name + " should be a valid extension, got: " + value +
						" recognized extensions are: " +
						extensions.stream().reduce( (x,y) -> x + " " + y) );
			}
		}
	}

	/**
	 * A parameter validator for an output RDF file.
	 */
	public static class OutputFileExtensionValidator implements IParameterValidator {
		/**
		 * Creates a new OutputFileExtensionValidator object
		 */
		public OutputFileExtensionValidator() {
		}
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			Lang lang = RDFLanguages.fileExtToLang(value);
			if (lang == null) {
				throw new ParameterException("Parameter " + name + " should be a valid RDF output extension, got: " + value +
						" recognized RDF extensions are: "+extensions);
			}
		}
	}

	/**
	 * The validator for output folder paths
	 */
	public static class OutputFolderPathValidator implements IParameterValidator {
		/**
		 * Creates a new OutputFolderPath object
		 */
		public OutputFolderPathValidator() {}
		@Override 
		public void validate(String name, String value) throws ParameterException {
			final var directory = new File(value).getAbsoluteFile();
			if (!directory.isDirectory()) {
				final var created = directory.mkdirs();
				if (!created) {
					throw new ParameterException("Parameter " + name + " should be a valid folder path");
				}
			}
	  	}
	}

}