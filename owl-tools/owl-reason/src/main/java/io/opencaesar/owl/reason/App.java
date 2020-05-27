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
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
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

public class App {
  
	private static final String CONSISTENCY = "consistency";
	private static final String SATISFIABILITY = "satisfiability";

	private static final Map<String,StatementType> STATEMENT_TYPES = new HashMap<String, StatementType>();
	static {
		STATEMENT_TYPES.put("AllInstance", StatementType.ALL_INSTANCE);
		STATEMENT_TYPES.put("AllSubclass", StatementType.ALL_SUBCLASS);
		STATEMENT_TYPES.put("AllSubproperty", StatementType.ALL_SUBPROPERTY);
		STATEMENT_TYPES.put("ComplementClass", StatementType.COMPLEMENT_CLASS);
		STATEMENT_TYPES.put("DataPropertyValue", StatementType.DATA_PROPERTY_VALUE);
		STATEMENT_TYPES.put("DifferentFrom", StatementType.DIFFERENT_FROM);
		STATEMENT_TYPES.put("DirectInstance", StatementType.DIRECT_INSTANCE);
		STATEMENT_TYPES.put("DirectSubclass", StatementType.DIRECT_SUBCLASS);
		STATEMENT_TYPES.put("DirectSubproperty", StatementType.DIRECT_SUBPROPERTY);
		STATEMENT_TYPES.put("DisjointClass", StatementType.DISJOINT_CLASS);
		STATEMENT_TYPES.put("DisjointProperty", StatementType.DISJOINT_PROPERTY);
		STATEMENT_TYPES.put("EquivalentClass", StatementType.EQUIVALENT_CLASS);
		STATEMENT_TYPES.put("EquivalentProperty", StatementType.EQUIVALENT_PROPERTY);
		STATEMENT_TYPES.put("InverseProperty", StatementType.INVERSE_PROPERTY);
		STATEMENT_TYPES.put("JenaDirectInstance", StatementType.JENA_DIRECT_INSTANCE);
		STATEMENT_TYPES.put("JenaDirectSubclass", StatementType.JENA_DIRECT_SUBCLASS);
		STATEMENT_TYPES.put("JenaDirectSubproperty", StatementType.JENA_DIRECT_SUBPROPERTY);
		STATEMENT_TYPES.put("ObjectPropertyValue", StatementType.OBJECT_PROPERTY_VALUE);
		STATEMENT_TYPES.put("SameAs", StatementType.SAME_AS);
	}

	private Options options = new Options();
	
	private class Options {
		@Parameter(
			names = { "-catalog" },
			description = "path to the OWL catalog file (Required)",
			validateWith = CatalogPath.class,
			required = true,
			order = 1)
		String catalogPath;
		
		@Parameter(
			names = { "-input-iri" },
			description = "iri of input ontology (Required)",
			required = true,
			order = 2)
		String input_iri;

		@Parameter(
			names = { "-spec" },
			description = "output-iri=Types where Types is a list of comma-separated entailment types",
			converter = SpecConverter.class,
			required = false,
			order = 3)
		List<Spec> specs = new ArrayList<Spec>();
		
		@Parameter(
			names = { "-format" },
			description = "output format",
			required = false,
			order = 4)
		String format = "TURTLE";
		
		@Parameter(
			names = { "-remove-unsats" },
			description = "remove entailments due to unsatisfiability",
			required = false,
			order = 5)
		boolean remove_unsats = false;
		
		@Parameter(
			names = { "-success" },
			description = "return success even if ontology is inconsistent",
			required = false,
			order = 6)
		boolean success = false;
		
		@Parameter(
			names = { "-backbone-iri" },
			description = "iri of backbone ontology",
			required = false,
			order = 7)
		String backbone_iri = "http://opencaesar.io/oml";
		
		@Parameter(
			names = { "-remove-backbone" },
			description = "whether to remove the backbone axioms",
			required = false,
			order = 8)
		boolean remove_backbone = false;
		
		@Parameter(
			names = { "-indent" },
			description = "indent JUnit XML elements",
			required = false,
			order = 9)
		int indent = 2;
		
		@Parameter(
			names = { "-id" },
			description = "identifier",
			required = false,
			order = 10)
		String id = null;
	}
		
	private static class Spec {
		String iri;
		String type_names;
		EnumSet<StatementType> types;
	}

	private static class Result {
		Map<String, Map<String, Boolean>> success;
		Map<String, Map<String, String>> explanation;
	}

	private final Logger LOGGER = LogManager.getLogger("Owl Reason"); {
		LOGGER.setLevel(Level.DEBUG);
		PatternLayout layout = new PatternLayout("%r [%t] %-5p %c %x - %m%n");
		LOGGER.addAppender(new ConsoleAppender(layout));
	}

	public static void main(final String... args) {
		final App app = new App();
		final JCommander builder = JCommander.newBuilder().addObject(app.options).build();
		builder.parse(args);
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

	    // Add Pellet entailment types to entailment specs.
	    
	    for (Spec spec : options.specs) {
	      EnumSet<StatementType> es = EnumSet.noneOf(StatementType.class);
	      for (String type : spec.type_names.split(" ")) {
	    	  StatementType st = STATEMENT_TYPES.get(type);
		        if (st != null) {
		        	es.add(st);
		        } else {
		  	    	LOGGER.fatal("invalid entailment type "+type);
		  	        LOGGER.fatal("legal types: "+String.join(" ", STATEMENT_TYPES.keySet()));
		        }
	      }
	      spec.types = es;
	    }
	    	    
	    final String[] tests = { CONSISTENCY, SATISFIABILITY };
	    
	    // Create ontology manager.
	    
	    LOGGER.info("create ontology manager");
	    final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	    if (manager == null) {
	    	throw new RuntimeException("couldn't create owl ontology manager");
	    }
	    LOGGER.debug("add location mappers");
		manager.getIRIMappers().add((new XMLCatalogIRIMapper(options.catalogPath)));

	    // Load input ontology.
	    
	    LOGGER.info("load ontology from "+options.input_iri);
	    final OWLOntology input_ontology = manager.loadOntology(IRI.create(options.input_iri));
	    if (input_ontology == null) {
	    	throw new RuntimeException("couldn't load ontology");
	    }
	    
	    // Get Pellete reasoner factory.

	    LOGGER.info("create pellet reasoner factory");
		final OpenlletReasonerFactory reasoner_factory = OpenlletReasonerFactory.getInstance();

	    // Create Pellet reasoner.

	    LOGGER.info("create pellet reasoner");
	    PelletExplanation.setup();
	    OpenlletReasoner reasoner = reasoner_factory.createReasoner(input_ontology);
	    if (reasoner == null) {
	    	throw new RuntimeException("couldn't create reasoner");
	    }
	    
	    // Create PelletExplanation.

	    PelletExplanation pl_explanation = new PelletExplanation(reasoner);
	    	    
	    // Create renderer for unsatisfiable class names.
	    
	    LOGGER.info("create renderer");
	    final SimpleRenderer renderer = new SimpleRenderer();
		renderer.setPrefixesFromOntologyFormat(input_ontology, false);
	  
	    // Check for consistency and satisfiability, save results.

	    Result result = check_consistency_and_satisfiability(reasoner, pl_explanation, renderer, tests);
	    write_result(result, System.out, options.indent);
	    if (result.success.get(CONSISTENCY).get(options.id) == null) {
	      int status = options.success ? 0 : 1;
	      LOGGER.warn("stopping with status "+status+" due to inconsistency");
	      System.exit(status);
	    }

	    // Create knowledge base.

	    LOGGER.info("create knowledge base and extractor");
	    KnowledgeBase kb = reasoner.getKB();
	    if (kb == null) {
	    	throw new RuntimeException("couldn't get knowledge base");
	    }
	    
	    // Iterate over specs and extract entailments.

	    for (Spec spec: options.specs) {
	      String output_iri = spec.iri;
	      String type_names = spec.type_names;
	      EnumSet<StatementType> types = spec.types;

	      extract_and_save_entailments(kb, output_iri, type_names, types, manager);
	    }

	    LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}
	
	private Result check_consistency_and_satisfiability(OpenlletReasoner reasoner, PelletExplanation pl_explanation, SimpleRenderer renderer, String[] tests) throws Exception {
		FunctionalSyntaxDocumentFormat functional_syntax_format = new FunctionalSyntaxDocumentFormat();
	    	    
	    Map<String, Map<String, Boolean>> success = new HashMap<String, Map<String, Boolean>>();
	    Map<String, Map<String, String>> explanation = new HashMap<String, Map<String, String>>();
	    Result result = new Result();
	    result.success = success;
	    result.explanation = explanation;
	    	    
	    for (String test : tests) {
	    	      
	    	LOGGER.info("test "+test);

	    	if (test.equals(CONSISTENCY)) {
	    		Map<String, Boolean> smap = new HashMap<String, Boolean>();
	    		success.put(CONSISTENCY, smap);
	    		Map<String, String> emap = new HashMap<String, String>();
	    		explanation.put(CONSISTENCY, emap);
	    		Boolean consistent = reasoner.isConsistent();
    	        smap.put(options.id, consistent);
    	    	LOGGER.info((consistent ? "" : "in") + "consistent");
    	        if (!consistent) {
					OWLClass owl_thing = OWLManager.getOWLDataFactory().getOWLThing();
					if (owl_thing == null) {
						throw new RuntimeException("couldn't create OWL:Thing");
					}
		    		emap.put(options.id, explain_class(owl_thing, pl_explanation, functional_syntax_format));
					break;
    	        }
    		} else if (test.equals(SATISFIABILITY)) {
	    		Map<String, Boolean> smap = new HashMap<String, Boolean>();
	    		success.put(SATISFIABILITY, smap);
	    		Map<String, String> emap = new HashMap<String, String>();
	    		explanation.put(SATISFIABILITY, emap);
	    		Set<OWLClass> all_classes = reasoner.getRootOntology().classesInSignature(Imports.INCLUDED).collect(Collectors.toSet());
	    		if (all_classes == null) {
	    			throw new RuntimeException("can't get all classes");
	    		}
    	    	LOGGER.info(all_classes.size()+" total classes");
	    		int n_class = all_classes.size();
    	    	int count = 0;
    	    	int  n_unsat = 0;
    	    	for (OWLClass klass : all_classes) {
    	    		String klass_name = renderer.render(klass);
    	    	    String case_name = options.id +" "+klass_name;
    	    	    LOGGER.debug(klass_name+" "+ ++count+" of "+n_class);
    	    	    long t_start = Instant.now().getEpochSecond();
    	    	    Boolean satisfiable = reasoner.isSatisfiable(klass);
    	    	    long t_end = Instant.now().getEpochSecond();
    	    	    LOGGER.debug(klass_name+" "+(satisfiable ? "" : "un")+"satisfiable "+(t_end - t_start)+" s");
        	        smap.put(case_name, satisfiable);
    	    	    if (!satisfiable) {
    	    	    	n_unsat += 1;
    		    		emap.put(case_name, explain_class(klass, pl_explanation, functional_syntax_format));
    	    	    }
    	    	}      
    	    	LOGGER.info(n_unsat+" unsatisfiable classes");
	    	}
	    }
	    return result;
	}

	private String explain_class(OWLClass klass, PelletExplanation explanation, FunctionalSyntaxDocumentFormat format) throws Exception {
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

	private void write_result(Result result, PrintStream io, int indent) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
	    Document doc = db.newDocument();
		Element tss = doc.createElement("testsuites");
		doc.appendChild(tss);
	    result.success.forEach((test, h1) -> {
	    	Element ts = doc.createElement("testsuite");
			tss.appendChild(ts);
  	      	ts.setAttribute("name", test);
    	    h1.forEach((id, success) -> {
    	    	Element tc = doc.createElement("testcase");
    	    	ts.appendChild(tc);
    	        tc.setAttribute("name", id);
    	        if (!success) {
    	        	Element fl = doc.createElement("failure");
    	        	tc.appendChild(fl);
	      	        String exp = result.explanation.get(test).get(id);
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
	
	private void extract_and_save_entailments(KnowledgeBase kb, String output_iri, String type_names, EnumSet<StatementType> types, OWLOntologyManager manager) throws Exception {
		// Create extractor.
	  
		LOGGER.info("create extractor for "+type_names);
		ModelExtractor extractor = new ModelExtractor(kb);

		// Extract entailments

		LOGGER.info("extract entailments for "+type_names);
		Model entailments = extract_entailments(extractor, types);

		// Remove trivial axioms involving owl:Thing and owl:Nothing.

		LOGGER.info("remove trivial entailments for "+type_names);
		entailments = remove_trivial(entailments, options.remove_unsats);

		// Remove backbone entailments.

		LOGGER.info("remove backbone entailments for "+type_names);
		if (options.remove_backbone) {
			entailments = remove_backbone(entailments, options.backbone_iri);
		}

		// Create Jena ontology model for results.
		  
		LOGGER.info("create jena ontology model for "+type_names);
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, entailments);
		  
		// Create Jena ontology from model.
		  
		LOGGER.info("create jena ontology "+output_iri+" from model");
		Ontology output_ontology = model.createOntology(output_iri);
		output_ontology.addImport(ResourceFactory.createResource(options.input_iri));
		output_ontology.addComment("Generated by Owl Reason "+ getAppVersion(), null);
		output_ontology.addVersionInfo(""+Instant.now().getEpochSecond());

		// Create an empty OWLAPI Ontology to get the ontology document IRI

		LOGGER.info("get output filename from location mapping");
		OWLOntology empty = manager.createOntology(IRI.create(output_iri));
		String filename = URI.create(manager.getOntologyDocumentIRI(empty).toString()).getPath();
		manager.removeOntology(empty);
		  
		// Open output stream.

		LOGGER.info("open output stream "+filename);
		FileOutputStream output = new FileOutputStream(new File(filename));
		  
		// Serialize Jena ontology model to output stream.
		  
		LOGGER.info("serialize "+entailments.size()+" entailments to "+filename);
		model.write(output, options.format);
		LOGGER.info("finished serializing "+filename);
	}
	
	private Model extract_entailments(ModelExtractor extractor, EnumSet<StatementType> types) {
	    // Extract entailments.
	    extractor.setSelector(types);
	    Model result = extractor.extractModel();
	    LOGGER.info("extracted "+result.size()+" entailed axioms");
	    return result;
	}
	
	/*
	 *  Remove trivial entailments involving owl:Thing, owl:Nothing, owl:topObjectProperty, owl:topDataProperty
	 */
	private Model remove_trivial(Model entailments, boolean remove_unsats) {
		StmtIterator iterator = entailments.listStatements();
		List<Statement> trivial = new ArrayList<Statement>();
	    while (iterator.hasNext()) {
	    	Statement statement = iterator.next();
	    	Resource subject = statement.getSubject();
	    	Property predicate = statement.getPredicate();
	    	RDFNode object = statement.getObject();
	    	if ((predicate.equals(RDFS.subClassOf) && (subject.equals(OWL2.Nothing) || (remove_unsats && object.equals(OWL2.Nothing)) || object.equals(OWL2.Thing))) ||
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
	private Model remove_backbone(Model entailments, String pattern) {
		StmtIterator iterator = entailments.listStatements();
		List<Statement> backbone = new ArrayList<Statement>();
	    while (iterator.hasNext()) {
	    	Statement statement = iterator.next();
	    	Property predicate = statement.getPredicate();
	    	RDFNode object = statement.getObject();
	    	if (object instanceof Resource) {
	    		String object_iri = ((Resource)object).getURI();
		        if ((predicate.equals(RDFS.subClassOf) || predicate.equals(RDFS.subPropertyOf)) && object_iri.startsWith(pattern)) {
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
			Spec spec = new Spec();
			String[] specComponents = value.split("=");
			spec.iri = specComponents[0];
			spec.type_names = specComponents[1];
			return spec;
		}
		
	}
}