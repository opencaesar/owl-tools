/**
 * Copyright 2019 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opencaesar.owl.reason;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.jena.datatypes.RDFDatatype;
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
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
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
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLQuantifiedDataRestriction;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
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
import openllet.core.OpenlletOptions;
import openllet.jena.ModelExtractor;
import openllet.jena.ModelExtractor.StatementType;
import openllet.jena.vocabulary.OWL2;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;

/**
 * A utility for running a reasoner on the set of ontologies in scope of an OASIS XML catalog.
 */
public class OwlReasonApp {
  
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
			validateWith = CatalogPath.class,
			required = true)
		private String catalogPath;
		
		@Parameter(
			names = { "--input-ontology-iri", "-i"},
			description = "iri of input OWL ontology (Required)",
			required = true)
		private String inputOntologyIri;
		
		@Parameter(
			names = {"--spec", "-s"},
			description = "output-ontology-iri= list of entailment statement types separarted by | (Required)",
			converter = SpecConverter.class,
			required = true)
		private List<Spec> specs = new ArrayList<>();
		
		@Parameter(
			names = {"--report-path", "-r"},
			description = "path to a report file in Junit XML format ",
			validateWith = ReportPathValidator.class,
			required = true)
		private String reportPath;
		
		@Parameter(
			names = {"--output-iris-path", "-oi"},
			description = "path to a .txt file listing all analyzed ontology IRIs (one per line)",
			validateWith = IrisPathValidator.class,
			required = false)
		private String outputOntologyIrisPath;

		@Parameter(
			names = {"--input-file-extension", "-if"},
			description = "input file extension (owl by default, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss)",
			validateWith = FileExtensionValidator.class)
	    private List<String> inputFileExtensions = new ArrayList<>();
	    {
	    	inputFileExtensions.add(DEFAULT_INPUT_FILE_EXTENSION);
	    }
		
		@Parameter(
			names = {"--output-file-extension", "-of"},
			description = "output file extension (ttl by default, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss)",
			validateWith = OutputFileExtensionValidator.class)
	    private String outputFileExtension = DEFAULT_OUTPUT_FILE_EXTENSION;
		
		@Parameter(
			names = {"--explanation-format", "-ef"},
			description = "Explanation format (owl by default, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss)",
			validateWith = ExplanationFormatValidator.class)
	    private String explanationFormat = DEFAULT_EXPLANATION_FORMAT;

		@Parameter(
			names = {"--unique-names", "-un"},
			description = "boolean indicating whether to use the unique name assumption")
	    private boolean uniqueNames = false;

		@Parameter(
			names = {"--remove-unsats", "-ru"},
			description = "boolean indicating whether to remove entailments due to unsatisfiability (optional, default=true)",
			arity = 1)
		private boolean removeUnsats = true;
		
		@Parameter(
			names = {"--check-min-cardinality", "-min"},
			description = "boolean indicating whether to check min cardinality restrictions (optional, default=true)",
			arity = 1)
		private boolean checkMinimumCardinality = true;

		@Parameter(
			names = {"--remove-backbone", "-rb"},
			description = "boolean indicating whether to remove axioms on the backhone from entailments (optional, default=true)",
			arity = 1)
		private boolean removeBackbone = true;
		
		@Parameter(
			names = {"--backbone-iri", "-b"},
			description = "iri of backbone ontology")
		private String backboneIri = "http://opencaesar.io/oml";
		
		@Parameter(
			names = {"--indent", "-n"},
			description = "indent of the JUnit XML elements")
		private int indent = 2;
		
		@Parameter(
			names = {"--debug", "-d"},
			description = "Shows debug logging statements")
		private boolean debug;
		
		@Parameter(
			names = {"--omit-explanations", "-oe"},
			description = "Omit explanations of inconsistency and unsatisfiability")
		private boolean omitExplanations = false;
			
		@Parameter(
			names = {"--help", "-h"},
			description = "Displays summary of options",
			help = true)
		private boolean help;
	}
		
	private static class Spec {
		String outputOntologyIri;
		EnumSet<StatementType> statementTypes;
	}

	private static class Result {
		public String name;
		public String message;
		public String explanation;
	}

	private final static Logger LOGGER = Logger.getLogger(OwlReasonApp.class);
	static {
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
	}

	/**
	 * Application for running a reasoner on the set of ontologies in scope of an OASIS XML catalog.
	 * @param args Application arguments.
	 * @throws Exception Error
	 */
	public static void main(final String... args) throws Exception {
		final OwlReasonApp app = new OwlReasonApp();
		final JCommander builder = JCommander.newBuilder().addObject(app.options).build();
		builder.parse(args);
		if (app.options.help) {
			builder.usage();
			return;
		}
		if (app.options.debug) {
			final Appender appender = LogManager.getRootLogger().getAppender("stdout");
			((AppenderSkeleton) appender).setThreshold(Level.DEBUG);
		}
		app.run();
	}

	/**
	 * Creates a new OwlReasonApp object
	 */
	public OwlReasonApp() {
	}
	
	private void run() throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                     OWL Reason " + getAppVersion());
		LOGGER.info("=================================================================");
	    	    	    
	    // Create ontology manager.
	    
	    LOGGER.info("create ontology manager");
	    final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	    if (manager == null) {
	    	throw new RuntimeException("couldn't create owl ontology manager");
	    }
	    LOGGER.debug("add location mappers");
		manager.getIRIMappers().add((new XMLCatalogIRIMapper(new File(options.catalogPath), options.inputFileExtensions)));

	    // Get Pellete reasoner factory.

	    LOGGER.info("create pellet reasoner factory");
		final OpenlletReasonerFactory reasonerFactory = OpenlletReasonerFactory.getInstance();

	    // Create explanation format

	    OWLDocumentFormat explanationFormat = extensions.get(options.explanationFormat);

	    // Check the input ontology
    	check(manager, reasonerFactory, explanationFormat, options.inputOntologyIri);

    	// Create dataset iris file
    	createIrisFile(manager);
    	
    	LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}
	
	private void createIrisFile(final OWLOntologyManager manager) {
		if (options.outputOntologyIrisPath != null) {
	        LOGGER.info("Saving "+options.outputOntologyIrisPath);
	        List<String> iris = manager.ontologies().map(o -> o.getOntologyID().getOntologyIRI().get().toString()).collect(Collectors.toList());
		    iris.addAll(options.specs.stream().map(s -> s.outputOntologyIri).collect(Collectors.toList()));

	        StringBuffer s = new StringBuffer();
			iris.stream().sorted().forEach(iri -> {;
	        	s.append(iri + "\r");
		    });
	        
	        try {
				Files.write(Path.of(options.outputOntologyIrisPath), s.toString().getBytes());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private void check(final OWLOntologyManager manager, OpenlletReasonerFactory reasonerFactory, OWLDocumentFormat explanationFormat, String inputOntologyIri) throws Exception {
	    // Load input ontology.
	    
	    LOGGER.info("load ontology "+inputOntologyIri);
	    final OWLOntology inputOntology = manager.loadOntology(IRI.create(inputOntologyIri));
	    if (inputOntology == null) {
	    	throw new RuntimeException("couldn't load ontology");
	    }
	    
	    // Create Pellet reasoner.

	    LOGGER.info("create pellet reasoner for "+inputOntologyIri);
	    PelletExplanation.setup();
	    OpenlletReasoner reasoner = reasonerFactory.createReasoner(inputOntology);
	    if (reasoner == null) {
	    	throw new RuntimeException("couldn't create reasoner");
	    }
	    
	    try {
		    // Create PelletExplanation.
		    
		    LOGGER.info("create explanation for "+inputOntologyIri);
		    PelletExplanation explanation = new PelletExplanation(reasoner);
		    	    
		    // Create knowledge base.
	
		    LOGGER.info("create knowledge base");
		    KnowledgeBase kb = reasoner.getKB();
		    if (kb == null) {
		    	throw new RuntimeException("couldn't get knowledge base");
		    }
	
		    // Set the unique name assumption
	
		    OpenlletOptions.USE_UNIQUE_NAME_ASSUMPTION = options.uniqueNames;
		    
		    // Check for consistency and satisfiability
			
			Map<String, List<Result>> allResults = new LinkedHashMap<>();
			allResults.put(CONSISTENCY, checkConsistency(inputOntologyIri, reasoner, explanation, explanationFormat));
			boolean isConsistent = allResults.get(CONSISTENCY).stream().noneMatch(r -> r.explanation != null);
			boolean isSatisfiable = false;
			if (isConsistent) {
		    	allResults.put(SATISFIABILITY, checkSatisfiability(inputOntologyIri, reasoner, explanation, explanationFormat));
		    	isSatisfiable = allResults.get(SATISFIABILITY).stream().noneMatch(r -> r.explanation != null);
		    }
			if (isConsistent && options.checkMinimumCardinality) {
				var result = checkMinCardinalities(inputOntologyIri, kb, manager, explanationFormat);
				allResults.put(CONSISTENCY, Collections.singletonList(result));
				isConsistent = result.explanation == null;
			}
			writeResults(inputOntologyIri, allResults, options.indent);

			// Check Results
			
			if (!isConsistent) {
				throw new ReasoningException("Ontology is inconsistent. Check " + options.reportPath + " for more details.");
		    }
			if (!isSatisfiable) {
				throw new ReasoningException("Ontology has insatisfiabilities. Check " + options.reportPath + " for more details.");
		    }
		    			
		    // Iterate over specs and extract entailments.
	
		    for (Spec spec: options.specs) {
		      String outputOntologyIri = spec.outputOntologyIri;
		      EnumSet<StatementType> statementTypes = spec.statementTypes;
		      extractAndSaveEntailments(kb, inputOntologyIri, outputOntologyIri, statementTypes, manager);
		    }
	    } finally {
		    // dispose
		    reasoner.dispose();
	    }
	}

	private List<Result> checkConsistency(String ontologyIri, OpenlletReasoner reasoner, PelletExplanation explanation, OWLDocumentFormat explanationFormat) throws Exception {
    	LOGGER.info("test consistency on "+ontologyIri);
    	List<Result> results = new ArrayList<>();
    	Result result = new Result();
    	result.name = ontologyIri;
    	
        if (!reasoner.isConsistent()) {
        	Set<OWLAxiom> axioms = explanation.getInconsistencyExplanation();
        	result.message = reasoner.getKB().getExplanation();
        	result.explanation = options.omitExplanations ? "[omitted]" : createExplanationOntology(axioms, explanationFormat);
        }
	    results.add(result);
    
	    return results;
	}

	private List<Result> checkSatisfiability(String ontologyIri, OpenlletReasoner reasoner, PelletExplanation explanation, OWLDocumentFormat explanationFormat) throws Exception {
    	LOGGER.info("test satisfiability on "+ontologyIri);
    	List<Result> results = new ArrayList<>();
    	
		Set<OWLClass> allClasses = reasoner.getRootOntology().classesInSignature(Imports.INCLUDED).collect(Collectors.toSet());
		
		int numOfClasses = allClasses.size();   	
    	LOGGER.info(numOfClasses+" total classes");

    	int count = 0;
    	for (OWLClass klass : allClasses) {
    		if (options.removeBackbone && klass.getIRI().getIRIString().startsWith(options.backboneIri))
    			continue;
    		if (klass.isOWLNothing()) // owl:Nothing should not be checked
    			continue;
    		String className = klass.getIRI().getIRIString();
    	    LOGGER.info(className+" "+ ++count+" of "+numOfClasses);

    	    Result result = new Result();
    	    results.add(result);
    	    result.name = className;
    	    
    	    if (!reasoner.isSatisfiable(klass)) {
    	    	result.message = "class "+className+" is insatisfiable";
    	    	result.explanation = options.omitExplanations ? "[omitted]" : createExplanationOntology(explanation.getUnsatisfiableExplanation(klass), explanationFormat);
    	    }
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

	private void writeResults(String ontologyIri, Map<String, List<Result>> allResults, int indent) throws Exception {
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
	
	private void extractAndSaveEntailments(KnowledgeBase kb, String inputOntologyIri, String outputOntologyIri, EnumSet<StatementType> statementTypes, OWLOntologyManager manager) throws Exception {
		// Create extractor.
	  
		LOGGER.info("create extractor for "+statementTypes);
		ModelExtractor extractor = new ModelExtractor(kb);

		// Extract entailments

		LOGGER.info("extract entailments for "+statementTypes);
		Model entailments = extractEntailments(extractor, statementTypes);

		// Remove trivial axioms involving owl:Thing and owl:Nothing.

		LOGGER.info("remove trivial entailments for "+statementTypes);
		entailments = removeTrivial(entailments, options.removeUnsats);

		// Remove backbone entailments.

		LOGGER.info("remove backbone entailments for "+statementTypes);
		if (options.removeBackbone) {
			entailments = removeBackbone(entailments, options.backboneIri);
		}

		// Create Jena ontology model for results.
		  
		LOGGER.info("create jena ontology model for "+statementTypes);
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, entailments);
		  
		// Create Jena ontology from model.
		  
		LOGGER.info("create jena ontology "+outputOntologyIri+" from model");
		Ontology outputOntology = model.createOntology(outputOntologyIri);
		outputOntology.addImport(ResourceFactory.createResource(inputOntologyIri));
		outputOntology.addComment("Generated by Owl Reason "+ getAppVersion(), null);
		outputOntology.addVersionInfo(""+Instant.now().getEpochSecond());

		// Create an empty OWLAPI Ontology to get the ontology document IRI

		LOGGER.info("get output filename from location mapping");
		OWLOntology empty = manager.createOntology(IRI.create(outputOntologyIri+"."+options.outputFileExtension));
		String filename = URI.create(manager.getOntologyDocumentIRI(empty).toString()).getPath();
		manager.removeOntology(empty);
		  
		// Open output stream.

		LOGGER.info("open output stream "+filename);
		File outputFile = new File(filename);
		//noinspection ResultOfMethodCallIgnored
		outputFile.getParentFile().mkdirs();
		FileOutputStream outputFileStream = new FileOutputStream(outputFile);
		  
		// Serialize Jena ontology model to output stream.
		  
		LOGGER.info("serialize "+entailments.size()+" entailments to "+filename);
		Lang lang = RDFLanguages.fileExtToLang(options.outputFileExtension);

		model.write(outputFileStream, lang.getName());
		LOGGER.info("finished serializing "+filename);
	}

	private Result checkMinCardinalities(final String ontologyIri, final KnowledgeBase kb, final OWLOntologyManager manager, OWLDocumentFormat explanationFormat) throws Exception {
    	LOGGER.info("test min cardinality restrictions on "+ontologyIri);

    	var result = new Result(); 
		result.name = ontologyIri;

		// Enumerate statement types
		
		EnumSet<StatementType> statementTypes = EnumSet.of(
				StatementType.ALL_INSTANCE,
				StatementType.DATA_PROPERTY_VALUE,
				StatementType.OBJECT_PROPERTY_VALUE);
		
		// Create extractor.
	  
		ModelExtractor extractor = new ModelExtractor(kb);
		
		// Get axioms for classes that have minimum cardinality restrictions
		
		List<OWLSubClassOfAxiom> axioms = getRestrictionAxioms(manager);
		
		// Define min cardinality restriction data
		
		class Data {
			String domainIri;
			String propertyIri;
			String rangeIri;
			int min;
		}
		
		// Collect min restriction data
		
		var datas = new ArrayList<Data>();
		for (var axiom : axioms) {
			var data = new Data();
			data.domainIri = ((OWLClass)axiom.getSubClass()).getIRI().getIRIString();
			var restriction = axiom.getSuperClass();
			data.propertyIri = ((OWLProperty)((OWLRestriction)restriction).getProperty()).getIRI().getIRIString();
			if (restriction instanceof OWLObjectMinCardinality || restriction instanceof OWLObjectExactCardinality) {
				var range = ((OWLQuantifiedObjectRestriction) restriction).getFiller();
				data.rangeIri = (range instanceof OWLClass) ? ((OWLClass)range).getIRI().getIRIString() : null;
				data.min = ((OWLObjectCardinalityRestriction) restriction).getCardinality();
			} else if (restriction instanceof OWLDataMinCardinality || restriction instanceof OWLDataExactCardinality) {
				var range = ((OWLQuantifiedDataRestriction) restriction).getFiller();
				data.rangeIri = (range instanceof OWLDatatype) ? ((OWLDatatype)range).getIRI().getIRIString() : null;
				data.min = ((OWLDataCardinalityRestriction) restriction).getCardinality();
			} else if (restriction instanceof OWLObjectSomeValuesFrom) {
				var range = ((OWLQuantifiedObjectRestriction) restriction).getFiller();
				data.rangeIri = (range instanceof OWLClass) ? ((OWLClass)range).getIRI().getIRIString() : null;
				data.min = 1;
			} else if (restriction instanceof OWLDataSomeValuesFrom) {
				var range = ((OWLQuantifiedDataRestriction) restriction).getFiller();
				data.rangeIri = (range instanceof OWLDatatype) ? ((OWLDatatype)range).getIRI().getIRIString() : null;
				data.min = 1;
			}
			datas.add(data);
		}
		
		// Extract Entailments
		Model model = extractEntailments(extractor, statementTypes);
		
		// Check minimum cardinality restrictions
		
		for (var data : datas) {
			Resource domain = model.getResource(data.domainIri);
			Property predicate = model.getProperty(data.propertyIri);
			Resource range = (data.rangeIri != null) ? model.getResource(data.rangeIri) : null;
			var i = model.listResourcesWithProperty(RDF.type, domain);
			while (i.hasNext()) {
				var subject = i.next();
				var statements = new ArrayList<Statement>();
				var j = subject.listProperties(predicate);
				while (j.hasNext()) {
					var statement = j.next();
					var object = statement.getObject();
					if (range == null) {
						statements.add(statement);
					} else if (object.isLiteral()) {
						if (range instanceof RDFDatatype) {
							var datatype = (RDFDatatype) range;
							if (datatype.isValidLiteral(object.asNode().getLiteral())) {
								statements.add(statement);
							}
						} else { // the case of owl:real and owl:rational
							statements.add(statement);
						}
					} else if (object.isResource()) {
						if (object.asResource().hasProperty(RDF.type, range)) {
							statements.add(statement);
						}
					}
				}
				if (statements.size() < data.min) {
					result.message = "Individual violates minimum cardinality restriction";
					result.explanation = createMinCardinalityExplanation(domain, predicate, range, subject, data.min, statements);
					return result;
				}
			}
		}
		
		return result;
	}
	
	private String createMinCardinalityExplanation(Resource domain, Property property, Resource range, Resource subject, int min, List<Statement> statements) {
		// ideally here we need to figure out all relevant axioms for the explanation using OWLAPI and call createExplanationOntology
		// but for now...
		var explanation = String.format(
				"// Class with restriction\n\n"
				+ "<%s> <%s> [\n" +
				"\t\t<%s> <%s> .\n" +
				"\t\t<%s> <%s> .\n" +
				"\t\t<%s> %d .\n%s" +
				"] .\n",
				domain.getURI(),
				RDFS.subClassOf,
				RDF.type,
				OWL.Restriction,
				OWL.onProperty,
				property.getURI(),
				(range != null) ? OWL.NS+"minQualifiedCardinality" : OWL.minCardinality,
				min, 
				(range != null) ? String.format("\t\t<%s> <%s> .\n", OWL.NS+"onClass",  range.getURI()) : "");
		var id = subject.isAnon() ? subject.getId().getLabelString() : subject.getURI();
		explanation += String.format(
				"\n// Violating individual with conforming property values\n"
				+ "\n<%s> <%s> <%s> .\n", 
				id, RDF.type, domain.getURI());
		for(var s : statements) {
			explanation += String.format("<%s> <%s> <%s> .\n", id, s.getPredicate().getURI(), s.getObject().toString());
		}
		return explanation;
	}
	
	private List<OWLSubClassOfAxiom> getRestrictionAxioms(final OWLOntologyManager manager) throws Exception {
		var axioms = new ArrayList<OWLSubClassOfAxiom>();
		for (var ontology : manager.getOntologies()) {
			for (var clazz : ontology.getClassesInSignature()) {
				for (var axiom : ontology.getSubClassAxiomsForSubClass(clazz)) {
					var exp = axiom.getSuperClass().getClassExpressionType();
					if (exp == ClassExpressionType.OBJECT_MIN_CARDINALITY ||
						exp == ClassExpressionType.OBJECT_EXACT_CARDINALITY ||
						exp == ClassExpressionType.OBJECT_SOME_VALUES_FROM ||
						exp == ClassExpressionType.DATA_MIN_CARDINALITY ||
						exp == ClassExpressionType.DATA_EXACT_CARDINALITY ||
						exp == ClassExpressionType.DATA_SOME_VALUES_FROM) {
						axioms.add(axiom);
					}
				}
			}
		}
		return axioms;
	}
	
	private Model extractEntailments(ModelExtractor extractor, EnumSet<StatementType> types) {
	    // Extract entailments.
	    extractor.setSelector(types);
	    Model result = extractor.extractModel();
	    LOGGER.info("extracted "+result.size()+" entailed axioms");
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
	    LOGGER.info("removed "+trivial.size()+" trivial axioms");
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
	    LOGGER.info("removed "+backbone.size()+" backbone axioms");
	    return entailments;
	}
	
	/**
	 * Get application version id from properties file.
	 * 
	 * @return version string from build.properties or UNKNOWN
	 */
	private String getAppVersion() {
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
	 * A string converter for specifying types of reasoning statements.
	 */
	public static class SpecConverter implements IStringConverter<Spec> {
		/**
		 * Creates a new SpecConverter object
		 */
		public SpecConverter() {
		}
		@Override
		public Spec convert(String value) {
			String[] s = value.split("=");
			Spec spec = new Spec();
			spec.outputOntologyIri = s[0].trim();
			spec.statementTypes = EnumSet.noneOf(StatementType.class);
			for (String type : s[1].trim().split("\\|")) {
				type = type.trim();
				StatementType st = StatementType.valueOf(type);
		        spec.statementTypes.add(st);
			}
			return spec;
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
			if (!extensions.containsKey(value)) {
				throw new ParameterException("Parameter " + name + " should be a valid extension, got: " + value +
						" recognized extensions are: " +
						extensions.keySet().stream().reduce( (x,y) -> x + " " + y) );
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
	 * A parameter validator for an output explanation file.
	 */
	public static class ExplanationFormatValidator implements IParameterValidator {
		/**
		 * Creates a new ExplanationFormatValidator object
		 */
		public ExplanationFormatValidator() {
		}
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			if (!extensions.containsKey(value)) {
				throw new ParameterException("Parameter " + name + " should be a valid RDF format, got: " + value +
						" recognized RDF formats are: "+extensions);
			}
		}

	}

	/**
	 * A parameter validator for an output XML report file.
	 */
	public static class ReportPathValidator implements IParameterValidator {
		/**
		 * Creates a new ReportPathValidator object
		 */
		public ReportPathValidator() {
		}
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File file = new File(value);
			if (!file.getName().endsWith(".xml")) {
				throw new ParameterException("Parameter " + name + " should be a valid XML path");
			}
			File parentFile = file.getParentFile();
			if (parentFile != null && !parentFile.exists()) {
				parentFile.mkdirs();
			}
	  	}
	}

	/**
	 * A parameter validator for an output IRI log file.
	 */
	public static class IrisPathValidator implements IParameterValidator {
		/**
		 * Creates a new IrisPathValidator object
		 */
		public IrisPathValidator() {
		}
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File file = new File(value);
			if (!file.getName().endsWith(".txt")) {
				throw new ParameterException("Parameter " + name + " should be a valid .txt file path");
			}
			File parentFile = file.getParentFile();
			if (parentFile != null && !parentFile.exists()) {
				parentFile.mkdirs();
			}
	  	}
	}

	private static class ReasoningException extends Exception {
		private static final long serialVersionUID = 1L;

		public ReasoningException(String s) {
			super(s);
		}
	}
}