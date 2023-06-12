package io.opencaesar.owl.reason_incrementally;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.opencaesar.owl.doc.OwlCatalog;
import io.opencaesar.owl.doc.OwlDocApp;
import io.opencaesar.owl.doc.OwlDocApp.CatalogPathValidator;
import io.opencaesar.owl.doc.OwlDocApp.FileExtensionValidator;
import io.opencaesar.owl.doc.OwlDocApp.OutputFolderPathValidator;
import io.opencaesar.owl.doc.OwlDocApp.URLConverter;
import openllet.aterm.ATermAppl;
import openllet.core.KnowledgeBase;
import openllet.core.boxes.abox.ABox;
import openllet.core.boxes.tbox.TBox;
import openllet.jena.PelletInfGraph;
import openllet.jena.PelletReasoner;
import openllet.jena.PelletReasonerFactory;
import openllet.shared.tools.Log;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Experiments with incremental reasoning.
 */
public class OwlReasonIncrementallyJena {

    /**
     * The default OWL file extensions
     */
    public static final String[] DEFAULT_EXTENSIONS = {"owl", "ttl"};

    private static final List<String> extensions = Arrays.asList(
            "fss", "owl", "rdf", "xml", "n3", "ttl", "rj", "nt", "jsonld", "trig", "trix", "nq"
    );

    private static final String CSS_DEFAULT = "default.css";
    private static final String CSS_MAIN = "main.css";

    private static final String IMG_ONTOLOGY = "https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/images/org.eclipse.jdt.ui/obj16/package_obj.svg";
    private static final String IMG_CLASS = "https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/images/org.eclipse.jdt.ui/obj16/methpub_obj.svg";
    private static final String IMG_DATATYPE = "https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/images/org.eclipse.jdt.ui/obj16/methpri_obj.svg";
    private static final String IMG_PROPERTY = "https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/images/org.eclipse.jdt.ui/obj16/methpro_obj.svg";
    private static final String IMG_INDIVIDUAL = "https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/images/org.eclipse.jdt.ui/obj16/field_public_obj.svg";
    private static final String IMG_ITEM = "https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/images/org.eclipse.jdt.ui/obj16/methdef_obj.svg";

    private static class Options {
        @Parameter(
                names = { "--input-catalog-path", "-c"},
                description = "path to the input OWL catalog (Required)",
                validateWith = CatalogPathValidator.class,
                required = true,
                order = 1)
        private String inputCatalogPath;

        @Parameter(
                names={"--input-catalog-title", "-t"},
                description="Title of OML input catalog (Optional)",
                order=2)
        private String inputCatalogTitle = "OWL Ontology Index";

        @Parameter(
                names={"--input-catalog-version", "-v"},
                description="Version of OML input catalog (Optional)",
                order=3)
        private String inputCatalogVersion = "";

        @Parameter(
                names = { "--input-ontology-iri", "-i"},
                description = "iri of input OWL ontology (Optional, by default all ontologies in catalog)",
                order = 4)
        private List<String> inputOntologyIris = new ArrayList<>();

        @Parameter(
                names = {"--input-file-extension", "-e"},
                description = "input file extension (owl and ttl by default, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss)",
                validateWith = FileExtensionValidator.class,
                order = 5)
        private List<String> inputFileExtensions = new ArrayList<>(Arrays.asList(DEFAULT_EXTENSIONS));

        @Parameter(
                names= { "--output-folder-path", "-o" },
                description="Path of Bikeshed output folder",
                validateWith=OutputFolderPathValidator.class,
                required=true,
                order=6)
        private String outputFolderPath = ".";

        @Parameter(
                names= { "--output-case-sensitive","-s" },
                description="Whether output paths are case sensitive",
                help=true,
                order=7)
        private boolean outputCaseSensitive;

        @Parameter(
                names= { "--css-file-path","-css" },
                description="Path of a css file",
                converter = URLConverter.class,
                help=true,
                order=8)
        private URL cssFilePath = OwlReasonIncrementallyJena.class.getClassLoader().getResource(CSS_DEFAULT);

        @Parameter(
                names = {"--debug", "-d"},
                description = "Shows debug logging statements",
                order=9)
        private boolean debug;

        @Parameter(
                names = {"--help", "-h"},
                description = "Displays summary of options",
                help = true,
                order=10)
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
            ontologies = OwlDocApp.sortByIri(ontModel.listOntologies().filterKeep(i-> hasTerms(i)).toList());
            classes = OwlDocApp.sortByName(ontModel.listNamedClasses().toList());
            datatypes = OwlDocApp.sortByName(ontModel.listSubjectsWithProperty(RDF.type, RDFS.Datatype).toList());
            annotationProperties = OwlDocApp.sortByName(ontModel.listAnnotationProperties().toList());
            objectProperties = OwlDocApp.sortByName(ontModel.listObjectProperties().toList());
            datatypeProperties = OwlDocApp.sortByName(ontModel.listDatatypeProperties().toList());
            individuals = OwlDocApp.sortByName(ontModel.listIndividuals().toList());
        }

        private boolean hasTerms(Ontology o) {
            var iri = o.getURI();
            var resources = ontModel.listResourcesWithProperty(RDF.type)
                    .filterDrop(i -> i.isAnon())
                    .filterKeep(i -> i.getURI().startsWith(iri))
                    .toList();
            resources.removeIf(i -> ontModel.getOntology(i.getURI()) != null);
            return !resources.isEmpty();
        }
    }

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
        app.run();
    }

    public OwlReasonIncrementallyJena() {}

    private void run() throws Exception {

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

        OntModelSpec s = new OntModelSpec(OntModelSpec.OWL_MEM);
        s.setDocumentManager(mgr);
        OntModel ontModel = ModelFactory.createOntologyModel(s);
        for (var iri : options.inputOntologyIris) {
            fm.readModelInternal(ontModel, iri);
        }

        LOGGER.info(ontModel.size() + " asserted axioms\n");

        final PelletReasoner r = PelletReasonerFactory.theInstance().create();
        final PelletInfGraph pig = r.bind(ontModel.getGraph());

        final KnowledgeBase kb0 = pig.getKB();
        final TBox tb0 = kb0.getTBox();
        final Collection<ATermAppl> tas0 = tb0.getAxioms();
        final Collection<ATermAppl> taas0 = tb0.getAssertedAxioms();
        LOGGER.info("tbox0: "+tas0.size()+" axioms; "+taas0.size()+" asserted axioms\n");
        ABox ab0 = kb0.getABox();
        LOGGER.info("abox0: "+ab0.size()+" statements\n");

        pig.classify();
        pig.realize();
        final KnowledgeBase kb1 = pig.getKB();
        LOGGER.info("kb.isConsistencyDone()? : "+kb1.isConsistencyDone()+"\n");
        if (kb1.isConsistent()) {
            LOGGER.info("consistency: OK\n");
        } else {
            LOGGER.warning("consistency: Conflicts\n");
            ValidityReport validity = pig.validate();
            Iterator<ValidityReport.Report> i = validity.getReports();
            while (i.hasNext()) {
                LOGGER.warning(" - " + i.next());
            }
        }
        final TBox tb1 = kb1.getTBox();
        final Collection<ATermAppl> tas = tb1.getAxioms();
        final Collection<ATermAppl> taas = tb1.getAssertedAxioms();
        int nAsserted = 0;
        int nDerived = 0;
        LOGGER.info("tbox: "+tas.size()+" axioms; "+taas.size()+" asserted axioms\n");

        final Set<ATermAppl> tDerived = new HashSet<>();
        final Set<ATermAppl> tAsserted = new HashSet<>();

        for (final ATermAppl ta: tas) {
            final String repr = ATermApplVisitor.convert(ta);
            if (taas.contains(ta)) {
                nAsserted++;
                tAsserted.add(ta);
                System.out.println("asserted:" + repr);
            } else {
                nDerived++;
                tDerived.add(ta);
                System.out.println("derived:" + repr);
            }
        }


        LOGGER.info("tbox: "+(tas.size() - taas.size()) +" derived axioms; "+taas.size()+" asserted axioms\n");
        LOGGER.info("tbox: "+nDerived+" derived axioms; "+nAsserted+" asserted axioms\n");

        int extraDerived = 0;
        for (final ATermAppl ta : tas) {
            if (!taas.contains(ta) && !tDerived.contains(ta)) {
                extraDerived++;
                final String repr = ATermApplVisitor.convert(ta);
                LOGGER.warning("tb.getAxioms has an extra derived axiom: " + repr + "\n");
            }
        }
        int extraAsserted = 0;
        for (final ATermAppl ta : taas) {
            if (!tas.contains(ta) && !tAsserted.contains(ta)) {
                extraAsserted++;
                final String repr = ATermApplVisitor.convert(ta);
                LOGGER.warning("tb.getAssertedAxioms has an extra asserted axiom: " + repr + "\n");
            }
        }

        LOGGER.info("tbox0: "+tas0.size()+" axioms; "+taas0.size()+" asserted axioms\n");
        LOGGER.info("abox0: "+ab0.size()+" statements\n");

        LOGGER.info("tbox1: "+(tas.size() - taas.size()) +" derived axioms; "+taas.size()+" asserted axioms\n");
        LOGGER.info("tbox1: "+extraDerived+" extra derived, "+nDerived+" derived axioms; "+extraAsserted+" extra asserted, "+nAsserted+" asserted axioms\n");

        ABox ab1 = kb1.getABox();
        LOGGER.info("abox1: "+ab1.size()+" statements\n");


    }

}