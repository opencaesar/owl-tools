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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
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
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.SimpleRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.io.CharStreams;

import openllet.core.KnowledgeBase;
import openllet.jena.ModelExtractor;
import openllet.jena.ModelExtractor.StatementType;
import openllet.jena.vocabulary.OWL2;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;

public class OwlReasonApp {
  
	private static final String CONSISTENCY = "consistency";
	private static final String SATISFIABILITY = "satisfiability";

	private Options options = new Options();
	
	private class Options {
		@Parameter(
			names = { "--catalog-path", "-c"},
			description = "path to the OWL catalog file (Required)",
			validateWith = CatalogPath.class,
			required = true,
			order = 1)
		String catalogPath;
		
		@Parameter(
			names = { "--input-iri", "-i"},
			description = "iri of input ontology (Required)",
			required = true,
			order = 2)
		List<String> inputOntologyIris;

		@Parameter(
			names = {"--spec", "-s"},
			description = "output-iri-postfix=Types where Types is a list of comma-separated entailment statementTypes",
			converter = SpecConverter.class,
			required = false,
			order = 3)
		List<Spec> specs = new ArrayList<Spec>();
		
		@Parameter(
			names = {"--format", "-f"},
			description = "output ontology format",
			converter = LanguageConverter.class,
			required = false,
			order = 4)
		Lang language = RDFLanguages.TURTLE;
		
		@Parameter(
			names = {"--remove-unsats", "-ru"},
			description = "remove entailments due to unsatisfiability",
			required = false,
			order = 5)
		boolean removeUnsats = true;
		
		@Parameter(
			names = {"--remove-backbone", "-rb"},
			description = "remove axioms on the backhone from entailments",
			required = false,
			order = 6)
		boolean removeBackbone = true;

		@Parameter(
			names = {"--backbone-iri", "-b"},
			description = "iri of backbone ontology",
			required = false,
			order = 7)
		String backboneIri = "http://opencaesar.io/oml";
				
		@Parameter(
			names = {"--indent", "-n"},
			description = "indent of the JUnit XML elements",
			required = false,
			order = 8)
		int indent = 2;

		@Parameter(
			names = {"--debug", "-d"},
			description = "Shows debug logging statements",
			order = 9)
		private boolean debug;

		@Parameter(
			names = {"--help", "-h"},
			description = "Displays summary of options",
			help = true,
			order =10)
		private boolean help;
	}
		
	private static class Spec {
		String iri;
		EnumSet<StatementType> statementTypes;
	}

	private static class Result {
		public boolean success;
		public String explanation;
	}

	private final static Logger LOGGER = Logger.getLogger(OwlReasonApp.class);
	{
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
	}

	public static void main(final String... args) {
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
	    try {
			app.run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run(final String... args) throws Exception {
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
		manager.getIRIMappers().add((new XMLCatalogIRIMapper(options.catalogPath)));

	    // Get Pellete reasoner factory.

	    LOGGER.info("create pellet reasoner factory");
		final OpenlletReasonerFactory reasonerFactory = OpenlletReasonerFactory.getInstance();

	    // Create FunctionalSyntaxDocumentFormat.

	    FunctionalSyntaxDocumentFormat functionalSyntaxFormat = new FunctionalSyntaxDocumentFormat();

	    // Check the input ontologies
	    for (String inputOntologyIri : options.inputOntologyIris) {
	    	check(manager, reasonerFactory, functionalSyntaxFormat, inputOntologyIri);
	    }
	    
	    LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}
	
	public void check(final OWLOntologyManager manager, OpenlletReasonerFactory reasonerFactory, FunctionalSyntaxDocumentFormat functionalSyntaxFormat, String inputOntologyIri) throws Exception {
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
	    
	    // Create PelletExplanation.
	    
	    LOGGER.info("create explanation for "+inputOntologyIri);
	    PelletExplanation explanation = new PelletExplanation(reasoner);
	    	    
		// Create renderer for unsatisfiable class names.
	    
	    LOGGER.info("create renderer for "+inputOntologyIri);
	    final SimpleRenderer renderer = new SimpleRenderer();
		renderer.setPrefixesFromOntologyFormat(inputOntology, false);
	  
	    // Check for consistency and satisfiability, save results.
		
		Map<String, Map<String, Result>> allResults = new HashMap<String, Map<String, Result>>();
		allResults.put(CONSISTENCY, checkConsistency(inputOntologyIri, reasoner, explanation, functionalSyntaxFormat));
		allResults.put(SATISFIABILITY, checkSatisfiability(inputOntologyIri, reasoner, explanation, renderer, functionalSyntaxFormat));
		
	    writeResults(allResults, System.out, options.indent);
	    if (allResults.get(CONSISTENCY).get(inputOntologyIri) == null) {
	      LOGGER.warn("stopping the analysis of "+inputOntologyIri+" due to inconsistency");
	      System.exit(1);
	    }

	    // Create knowledge base.

	    LOGGER.info("create knowledge base and extractor");
	    KnowledgeBase kb = reasoner.getKB();
	    if (kb == null) {
	    	throw new RuntimeException("couldn't get knowledge base");
	    }
	    
	    // Iterate over specs and extract entailments.

	    for (Spec spec: options.specs) {
	      String outputOntologyIri = spec.iri;
	      EnumSet<StatementType> statementTypes = spec.statementTypes;
	      extractAndSaveEntailments(kb, inputOntologyIri, outputOntologyIri, statementTypes, manager);
	    }
	}

	private Map<String, Result> checkConsistency(String ontologyIri, OpenlletReasoner reasoner, PelletExplanation explanation, FunctionalSyntaxDocumentFormat functionalSyntaxFormat) throws Exception {
    	LOGGER.info("test consistency on "+ontologyIri);
    	Map<String, Result> results = new HashMap<String, Result>();

    	Result result = new Result();
	    results.put(ontologyIri, result);
	    	      
		result.success = reasoner.isConsistent();
    	LOGGER.info((result.success ? "" : "in") + "consistent");
    	
        if (!result.success) {
			OWLClass owl_Thing = OWLManager.getOWLDataFactory().getOWLThing();
			if (owl_Thing == null) {
				throw new RuntimeException("couldn't create OWL:Thing");
			}
    		result.explanation = explainClass(owl_Thing, explanation, functionalSyntaxFormat);
        }
    
	    return results;
	}

	private Map<String, Result> checkSatisfiability(String ontologyIri, OpenlletReasoner reasoner, PelletExplanation explanation, SimpleRenderer renderer, FunctionalSyntaxDocumentFormat functionalSyntaxFormat) throws Exception {
    	LOGGER.info("test satisfiability on "+ontologyIri);
    	Map<String, Result> results = new HashMap<String, Result>();
    	
		Set<OWLClass> allClasses = reasoner.getRootOntology().classesInSignature(Imports.INCLUDED).collect(Collectors.toSet());
		if (allClasses == null) {
			throw new RuntimeException("can't get all classes");
		}
		
		int numOfClasses = allClasses.size();   	
    	LOGGER.info(numOfClasses+" total classes");

    	int count = 0;
    	int numOfUnsat = 0;
    	for (OWLClass klass : allClasses) {
    	    String className = renderer.render(klass);
    	    LOGGER.info(className+" "+ ++count+" of "+numOfClasses);

    	    String caseName = ontologyIri+" "+className;
    	    Result result = new Result();
    	    results.put(caseName, result);

    	    result.success = reasoner.isSatisfiable(klass);
    	    LOGGER.info(caseName+" "+(result.success?"":"un")+"satisfiable");

    	    if (!result.success) {
    	    	numOfUnsat += 1;
    	    	result.explanation = explainClass(klass, explanation, functionalSyntaxFormat);
    	    }
    	}
    	LOGGER.info(numOfUnsat+" unsatisfiable classes");

    	return results;
	}

	private String explainClass(OWLClass klass, PelletExplanation explanation, FunctionalSyntaxDocumentFormat format) throws Exception {
	    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	    if (manager == null ) {
	    	throw new RuntimeException("couldn't create owl ontology manager");
	    }
	    Set<OWLAxiom> axioms = explanation.getUnsatisfiableExplanation(klass);
	    if (axioms == null) {
	    	throw new RuntimeException("couldn't get explanation for "+klass);
	    }
	    OWLOntology ontology = manager.createOntology(axioms);
	    if (ontology == null) {
	    	throw new RuntimeException("couldn't create ontology for explanation");
	    }
		StringDocumentTarget target = new StringDocumentTarget();
	    manager.saveOntology(ontology, format, target);
	    return target.toString();
	}

	private void writeResults(Map<String, Map<String, Result>> allResults, PrintStream io, int indent) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
	    Document doc = db.newDocument();
		Element tss = doc.createElement("testsuites");
		doc.appendChild(tss);
		
		allResults.forEach((test, results) -> {
	    	Element ts = doc.createElement("testsuite");
	      	ts.setAttribute("name", test);
			tss.appendChild(ts);
			results.forEach((id, result) -> {
		    	Element tc = doc.createElement("testcase");
		        tc.setAttribute("name", id);
		    	ts.appendChild(tc);
		        if (!result.success) {
		        	Element fl = doc.createElement("failure");
		        	tc.appendChild(fl);
	      	        String exp = result.explanation;
	      	        Text tn = doc.createTextNode(exp);
		    		fl.appendChild(tn);
	  	        }
	    	});
	    });
	    	    
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", ""+indent);
        DOMSource source = new DOMSource(doc);
        StreamResult console = new StreamResult(io);
        transformer.transform(source, console);
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
		OWLOntology empty = manager.createOntology(IRI.create(outputOntologyIri+"."+options.language.getFileExtensions().get(0)));
		String filename = URI.create(manager.getOntologyDocumentIRI(empty).toString()).getPath();
		manager.removeOntology(empty);
		  
		// Open output stream.

		LOGGER.info("open output stream "+filename);
		File outputFile = new File(filename);
		outputFile.getParentFile().mkdirs();
		FileOutputStream outputFileStream = new FileOutputStream(outputFile);
		  
		// Serialize Jena ontology model to output stream.
		  
		LOGGER.info("serialize "+entailments.size()+" entailments to "+filename);
		model.write(outputFileStream, options.language.getName());
		LOGGER.info("finished serializing "+filename);
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
		List<Statement> trivial = new ArrayList<Statement>();
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
		List<Statement> backbone = new ArrayList<Statement>();
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
	public String getAppVersion() {
		String version = "UNKNOWN";
		try {
			InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("version.txt");
			InputStreamReader reader = new InputStreamReader(input);
			version = CharStreams.toString(reader);
		} catch (IOException e) {
			String errorMsg = "Could not read version.txt file." + e;
			LOGGER.error(errorMsg, e);
		}
		return version;
	}

	public static class CatalogPath implements IParameterValidator {
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File file = new File(value);
			if (!file.getName().endsWith("catalog.xml")) {
				throw new ParameterException("Parameter " + name + " should be a valid OWL catalog path");
			}
		}
	}
	
	public static class SpecConverter implements IStringConverter<Spec> {
		@Override
		public Spec convert(String value) {
			String[] s = value.split("=");
			Spec spec = new Spec();
			spec.iri = s[0];
			spec.statementTypes = EnumSet.noneOf(StatementType.class);
			for (String type : s[1].split(" ")) {
				StatementType st = StatementType.valueOf(type);
		        if (st != null) {
		        	spec.statementTypes.add(st);
		        } else {
		  	    	throw new ParameterException("invalid entailment type "+type);
		        }
			}
			return spec;
		}
		
	}

	public static class LanguageConverter implements IStringConverter<Lang> {
		@Override
		public Lang convert(String value) {
			Lang lang = RDFLanguages.filenameToLang(value);
			return (lang != null) ? lang : RDFLanguages.TURTLE;
		}
		
	}
}