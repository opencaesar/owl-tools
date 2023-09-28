package io.opencaesar.owl.fuseki_reasoner;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasonerFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.ReasonerVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Experiments with incremental reasoning.
 */
public class OwlReasonIncrementallyJena1d {

    /**
     * The default OWL file extensions
     */
    public static final String[] DEFAULT_EXTENSIONS = {"owl", "ttl"};

    private static final List<String> extensions = Arrays.asList("fss", "owl", "rdf", "xml", "n3", "ttl", "rj", "nt",
            "jsonld", "trig", "trix", "nq");

    private static class Options {
        @Parameter(names = {"--input-catalog-path",
                "-c"}, description = "path to the input OWL catalog (Required)", validateWith = CatalogPathValidator.class, required = true, order = 1)
        private String inputCatalogPath;

        @Parameter(names = {"--input-ontology-iri",
                "-i"}, description = "iri of input OWL ontology (Optional, by default all ontologies in catalog)", order = 2)
        private List<String> inputOntologyIris = new ArrayList<>();

        @Parameter(names = {"--input-file-extension",
                "-e"}, description = "input file extension (owl and ttl by default, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss)", validateWith = FileExtensionValidator.class, order = 3)
        private List<String> inputFileExtensions = new ArrayList<>(Arrays.asList(DEFAULT_EXTENSIONS));

        @Parameter(names = {"--debug", "-d"}, description = "Shows debug logging statements", order = 4)
        private boolean debug;

        @Parameter(names = {"--help", "-h"}, description = "Displays summary of options", help = true, order = 5)
        private boolean help;
    }

    private final Options options = new Options();

    static final Logger LOGGER = LoggerFactory.getLogger("io.opencaesar.owl.fuseki_reasoner.OwlReasonIncrementallyJena1d");

    public static void main(String[] args) throws Exception {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        String loggerFactoryClassStr = loggerFactory.getClass().getName();

        System.out.println("Logger Factory: " + loggerFactoryClassStr);

        final OwlReasonIncrementallyJena1d app = new OwlReasonIncrementallyJena1d();
        final JCommander builder = JCommander.newBuilder().addObject(app.options).build();
        builder.parse(args);
        if (app.options.help) {
            builder.usage();
            return;
        }
        app.run1();
    }

    public OwlReasonIncrementallyJena1d() {
        JenaSystem.init();
    }

    private void queryString(String queryString, Dataset ds, boolean showResults) {
        LOGGER.info("<<< query: " + queryString);
        Query query = QueryFactory.create(queryString);
        int nbRows = 0;
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext(); ) {
                QuerySolution soln = results.nextSolution();
                if (showResults)
                    LOGGER.info(soln.toString());
                nbRows = results.getRowNumber();
            }
        }

        LOGGER.info(">>> query ("+nbRows+" results)");
    }

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

        Dataset ds0 = TDB2Factory.createDataset();

        for (var iri : options.inputOntologyIris) {
            Txn.executeWrite(ds0, () -> {
                Model m = ModelFactory.createDefaultModel();
                fm.readModelInternal(m, iri);
                ds0.addNamedModel(iri, m);
            });
        }

        // Create a union model and set it as the default model of the dataset.
        Model unionModel = ModelFactory.createDefaultModel();
        Txn.executeRead(ds0, () -> {
            Iterator<Resource> it = ds0.listModelNames();
            while (it.hasNext()) {
                Resource r = it.next();
                unionModel.add(ds0.getNamedModel(r));
            }
        });
        Txn.executeWrite(ds0, () -> {
            ds0.setDefaultModel(unionModel);
        });

        Txn.executeRead(ds0, () -> {
            queryString("SELECT ?g ?s ?p ?o { GRAPH ?g { ?s ?p ?o} }", ds0, false);
            queryString("SELECT * {?s ?p ?o}", ds0, false);
        });

        Model baseModel = ds0.getDefaultModel();

        Resource cr = ModelFactory.createDefaultModel().createResource();
        cr.addProperty(ReasonerVocabulary.PROPderivationLogging, "true");
        cr.addProperty(ReasonerVocabulary.PROPenableOWLTranslation, "true");
        cr.addProperty(ReasonerVocabulary.PROPenableTGCCaching, "true");
        cr.addProperty(ReasonerVocabulary.PROPtraceOn, "false");
        cr.addProperty(ReasonerVocabulary.PROPruleMode, GenericRuleReasoner.HYBRID.toString());
        cr.addProperty(ReasonerVocabulary.PROPruleSet, "owl-fuseki-reasoner/src/main/resources/mission.rules");
        Reasoner gr = GenericRuleReasonerFactory.theInstance().create(cr);

        InfModel infModel = ModelFactory.createInfModel(gr, baseModel);

        // Wrapping the infModel results in an unsupportedMethod exception when executing SPARQL queries.
        // Dataset ds1 = DatasetFactory.wrap(infModel);
        Dataset ds1 = DatasetFactory.create(infModel);

        Txn.executeRead(ds1, () -> {
            queryString("SELECT * {?s ?p ?o}", ds1, false);
            queryPresentsByGraph(ds1, true);
            queryPresentsByUnion(ds1, true);
            LOGGER.info("valid = " + infModel.validate().isValid());
            LOGGER.info("statements (base) = " + baseModel.getGraph().size());
            LOGGER.info("statements (inf)  = " + infModel.getGraph().size());
        });


        Txn.executeWrite(ds1, () -> {
            UpdateRequest request = UpdateFactory.create();
            request.add(
                    "INSERT DATA { GRAPH <http://imce.jpl.nasa.gov/foundation/mission> { <http://example.com#c1> a <http://imce.jpl.nasa.gov/foundation/mission#Component> } }");
            LOGGER.info("INSERT...");
            UpdateAction.execute(request, ds1);
        });

        Txn.executeRead(ds1, () -> {
            LOGGER.info("statements2 = " + infModel.getGraph().size());
            LOGGER.info("valid = " + infModel.validate().isValid());
        });


        Txn.executeWrite(ds1, () -> {
            UpdateRequest request = UpdateFactory.create();
            request.add(
                    "DELETE DATA { GRAPH <http://imce.jpl.nasa.gov/foundation/mission> { <http://example.com#c1> a <http://imce.jpl.nasa.gov/foundation/mission#Component> } }");
            LOGGER.info("DELETE...");
            UpdateAction.execute(request, ds1);
        });

        Txn.executeRead(ds1, () -> {
            LOGGER.info("statements3 = " + infModel.getGraph().size());
            LOGGER.info("valid = " + infModel.validate().isValid());
        });

    }

    private void queryPresentsByGraph(Dataset ds, boolean showResults) {
        String queryString = String
                .format("PREFIX mission: <http://example.com/tutorial/vocabulary/mission#>\n"
                        + "SELECT ?g ?c ?i WHERE { GRAPH ?g { ?c mission:presents ?i . } }\n"
                        + "ORDER BY ?g ?c ?i");
        queryString(queryString, ds, showResults);
    }

    private void queryPresentsByUnion(Dataset ds, boolean showResults) {
        String queryString = String
                .format("PREFIX mission: <http://example.com/tutorial/vocabulary/mission#>\n"
                        + "SELECT ?c ?i WHERE { ?c mission:presents ?i . }\n"
                        + "ORDER BY ?c ?i");
        queryString(queryString, ds, showResults);
    }

    //--------

    /**
     * Sort resources by label.
     *
     * @param resources resources
     * @param getLabel  label function
     * @param <T>       resource type.
     * @return sorted resources by their labels.
     */
    public static <T extends Resource> List<T> sortResourcesi(List<T> resources, Function<RDFNode, String> getLabel) {
        var filtered = resources.stream().filter(i -> !i.isAnon()).collect(Collectors.toList());
        filtered.sort((x1, x2) -> getLabel.apply(x1).compareTo(getLabel.apply(x2)));
        return filtered;
    }

    /**
     * sort resources by iri.
     *
     * @param resources resources
     * @param <T>       resource type.
     * @return sorted resources by iri.
     */
    public static <T extends Resource> List<T> sortByIri(List<T> resources) {
        return sortResourcesi(resources, i -> ((Resource) i).getURI());
    }

    /**
     * sort resources by name
     *
     * @param resources resources.
     * @param <T>       resource type.
     * @return sorted resources by name.
     */
    public static <T extends Resource> List<T> sortByName(List<T> resources) {
        return sortResourcesi(resources, i -> localName((Resource) i));
    }

    private static String localName(Resource resource) {
        var iri = resource.getURI();
        int index = iri.lastIndexOf("#");
        if (index == -1) {
            index = iri.lastIndexOf("/");
        }
        return (index != -1) ? iri.substring(index + 1) : resource.getLocalName();
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
                        extensions.stream().reduce((x, y) -> x + " " + y));
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
                        " recognized RDF extensions are: " + extensions);
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
        public OutputFolderPathValidator() {
        }

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