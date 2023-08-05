package io.opencaesar.owl.reason_incrementally;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.opencaesar.owl.doc.OwlCatalog;
import io.opencaesar.owl.doc.OwlDocApp;
import io.opencaesar.owl.doc.OwlDocApp.CatalogPathValidator;
import io.opencaesar.owl.doc.OwlDocApp.FileExtensionValidator;
import openllet.core.KnowledgeBase;
import openllet.jena.PelletInfGraph;
import openllet.jena.PelletReasonerFactory;
import openllet.shared.tools.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.ontology.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Experiments with incremental reasoning.
 */
public class OwlReasonIncrementallyJena2 {

	/**
	 * The default OWL file extensions
	 */
	public static final String[] DEFAULT_EXTENSIONS = { "owl", "ttl" };

	private static final List<String> extensions = Arrays.asList("fss", "owl", "rdf", "xml", "n3", "ttl", "rj", "nt",
			"jsonld", "trig", "trix", "nq");

	private static final String CSS_DEFAULT = "default.css";
	private static final String CSS_MAIN = "main.css";

	private static final String IMG_ONTOLOGY = "https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/images/org.eclipse.jdt.ui/obj16/package_obj.svg";
	private static final String IMG_CLASS = "https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/images/org.eclipse.jdt.ui/obj16/methpub_obj.svg";
	private static final String IMG_DATATYPE = "https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/images/org.eclipse.jdt.ui/obj16/methpri_obj.svg";
	private static final String IMG_PROPERTY = "https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/images/org.eclipse.jdt.ui/obj16/methpro_obj.svg";
	private static final String IMG_INDIVIDUAL = "https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/images/org.eclipse.jdt.ui/obj16/field_public_obj.svg";
	private static final String IMG_ITEM = "https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/images/org.eclipse.jdt.ui/obj16/methdef_obj.svg";

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

	private class OwlModel {
		private OntModel ontModel;
		private List<Ontology> ontologies;
		private List<OntClass> classes;
		private List<Resource> datatypes;
		private List<AnnotationProperty> annotationProperties;
		private List<DatatypeProperty> datatypeProperties;
		private List<ObjectProperty> objectProperties;
		private List<Individual> individuals;

		public OwlModel(OntModel ontModel) {
			this.ontModel = ontModel;
			ontologies = OwlDocApp.sortByIri(ontModel.listOntologies().filterKeep(i -> hasTerms(i)).toList());
			classes = OwlDocApp.sortByName(ontModel.listNamedClasses().toList());
			datatypes = OwlDocApp.sortByName(ontModel.listSubjectsWithProperty(RDF.type, RDFS.Datatype).toList());
			annotationProperties = OwlDocApp.sortByName(ontModel.listAnnotationProperties().toList());
			objectProperties = OwlDocApp.sortByName(ontModel.listObjectProperties().toList());
			datatypeProperties = OwlDocApp.sortByName(ontModel.listDatatypeProperties().toList());
			individuals = OwlDocApp.sortByName(ontModel.listIndividuals().toList());
		}

		private boolean hasTerms(Ontology o) {
			var iri = o.getURI();
			var resources = ontModel.listResourcesWithProperty(RDF.type).filterDrop(i -> i.isAnon())
					.filterKeep(i -> i.getURI().startsWith(iri)).toList();
			resources.removeIf(i -> ontModel.getOntology(i.getURI()) != null);
			return !resources.isEmpty();
		}
	}

	private final static Logger LOGGER = Log.getLogger(OwlReasonIncrementallyJena2.class, Level.ALL);
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
		final OwlReasonIncrementallyJena2 app = new OwlReasonIncrementallyJena2();
		final JCommander builder = JCommander.newBuilder().addObject(app.options).build();
		builder.parse(args);
		if (app.options.help) {
			builder.usage();
			return;
		}
		if (app.options.debug) {
			LOGGER.addHandler(new StdErrHandler());
		}
		app.run2();
	}

	public OwlReasonIncrementallyJena2() {
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

		final Graph g = ontModel.getGraph();
		assert g instanceof PelletInfGraph;
		final PelletInfGraph ig = (PelletInfGraph)g;

		HashMap<String, Model> modelByIRI = new HashMap<>();
		for (var iri : options.inputOntologyIris) {
			Model m = ModelFactory.createDefaultModel();
			fm.readModelInternal(m, iri);
			modelByIRI.put(iri, m);
			System.out.println(iri);
			ontModel.addSubModel(m);
		}

		final KnowledgeBase kb = ig.getKB();

		final String base = "http://imce.jpl.nasa.gov/foundation/base#";
		final String mission = "http://imce.jpl.nasa.gov/foundation/mission#";
		final OntClass Component = ontModel.getOntClass(mission + "Component");
		final OntClass Function = ontModel.getOntClass(mission + "Function");

		System.out.println("valid = " + ontModel.validate().isValid());
		System.out.println("statements1 = " + ontModel.getGraph().size());
		System.out.println("kb individuals = " + kb.getIndividuals().size());

		UpdateRequest request = UpdateFactory.create();
		request.add(
				"INSERT DATA { <http://example.com#c1> a <http://imce.jpl.nasa.gov/foundation/mission#Component> }");
		System.out.println("INSERT...");

//		Model missionM = modelByIRI.get("http://imce.jpl.nasa.gov/foundation/mission");
//		UpdateAction.execute(request, missionM);
		UpdateAction.execute(request, ontModel);

		System.out.println("statements2 = " + ontModel.getGraph().size());
		System.out.println("kb individuals = " + kb.getIndividuals().size());
		System.out.println("valid = " + ontModel.validate().isValid());

		query(ontModel, "http://example.com#c1"); // triggers reasoning.

		// ontModel.remove(c1, RDF.type, Component);
		request = UpdateFactory.create();
		request.add(
				"DELETE DATA { <http://example.com#c1> a <http://imce.jpl.nasa.gov/foundation/mission#Component> }");
		System.out.println("DELETE...");

//		UpdateAction.execute(request, missionM);
		UpdateAction.execute(request, ontModel);

		System.out.println("statements3 = " + ontModel.getGraph().size());
		System.out.println("kb individuals = " + kb.getIndividuals().size());
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

}