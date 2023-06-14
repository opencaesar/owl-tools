package io.opencaesar.owl.reason_incrementally;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import io.opencaesar.owl.reason.OwlReasonApp;
import io.opencaesar.owl.reason.XMLCatalogIRIMapper;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.N3DocumentFormat;
import org.semanticweb.owlapi.formats.NQuadsDocumentFormat;
import org.semanticweb.owlapi.formats.NTriplesDocumentFormat;
import org.semanticweb.owlapi.formats.RDFJsonDocumentFormat;
import org.semanticweb.owlapi.formats.RDFJsonLDDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RioTurtleDocumentFormat;
import org.semanticweb.owlapi.formats.TrigDocumentFormat;
import org.semanticweb.owlapi.formats.TrixDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import openllet.core.KnowledgeBase;
import openllet.jena.ModelExtractor;
import openllet.jena.ModelExtractor.StatementType;
import openllet.jena.vocabulary.OWL2;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;
/**
 * Experiments with incremental reasoning.
 */
public class OwlReasonIncrementallyOWLAPI {

    private static final String CONSISTENCY = "Consistency";
    private static final String SATISFIABILITY = "Satisfiability";

    private static final Map<String, OWLDocumentFormat> extensions = new HashMap<>();
    static {
        extensions.put("fss", new FunctionalSyntaxDocumentFormat());
        // triple formats
        extensions.put("owl", new RDFXMLDocumentFormat());
        extensions.put("rdf", new RDFXMLDocumentFormat());
        extensions.put("xml", new RDFXMLDocumentFormat());
        extensions.put("n3", new N3DocumentFormat());
        extensions.put("ttl", new RioTurtleDocumentFormat());
        extensions.put("rj", new RDFJsonDocumentFormat());
        extensions.put("nt", new NTriplesDocumentFormat());
        // quad formats
        extensions.put("jsonld", new RDFJsonLDDocumentFormat());
        extensions.put("trig", new TrigDocumentFormat());
        extensions.put("trix", new TrixDocumentFormat());
        extensions.put("nq", new NQuadsDocumentFormat());
    }

    private final Options options = new Options();

    /**
     * default input ontology file extension.
     */
    public static final String DEFAULT_INPUT_FILE_EXTENSION = "owl";

    /**
     * default reasoner output file extension.
     */
    public static final String DEFAULT_OUTPUT_FILE_EXTENSION = "ttl";

    /**
     * default reasoner output explanation file extension.
     */
    public static final String DEFAULT_EXPLANATION_FORMAT = "owl";

    private static class Options {
        @Parameter(
                names = { "--catalog-path", "-c"},
                description = "path to the input OWL catalog (Required)",
                validateWith = OwlReasonApp.CatalogPath.class,
                required = true,
                order = 1)
        private String catalogPath;

        @Parameter(
                names = { "--input-ontology-iri", "-i"},
                description = "iri of input OWL ontology (Required)",
                required = true,
                order = 2)
        private String inputOntologyIri;

        @Parameter(
                names = {"--report-path", "-r"},
                description = "path to a report file in Junit XML format ",
                validateWith = OwlReasonApp.ReportPathValidator.class,
                required = true,
                order = 4)
        private String reportPath;

        @Parameter(
                names = {"--input-file-extension", "-if"},
                description = "input file extension (owl by default, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss)",
                validateWith = OwlReasonApp.FileExtensionValidator.class,
                order = 5)
        private List<String> inputFileExtensions = new ArrayList<>();
        {
            inputFileExtensions.add(DEFAULT_INPUT_FILE_EXTENSION);
        }

        @Parameter(
                names = {"--output-file-extension", "-of"},
                description = "output file extension (ttl by default, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss)",
                validateWith = OwlReasonApp.OutputFileExtensionValidator.class,
                order = 6)
        private String outputFileExtension = DEFAULT_OUTPUT_FILE_EXTENSION;

        @Parameter(
                names = {"--explanation-format", "-ef"},
                description = "Explanation format (owl by default, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss)",
                validateWith = OwlReasonApp.ExplanationFormatValidator.class,
                order = 7)
        private String explanationFormat = DEFAULT_EXPLANATION_FORMAT;

        @Parameter(
                names = {"--remove-unsats", "-ru"},
                description = "boolean indicating whether to remove entailments due to unsatisfiability (optional, default=true)",
                arity = 1,
                order = 8)
        private boolean removeUnsats = true;

        @Parameter(
                names = {"--remove-backbone", "-rb"},
                description = "boolean indicating whether to remove axioms on the backhone from entailments (optional, default=true)",
                arity = 1,
                order = 9)
        private boolean removeBackbone = true;

        @Parameter(
                names = {"--backbone-iri", "-b"},
                description = "iri of backbone ontology",
                order = 10)
        private String backboneIri = "http://opencaesar.io/oml";

        @Parameter(
                names = {"--indent", "-n"},
                description = "indent of the JUnit XML elements",
                order = 11)
        private int indent = 2;

        @Parameter(
                names = {"--debug", "-d"},
                description = "Shows debug logging statements",
                order = 12)
        private boolean debug;

        @Parameter(
                names = {"--help", "-h"},
                description = "Displays summary of options",
                help = true,
                order =13)
        private boolean help;
    }

    private final static Logger LOGGER = Logger.getLogger(OwlReasonIncrementallyOWLAPI.class);
    static {
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
    }

    public static void main(String[] args) throws Exception {
        final OwlReasonIncrementallyOWLAPI app = new OwlReasonIncrementallyOWLAPI();
        final JCommander builder = JCommander.newBuilder().addObject(app.options).build();
        builder.parse(args);
        if (app.options.help) {
            builder.usage();
            return;
        }
        if (app.options.debug) {
            final Appender appender = LogManager.getRootLogger().getAppender("ConsoleAppender");
            ((AppenderSkeleton) appender).setThreshold(Level.DEBUG);
        }
        app.run();
    }

    public OwlReasonIncrementallyOWLAPI() {}


//        -i
//        http://srl.jpl.nasa.gov/efse/bundle
//        http://srl.jpl.nasa.gov/efse/bundle/classes=ALL_SUBCLASS
//        -s
//        http://srl.jpl.nasa.gov/efse/bundle/properties=INVERSE_PROPERTY|ALL_SUBPROPERTY
//        -s
//        http://srl.jpl.nasa.gov/efse/bundle/individuals=ALL_INSTANCE|DATA_PROPERTY_VALUE|OBJECT_PROPERTY_VALUE|SAME_AS

    static final EnumSet<StatementType> classStatementTypes = EnumSet.of(StatementType.ALL_SUBCLASS);
    static final EnumSet<StatementType> propertyStatementTypes = EnumSet.of(StatementType.INVERSE_PROPERTY, StatementType.ALL_SUBPROPERTY);
    static final EnumSet<StatementType> individualStatementTypes = EnumSet.of(StatementType.ALL_INSTANCE, StatementType.DATA_PROPERTY_VALUE, StatementType.OBJECT_PROPERTY_VALUE, StatementType.SAME_AS);

    private void run() throws Exception {
        LOGGER.info("=================================================================");
        LOGGER.info("                        S T A R T");
        LOGGER.info("                     OWL Reason Incrementally w/ OWL API+Pellet" + getAppVersion());
        LOGGER.info("=================================================================");

        // Create ontology manager.

        LOGGER.info("create ontology manager");
        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        if (manager == null) {
            throw new RuntimeException("couldn't create owl ontology manager");
        }
        LOGGER.debug("add location mappers");
        manager.getIRIMappers().add((new XMLCatalogIRIMapper(new File(options.catalogPath), options.inputFileExtensions)));

        final OWLDataFactory factory = manager.getOWLDataFactory();

        LOGGER.info("load ontology "+options.inputOntologyIri);
        final OWLOntology inputOntology = manager.loadOntology(IRI.create(options.inputOntologyIri));
        if (inputOntology == null) {
            throw new RuntimeException("couldn't load ontology");
        }

        // Get Pellet reasoner factory.

        LOGGER.info("create pellet reasoner factory");
        final OpenlletReasonerFactory reasonerFactory = OpenlletReasonerFactory.getInstance();

        // Create a non-buffering Pellet reasoner.

        // A bufferring reasoner will not process any additions until manually refreshed.
        // Since refresshing the reasoner involves reloading the import closure of all ontologies,
        // it amounts to batch reasoning, not incremental reasoning.

        LOGGER.info("create pellet reasoner for "+inputOntology);
        PelletExplanation.setup();
        OpenlletReasoner reasoner = reasonerFactory.createNonBufferingReasoner(inputOntology);
        if (reasoner == null) {
            throw new RuntimeException("couldn't create reasoner");
        }

        manager.addOntologyChangeListener(reasoner);


        // Create explanation format
        OWLDocumentFormat explanationFormat = extensions.get(options.explanationFormat);

        // Check the input ontology
        Entailments e1 = check(reasoner, explanationFormat, options.inputOntologyIri, 1);
        e1.describe();

        LOGGER.info("----- Add axioms -----\n");
        IRI niIRI = IRI.create("http://srl.jpl.nasa.gov/efse/assemblies#ATest");
        OWLNamedIndividual ni = factory.getOWLNamedIndividual(niIRI);
        OWLClass subsC = factory.getOWLClass(IRI.create("http://imce.jpl.nasa.gov/discipline/fse/fse#Subsystem"));
        ChangeApplied ca1 = manager.applyChanges(
                new AddAxiom(
                        inputOntology, factory.getOWLDeclarationAxiom(ni)
                ),
                new AddAxiom(
                        inputOntology, factory.getOWLClassAssertionAxiom(subsC, ni)
                )
        );
        assert ca1 == ChangeApplied.SUCCESSFULLY;

        Optional<OWLNamedIndividual> found1 = inputOntology.individualsInSignature().filter(x -> niIRI.equals(x.getIRI())).findFirst();
        assert found1.isPresent();

        Entailments e2 = check(reasoner, explanationFormat, options.inputOntologyIri, 2);
        e2.describe();
//            <owl:NamedIndividual rdf:about="http://srl.jpl.nasa.gov/efse/function-list#a0f23b63-d452-4c58-8f40-40ed3217ce93">
//                <rdf:type rdf:resource="http://imce.jpl.nasa.gov/library/types#PRT2Measurement"/>
//                <fse:connectsAssembly1 rdf:resource="http://srl.jpl.nasa.gov/efse/assemblies#ed910146-c43c-47a7-97de-373c72a90563"/>
//                <fse:connectsAssembly2 rdf:resource="http://srl.jpl.nasa.gov/efse/assemblies#8ab3dbe5-bd3e-4d99-8e51-14ea6e3607bf"/>
//                <fse:connectsSubsystem1 rdf:resource="http://srl.jpl.nasa.gov/efse/assemblies#Subsystem_2006"/>
//                <fse:connectsSubsystem2 rdf:resource="http://srl.jpl.nasa.gov/efse/assemblies#Subsystem_2014"/>
//                <mission:joins1 rdf:resource="http://srl.jpl.nasa.gov/efse/assemblies#ed910146-c43c-47a7-97de-373c72a90563_PRTin19"/>
//                <fse:hasEndCircuitType>Quiet</fse:hasEndCircuitType>
//                <fse:hasFunctionDirection>2to1</fse:hasFunctionDirection>
//                <fse:hasFunctionNumber>006-014-039</fse:hasFunctionNumber>
//                <fse:lifecycleState>Preliminary</fse:lifecycleState>
//                <base:hasCanonicalName>TLM UPPER LINK TEMP A</base:hasCanonicalName>
//            </owl:NamedIndividual>


        KnowledgeBase kb = reasoner.getKB();
        LOGGER.info(kb.getIndividualsCount() + " individuals (before removal)\n");

        LOGGER.info(inputOntology.individualsInSignature(Imports.INCLUDED).count() + " individuals in all ontologies (before removal)");

        LOGGER.info("----- Remove axioms -----\n");

        OWLOntology fl = manager.getOntology(IRI.create("http://srl.jpl.nasa.gov/efse/function-list"));
        assert null != fl;

        IRI fl2IRI = IRI.create("http://srl.jpl.nasa.gov/efse/function-list#a0f23b63-d452-4c58-8f40-40ed3217ce93");

        // Verify that the function list individual is present before removing it.
        Optional<OWLNamedIndividual> found2a = fl.individualsInSignature().filter(x -> fl2IRI.equals(x.getIRI())).findFirst();
        assert found2a.isPresent();
        OWLNamedIndividual flI = found2a.get();

        Set<OWLOntology> allOntologies = inputOntology.getImportsClosure();
        LOGGER.info(allOntologies.size() + " ontologies");

        Set<OWLSameIndividualAxiom> sameAs = inputOntology.getSameIndividualAxioms(flI);
        LOGGER.info(sameAs.size() + " sameAs axioms about individual: "+fl2IRI);

        List<RemoveAxiom> changes = new ArrayList<>();
        changes.addAll(inputOntology.importsClosure().flatMap(o -> o.axioms(flI).map(ax -> new RemoveAxiom(o, ax))).toList());

        // This has no effect.
        changes.add(new RemoveAxiom(
                fl, factory.getOWLSameIndividualAxiom(flI, flI)
        ));

        LOGGER.info("Removing "+changes.size() + " axioms about individual: "+fl2IRI);

        ChangeApplied ca2 = manager.applyChanges(changes);
        assert ca2 == ChangeApplied.SUCCESSFULLY;

        LOGGER.info(inputOntology.individualsInSignature(Imports.INCLUDED).count() + " individuals in all ontologies (after removal)");

        // Verify that the function list individual is absent after removing it.
        Optional<OWLNamedIndividual> found2b = fl.individualsInSignature().filter(x -> fl2IRI.equals(x.getIRI())).findFirst();
        assert found2b.isEmpty();

        Optional<OWLNamedIndividual> found2c = reasoner.getOntology().individualsInSignature(Imports.INCLUDED).filter(x -> fl2IRI.equals(x.getIRI())).findFirst();
        assert found2c.isEmpty();

        Optional<OWLNamedIndividual> found2d = inputOntology.individualsInSignature(Imports.INCLUDED).filter(x -> fl2IRI.equals(x.getIRI())).findFirst();
        assert found2d.isEmpty();

        List<OWLNamedIndividual> shouldBeEmpty = inputOntology.individualsInSignature(Imports.INCLUDED).filter(x -> fl2IRI.equals(x.getIRI())).toList();
        assert shouldBeEmpty.isEmpty();

        LOGGER.info(kb.getIndividualsCount() + " individuals (after removal, before refresh)");

        // Force Pellet to recompute consistency.
        reasoner.refresh();

        LOGGER.info(kb.getIndividualsCount() + " individuals (after removal and refresh)");

        Entailments e3 = check(reasoner, explanationFormat, options.inputOntologyIri, 3);
        e3.describe();

        LOGGER.info(kb.getIndividualsCount() + " individuals (after reasoner check and entailment extraction)");

        Optional<OWLNamedIndividual> found3 = reasoner.getOntology().individualsInSignature(Imports.INCLUDED).filter(x -> fl2IRI.equals(x.getIRI())).findFirst();
        assert found3.isEmpty();

        LOGGER.info("=================================================================");
        LOGGER.info("                          E N D");
        LOGGER.info("=================================================================");
    }

    private Entailments check(final OpenlletReasoner reasoner, OWLDocumentFormat explanationFormat, String inputOntologyIri, int suffix) throws Exception {


        // Create PelletExplanation.

        LOGGER.trace("create explanation for "+inputOntologyIri);
        PelletExplanation explanation = new PelletExplanation(reasoner);

        // Create knowledge base.

        LOGGER.trace("create knowledge base and extractor");
        KnowledgeBase kb = reasoner.getKB();
        if (kb == null) {
            throw new RuntimeException("couldn't get knowledge base");
        }

        // Check for consistency and satisfiability

        Map<String, List<OwlReasonApp.Result>> allResults = new LinkedHashMap<>();
        allResults.put(CONSISTENCY, checkConsistency(inputOntologyIri, reasoner, explanation, explanationFormat));
        boolean isConsistent = allResults.get(CONSISTENCY).stream().noneMatch(r -> r.explanation != null);
        boolean isSatisfiable = false;
        if (isConsistent) {
            allResults.put(SATISFIABILITY, checkSatisfiability(inputOntologyIri, reasoner, explanation, explanationFormat));
            isSatisfiable = allResults.get(SATISFIABILITY).stream().noneMatch(r -> r.explanation != null);
        }
        writeResults(inputOntologyIri, allResults, options.indent);

        // Check Results

        if (!isConsistent) {
            LOGGER.error("Check "+options.reportPath+" for more details.");
            throw new OwlReasonApp.ReasoningException("Ontology is inconsistent. Check " + options.reportPath + " for more details.");
        }
        if (!isSatisfiable) {
            LOGGER.error("Check "+options.reportPath+" for more details.");
            throw new OwlReasonApp.ReasoningException("Ontology has insatisfiabilities. Check " + options.reportPath + " for more details.");
        }

        // Iterate over specs and extract entailments.

        String classesOutputIri = inputOntologyIri + "/classes" + suffix;
        OntModel classesEntailments = extractAndSaveEntailments(kb, inputOntologyIri, classesOutputIri, classStatementTypes, reasoner.getManager());

        String propertiesOutputIri = inputOntologyIri + "/properties" + suffix;
        OntModel propertiesEntailments = extractAndSaveEntailments(kb, inputOntologyIri, propertiesOutputIri, propertyStatementTypes, reasoner.getManager());

        String individualsOutputIri = inputOntologyIri + "/individuals" + suffix;
        OntModel individualEntailments = extractAndSaveEntailments(kb, inputOntologyIri, individualsOutputIri, individualStatementTypes, reasoner.getManager());

        return new Entailments(classesEntailments, propertiesEntailments, individualEntailments);
    }

    private List<OwlReasonApp.Result> checkConsistency(String ontologyIri, OpenlletReasoner reasoner, PelletExplanation explanation, OWLDocumentFormat explanationFormat) throws Exception {
        LOGGER.trace("test consistency on "+ontologyIri);
        List<OwlReasonApp.Result> results = new ArrayList<>();

        long s = System.currentTimeMillis();
        boolean success = reasoner.isConsistent();
        long e = System.currentTimeMillis();

        if (success) {
            LOGGER.info("Ontology "+ontologyIri+" is consistent ("+(e-s)+" ms)");
        } else {
            LOGGER.error("Ontology "+ontologyIri+" is inconsistent ("+(e-s)+" ms)");
        }

        OwlReasonApp.Result result = new OwlReasonApp.Result();
        result.name = ontologyIri;
        if (!success) {
            //TODO: consider using explanation.getInconsistencyExplanations()
            Set<OWLAxiom> axioms = explanation.getInconsistencyExplanation();
            result.message = reasoner.getKB().getExplanation();
            result.explanation = createExplanationOntology(axioms, explanationFormat);
        }
        results.add(result);

        return results;
    }

    private List<OwlReasonApp.Result> checkSatisfiability(String ontologyIri, OpenlletReasoner reasoner, PelletExplanation explanation, OWLDocumentFormat explanationFormat) throws Exception {
        LOGGER.trace("test satisfiability on "+ontologyIri);
        List<OwlReasonApp.Result> results = new ArrayList<>();

        Set<OWLClass> allClasses = reasoner.getRootOntology().classesInSignature(Imports.INCLUDED).collect(Collectors.toSet());

        int numOfClasses = allClasses.size();
        LOGGER.trace(numOfClasses+" total classes");

        int count = 0;
        int numOfUnsat = 0;
        for (OWLClass klass : allClasses) {
            if (options.removeBackbone && klass.getIRI().getIRIString().startsWith(options.backboneIri))
                continue;
            if (klass.isOWLNothing()) // owl:Nothing should not be checked
                continue;
            String className = klass.getIRI().getIRIString();
            LOGGER.trace(className+" "+ ++count+" of "+numOfClasses);

            boolean success = reasoner.isSatisfiable(klass);
            LOGGER.trace("class "+className+" is "+(success?"":"un")+"satisfiable");

            OwlReasonApp.Result result = new OwlReasonApp.Result();
            results.add(result);
            result.name = className;

            if (!success) {
                result.message = "class "+className+" is insatisfiable";
                result.explanation = createExplanationOntology(explanation.getUnsatisfiableExplanation(klass), explanationFormat);
                numOfUnsat += 1;
            }
        }
        if (numOfUnsat > 0) {
            LOGGER.error("Ontology "+ontologyIri+" has "+numOfUnsat+" insatisfiabilities");
        }

        return results;
    }

    private String createExplanationOntology(Set<OWLAxiom> axioms, OWLDocumentFormat format) throws Exception {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        if (manager == null ) {
            throw new RuntimeException("couldn't create owl ontology manager");
        }
        OWLOntology ontology = manager.createOntology(axioms);
        if (ontology == null) {
            throw new RuntimeException("couldn't create ontology for explanation");
        }
        StringDocumentTarget target = new StringDocumentTarget();
        manager.saveOntology(ontology, format, target);
        return target.toString();
    }

    private void writeResults(String ontologyIri, Map<String, List<OwlReasonApp.Result>> allResults, int indent) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();
        Element tss = doc.createElement("testsuites");
        tss.setAttribute("name", ontologyIri);
        doc.appendChild(tss);

        allResults.forEach((test, results) -> {
            Element ts = doc.createElement("testsuite");
            ts.setAttribute("name", test);
            tss.appendChild(ts);
            results.forEach(result -> {
                Element tc = doc.createElement("testcase");
                tc.setAttribute("name", result.name);
                ts.appendChild(tc);
                if (result.explanation != null) {
                    Element fl = doc.createElement("failure");
                    tc.appendChild(fl);
                    fl.setAttribute("message", result.message);
                    CDATASection cdoc = doc.createCDATASection("\n"+result.message+"\n\n"+result.explanation+"\n");
                    fl.appendChild(cdoc);
                }
            });
        });

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", ""+indent);
        DOMSource source = new DOMSource(doc);
        File reportFile = new File(options.reportPath);
        try (FileOutputStream stream = new FileOutputStream(reportFile)) {
            StreamResult console = new StreamResult(stream);
            transformer.transform(source, console);
        }
    }

    private OntModel extractAndSaveEntailments(KnowledgeBase kb, String inputOntologyIri, String outputOntologyIri, EnumSet<StatementType> statementTypes, OWLOntologyManager manager) throws Exception {
        // Create extractor.

        LOGGER.trace("create extractor for "+statementTypes);
        LOGGER.trace("extract entailments for "+statementTypes);
        LOGGER.trace("remove trivial entailments for "+statementTypes);
        LOGGER.trace("remove backbone entailments for "+statementTypes);

        long s = System.currentTimeMillis();

        ModelExtractor extractor = new ModelExtractor(kb);

        // Extract entailments
        Model entailments = extractEntailments(extractor, statementTypes);

        // Remove trivial axioms involving owl:Thing and owl:Nothing.
        entailments = removeTrivial(entailments, options.removeUnsats);

        // Remove backbone entailments.
        if (options.removeBackbone) {
            entailments = removeBackbone(entailments, options.backboneIri);
        }
        long e1 = System.currentTimeMillis();

        // Create Jena ontology model for results.
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, entailments);

        // Create Jena ontology from model.
        Ontology outputOntology = model.createOntology(outputOntologyIri);
        outputOntology.addImport(ResourceFactory.createResource(inputOntologyIri));
        outputOntology.addComment("Generated by Owl Reason "+ getAppVersion(), null);
        outputOntology.addVersionInfo(""+Instant.now().getEpochSecond());

        long e2 = System.currentTimeMillis();

        // Create an empty OWLAPI Ontology to get the ontology document IRI

        OWLOntology empty = manager.createOntology(IRI.create(outputOntologyIri+"."+options.outputFileExtension));
        String filename = URI.create(manager.getOntologyDocumentIRI(empty).toString()).getPath();
        manager.removeOntology(empty);

        // Open output stream.

        File outputFile = new File(filename);
        //noinspection ResultOfMethodCallIgnored
        outputFile.getParentFile().mkdirs();
        FileOutputStream outputFileStream = new FileOutputStream(outputFile);

        // Serialize Jena ontology model to output stream.

        Lang lang = RDFLanguages.fileExtToLang(options.outputFileExtension);

        model.write(outputFileStream, lang.getName());
        long e3 = System.currentTimeMillis();

        LOGGER.info("extract entailments: ("+(e1-s)+" ms) create Jena model from results: ("+(e2-e1)+" ms) Serialize: ("+(e3-e2)+" ms)");
        LOGGER.info("extract entailments: filename="+filename);

        return model;
    }

    private Model extractEntailments(ModelExtractor extractor, EnumSet<StatementType> types) {
        // Extract entailments.
        extractor.setSelector(types);
        Model result = extractor.extractModel();
        LOGGER.trace("extracted "+result.size()+" entailed axioms");
        return result;
    }

    /*
     *  Remove trivial entailments involving owl:Thing, owl:Nothing, owl:topObjectProperty, owl:topDataProperty
     */
    private Model removeTrivial(Model entailments, boolean removeUnsats) {
        StmtIterator iterator = entailments.listStatements();
        List<Statement> trivial = new ArrayList<>();
        while (iterator.hasNext()) {
            Statement statement = iterator.next();
            Resource subject = statement.getSubject();
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();
            if ((predicate.equals(RDFS.subClassOf) && (subject.equals(OWL2.Nothing) || (removeUnsats && object.equals(OWL2.Nothing)) || object.equals(OWL2.Thing))) ||
                    (predicate.equals(RDFS.subPropertyOf) && (object.equals(OWL2.topObjectProperty) || object.equals(OWL2.topDataProperty)) || (subject.equals(OWL2.bottomObjectProperty) || subject.equals(OWL2.bottomDataProperty)))) {
                trivial.add(statement);
            }
        }
        entailments.remove(trivial);
        LOGGER.trace("removed "+trivial.size()+" trivial axioms");
        return entailments;
    }

    /*
     * Remove entailments involving backbone items.
     */
    private Model removeBackbone(Model entailments, String pattern) {
        StmtIterator iterator = entailments.listStatements();
        List<Statement> backbone = new ArrayList<>();
        while (iterator.hasNext()) {
            Statement statement = iterator.next();
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();
            if (object instanceof Resource) {
                String objectIri = ((Resource)object).getURI();
                if ((predicate.equals(RDFS.subClassOf) || predicate.equals(RDFS.subPropertyOf)) && objectIri.startsWith(pattern)) {
                    backbone.add(statement);
                }
            }
        }
        entailments.remove(backbone);
        LOGGER.trace("removed "+backbone.size()+" backbone axioms");
        return entailments;
    }

    private String getAppVersion() {
        var version = this.getClass().getPackage().getImplementationVersion();
        return (version != null) ? version : "<SNAPSHOT>";
    }

    public record Entailments(OntModel classes, OntModel properties, OntModel individuals) {

        public void describe() {
            LOGGER.info("Statements: classes="+classes.size() + ", properties="+properties.size()+", individuals="+individuals.size()+"\n");
        }
    }
}