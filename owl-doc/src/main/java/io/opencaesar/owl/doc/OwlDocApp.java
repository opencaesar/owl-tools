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
package io.opencaesar.owl.doc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.jena.graph.Graph;
import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import net.sourceforge.plantuml.code.TranscoderUtil;

/**
 * A utility for running a reasoner on the set of ontologies in scope of an OASIS XML catalog.
 */
public class OwlDocApp {
  
	private static final Set<String> extensions = new HashSet<String>(Arrays.asList(
		"fss", "owl", "rdf", "xml", "n3", "ttl", "rj", "nt", "jsonld", "trig", "trix", "nq"
	));
	
	private final Options options = new Options();

	/**
	 * default input ontology file extension.
	 */
	public static final String[] DEFAULT_EXTENSIONS = {"owl", "ttl"};

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
			order=8)
		private boolean outputCaseSensitive;
			
		@Parameter(
			names = {"--debug", "-d"},
			description = "Shows debug logging statements",
			order = 9)
		private boolean debug;
		
		@Parameter(
			names = {"--help", "-h"},
			description = "Displays summary of options",
			help = true,
			order = 10)
		private boolean help;
	}
	
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
			ontologies = sortByIri(ontModel.listOntologies().toList());
			classes = sortByName(ontModel.listNamedClasses().toList());
			datatypes = sortByName(ontModel.listSubjectsWithProperty(RDF.type, RDFS.Datatype).toList());
			annotationProperties = sortByName(ontModel.listAnnotationProperties().toList());
			objectProperties = sortByName(ontModel.listObjectProperties().toList());
			datatypeProperties = sortByName(ontModel.listDatatypeProperties().toList());
			individuals = sortByName(ontModel.listIndividuals().toList());
		}
	}
	
	private final static Logger LOGGER = Logger.getLogger(OwlDocApp.class);

	/**
	 * Application for running a reasoner on the set of ontologies in scope of an OASIS XML catalog.
	 * @param args Application arguments.
	 * @throws Exception Error
	 */
	public static void main(final String... args) throws Exception {
		final OwlDocApp app = new OwlDocApp();
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
	 * Creates a new OwlDocApp object
	 */
	public OwlDocApp() {
	}
	
	private void run() throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                     OWL Doc " + getAppVersion());
		LOGGER.info("=================================================================");
        LOGGER.info(("Input Catalog Path = " + options.inputCatalogPath));
        LOGGER.info(("Input Catalog Title = " + options.inputCatalogTitle));
        LOGGER.info(("Input Catalog Version = " + options.inputCatalogVersion));
        LOGGER.info(("Input Ontology Iris = " + options.inputOntologyIris));
        LOGGER.info(("Input File Extensions = " + options.inputFileExtensions));
        LOGGER.info(("Output Folder Path = " + options.outputFolderPath));
        LOGGER.info(("Output Case Sensitive = " + options.outputCaseSensitive));
	    	    	    
		OwlCatalog catalog = OwlCatalog.create(new File(options.inputCatalogPath).toURI());
		Map<String, URI> fileMap = catalog.getFileUriMap(options.inputFileExtensions);
		OntDocumentManager mgr = new OntDocumentManager();
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
			mgr.getFileManager().readModelInternal(ontModel, iri);
		}
		
		OwlModel owlModel = new OwlModel(ontModel);
		final var outputFiles = new HashMap<File, String>();
		
		// create output files
		outputFiles.putAll(generateSummaryFile(owlModel));
		outputFiles.putAll(generateIndexFile(owlModel));
		outputFiles.putAll(generateNavigationFile(owlModel));
		outputFiles.putAll(generateOntologiesFile(owlModel));
		outputFiles.putAll(generateClassesFile(owlModel));
		outputFiles.putAll(generateDatatypesFile(owlModel));
		outputFiles.putAll(generatePropertiesFile(owlModel.annotationProperties, "Annotation Properties"));
		outputFiles.putAll(generatePropertiesFile(owlModel.datatypeProperties, "Datatype Properties"));
		outputFiles.putAll(generatePropertiesFile(owlModel.objectProperties, "Object Properties"));
		outputFiles.putAll(generateIndividualsFile(owlModel));
		
		// save output files				
		outputFiles.forEach((file, result) -> {
			BufferedWriter out = null;
			try {
				file.getParentFile().mkdirs();
				final var filePath = file.getCanonicalPath();
				out = new BufferedWriter(new FileWriter(filePath));

				LOGGER.info("Saving: "+filePath);
			    out.write(result.toString());
			    
			    if (file.getName().endsWith(".sh")) {
					file.setExecutable(true);
			    }
			}
			catch (IOException e) {
			    System.out.println(e);
			}
			finally {
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {
						// no handling
					}
				}
			}
			
		});

		ontModel.close();
		
    	LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}
	
	//----------------------------------------------------------------------------------
	
	private Map<File, String> generateSummaryFile(OwlModel owlModel) throws IOException {
		final var content = new StringBuffer();

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
	    LocalDateTime now = LocalDateTime.now();  
	    var timestamp = dtf.format(now);  

		content.append(String.format(
			"""
			<html>
			   <head>
			      <title>%s</title>
			   </head>
			   <body>
			      <font face="Verdana, Helvetica, sans-serif">
			         <h2>
			            %s v%s
			         </h2>
			         <p><i>Updated on: %s</i></p>
			         <hr>
			      </font>
			   </body>
			</html>
			""",
			options.inputCatalogTitle,
			options.inputCatalogTitle,
			options.inputCatalogVersion,
			timestamp));
				
		final var file = new File(options.outputFolderPath+File.separator+"summary.html").getCanonicalFile();
		return Collections.singletonMap(file, content.toString());
	}
	
	private Map<File, String> generateIndexFile(OwlModel owlModel) throws IOException {
		final var content = new StringBuffer();
		
		content.append(
			"""
			<html>
			   <head>
			      <title>Ontology Documentation</title>
			      <script type="text/javascript">
			         targetPage = "" + window.location.search;
			         if (targetPage != "" && targetPage != "undefined")
			             targetPage = targetPage.substring(1);
			         function loadFrames() {
			             if (targetPage != "" && targetPage != "undefined")
			                 top.mainFrame.location = top.targetPage;
			         }
			      </script>
			   </head>
			   <frameset cols="30%,70%" title="" onLoad="top.loadFrames()">
			      <frameset rows="40%,60%" title="" onLoad="top.loadFrames()">
			         <frame src="navigation.html" title="Navigation">
			         <frame src="ontologies.html" name="navigationFrame" title="Elements">
			      </frameset>
			      <frame src="summary.html" name="mainFrame" title="Details" scrolling="yes"/>
			   </frameset>
			</html>
			""");
		
		final var file = new File(options.outputFolderPath+File.separator+"index.html").getCanonicalFile();
		return Collections.singletonMap(file, content.toString());
	}
	
	private Map<File, String> generateNavigationFile(OwlModel owlModel) throws IOException {
		final var content = new StringBuffer();
		
		content.append(
			"""
			<html>
			   <head></head>
			   <body style="background-image: url(icons/WatermarkRight.gif);  background-position: 100% 100%; background-repeat: no-repeat">
			      <font face="Verdana, Helvetica, sans-serif">
			         <p><b>Navigation</b></p>
			         <font size="-1">
			         &nbsp;<a href="summary.html" target="mainFrame">Summary</a><br>
			         &nbsp;<a href="ontologies.html" target="navigationFrame">Ontologies</a><br>
			         &nbsp;<a href="classes.html" target="navigationFrame">Classes</a><br>
			         &nbsp;<a href="datatypes.html" target="navigationFrame">Datatypes</a><br>
			         &nbsp;<a href="annotation_Properties.html" target="navigationFrame">Annotation Properties</a><br>
			         &nbsp;<a href="datatype_Properties.html" target="navigationFrame">Datatype Properties</a><br>
			         &nbsp;<a href="object_Properties.html" target="navigationFrame">Object Properties</a><br>
			         &nbsp;<a href="individuals.html" target="navigationFrame">Individuals</a><br>
			         </font>
			      </font>
			   </body>
			</html>			
			""");
		
		final var file = new File(options.outputFolderPath+File.separator+"navigation.html").getCanonicalFile();
		return Collections.singletonMap(file, content.toString());
	}

	private Map<File, String> generateOntologiesFile(OwlModel owlModel) throws IOException {
		final var files = new HashMap<File, String>();
		final var elements = new StringBuffer();

		for (var s : owlModel.ontologies) {
			var iri = s.getURI();
			var uri = URI.create(iri);
            var relativePath = uri.getAuthority()+uri.getPath()+hashCode(iri);
            var path = options.outputFolderPath+File.separator+relativePath+".html";
			elements.append(String.format(
				"""
				<tr>
				   <td><img border="0" src="icons/OWLOntology.greyed.gif" width="16" height="16"></td>
				   <td nowrap="nowrap"><font size="-1"><a href="%s" target="mainFrame">%s</a></font></td>
				</tr>
				""", 
				path, 
				iri));
			
			var axioms = getAxioms(s.listProperties());
			var classes = getTerms("owl:Class", iri, owlModel.classes);
			var datatypes = getTerms("rdfs:Datatype", iri, owlModel.datatypes);
			var annotationProperties = getTerms("owl:AnnotationProperty", iri, owlModel.annotationProperties);
			var datatypeProperties = getTerms("owl:DatatypeProperty", iri, owlModel.datatypeProperties);
			var objectProperties = getTerms("owl:ObjectProperty", iri, owlModel.objectProperties);
			var individuals = getTerms("owl:NamedIndividual", iri, owlModel.individuals);
			
			final var content = new StringBuffer(String.format(
				"""
				<html>
				   <head>
				      <title>%s</title>
				   </head>
				   <body>
				      <font face="Verdana, Helvetica, sans-serif">
				         <h2>
				            Ontology %s
				         </h2>
				         <hr>
				         %s
				         %s
				         %s
				         %s
				         %s
				         %s
				         %s
				      </font>
				   </body>
				</html>
				""",
				iri,
				iri,
				axioms,
				classes,
				datatypes,
				annotationProperties,
				objectProperties,
				datatypeProperties,
				individuals
			));
			files.put(new File(path).getCanonicalFile(), content.toString());
		};

		String content = String.format(
			"""
			<html>
			   <head>
			      <title>Ontologies</title>
			   </head>
			   <body>
			      <font face="Verdana, Helvetica, sans-serif">
			         <p><b>Ontologies</b></p>
			         <table>
			            <tbody>
								%s
			            </tbody>
			         </table>
			      </font>
			   </body>
			</html>
			""", 
			elements.toString().replaceAll("\n", "\n\t\t\t\t\t")); 

		final var file = new File(options.outputFolderPath+File.separator+"ontologies.html").getCanonicalFile();
		files.put(file, content.toString());
		return files;
	}

	private Map<File, String> generateClassesFile(OwlModel owlModel) throws IOException {
		final var files = new HashMap<File, String>();
		final var elements = new StringBuffer();

		Function<Restriction, Resource> qualifiedType = i -> {
			if (i.getOnProperty().isDatatypeProperty())
				return i.getProperty(OWL2.onDataRange).getObject().asResource();
			else
				return i.getProperty(OWL2.onClass).getObject().asResource();
		};
		
		Function<RDFNode, String> restrictionFunc = i -> {
			var restriction = (Restriction)i;
			var property = asString(restriction.getOnProperty());
			if (restriction.isAllValuesFromRestriction()) {
				property += " all " + asString(restriction.asAllValuesFromRestriction().getAllValuesFrom());
			} else if (restriction.isSomeValuesFromRestriction()) {
				property += " some " + asString(restriction.asSomeValuesFromRestriction().getSomeValuesFrom());
			} else if (restriction.isHasValueRestriction()) {
				property += " equals "+asString(restriction.asHasValueRestriction().getHasValue());
			} else if (restriction.isMinCardinalityRestriction()) {
				property += " min "+restriction.asMinCardinalityRestriction().getMinCardinality();
			} else if (restriction.isMaxCardinalityRestriction()) {
				property += " max "+restriction.asMaxCardinalityRestriction().getMaxCardinality();
			} else if (restriction.isCardinalityRestriction()) {
				property += " exactly "+restriction.asCardinalityRestriction().getCardinality();
			} else if (restriction.getProperty(OWL2.minQualifiedCardinality) != null) {
				property += " min "+restriction.getProperty(OWL2.minQualifiedCardinality).getInt();
				property += " "+ asString(qualifiedType.apply(restriction));
			} else if (restriction.getProperty(OWL2.maxQualifiedCardinality) != null) {
				property += " max "+restriction.getProperty(OWL2.maxQualifiedCardinality).getInt();
				property += " "+ asString(qualifiedType.apply(restriction));
			} else if (restriction.getProperty(OWL2.qualifiedCardinality) != null) {
				property += " exactly "+restriction.getProperty(OWL2.qualifiedCardinality).getInt();
				property += " "+ asString(qualifiedType.apply(restriction));
			}
			return property;
		};
		
		for (var s : owlModel.classes) {
			var localName = localName(s);
			var iri = s.getURI();
			var uri = URI.create(iri);
            var relativePath = uri.getAuthority()+uri.getPath()+(uri.getFragment() == null ? "" : File.separator+uri.getFragment())+hashCode(iri);
            var path = options.outputFolderPath+File.separator+relativePath+".html";
			elements.append(String.format(
				"""
				<tr>
					<td><img border="0" src="icons/OWLNamedClassDefined.gif" width="16" height="16"></td>
					<td nowrap="nowrap"><font size="-1"><a href="%s" target="mainFrame">%s</a></font></td>
				</tr>
				""", 
				path, 
				localName));
			
			var image = getClassImage(s);
			var axioms = getAxioms(s.listProperties());
			var superClasses = getTerms("rdfs:superClassOf", sortByAbbreviatedIri(s.listSubClasses(true).toList()));
			var properties = getTerms("rdfs:Property", sortByAbbreviatedIri(owlModel.ontModel.listSubjectsWithProperty(RDFS.domain, s).toList()), i -> asString(i)+" : "+asString(((OntProperty)i).listRange().toList()));
			var restrictedProperties = getTerms("owl:Restriction", s.listSuperClasses(true).filterKeep(i -> i.isRestriction()).mapWith(i -> i.asRestriction()).toList(), restrictionFunc);
			final var content = new StringBuffer(String.format(
				"""
				<html>
				   <head>
				      <title>%s</title>
				   </head>
				   <body>
				      <font face="Verdana, Helvetica, sans-serif">
				         <h2>
				            <font size="-1">%s</font><br>
				            Class %s
				         </h2>
				         <img src="http://www.plantuml.com/plantuml/svg/%s"/>
				         <hr>
				         %s
				         %s
				         %s
				         %s
				      </font>
				   </body>
				</html>
				""",
				abbreviatedIri(s),
				iri,
				abbreviatedIri(s),
				image,
				axioms,
				superClasses,
				properties,
				restrictedProperties));
			files.put(new File(path).getCanonicalFile(), content.toString());
		};

		String content = String.format(
			"""
			<html>
			   <head>
			      <title>Classes</title>
			   </head>
			   <body>
			      <font face="Verdana, Helvetica, sans-serif">
			         <p><b>Classes</b></p>
			         <table>
			            <tbody>
								%s
			            </tbody>
			         </table>
			      </font>
			   </body>
			</html>
			""", 
			elements.toString().replaceAll("\n", "\n\t\t\t\t\t")); 

		final var file = new File(options.outputFolderPath+File.separator+"classes.html").getCanonicalFile();
		files.put(file, content.toString());
		return files;
	}

	private Map<File, String> generateDatatypesFile(OwlModel owlModel) throws IOException {
		final var files = new HashMap<File, String>();
		final var elements = new StringBuffer();

		for (var s : owlModel.datatypes) {
			var localName = localName(s);
			var iri = s.getURI();
			var uri = URI.create(iri);
            var relativePath = uri.getAuthority()+uri.getPath()+(uri.getFragment() == null ? "" : File.separator+uri.getFragment())+hashCode(iri);
            var path = options.outputFolderPath+File.separator+relativePath+".html";
			elements.append(String.format(
				"""
				<tr>
					<td><img border="0" src="icons/OWLNamedDatatypeDefined.gif" width="16" height="16"></td>
					<td nowrap="nowrap"><font size="-1"><a href="%s" target="mainFrame">%s</a></font></td>
				</tr>
				""", 
				path, 
				localName));
			
			var listOfLiterals = owlModel.ontModel.listObjectsOfProperty(s, OWL2.equivalentClass).toList().stream()
					.map(i -> i.asResource())
					.filter(i -> i.hasProperty(OWL2.oneOf))
					.flatMap(i -> i.getProperty(OWL2.oneOf).getObject().as(RDFList.class).asJavaList().stream())
					.collect(Collectors.toList());
						
			var axioms = getAxioms(s.listProperties());
			var literals = getTerms("rdfs:Literal", listOfLiterals);
			
			final var content = new StringBuffer(String.format(
				"""
				<html>
				   <head>
				      <title>%s</title>
				   </head>
				   <body>
				      <font face="Verdana, Helvetica, sans-serif">
				         <h2>
				            <font size="-1">%s</font><br>
				            Datatype %s
				         </h2>
				         <hr>
				         %s
				         %s
				      </font>
				   </body>
				</html>
				""",
				abbreviatedIri(s),
				iri,
				abbreviatedIri(s),
				axioms,
				literals));
			files.put(new File(path).getCanonicalFile(), content.toString());
		};

		String content = String.format(
			"""
			<html>
			   <head>
			      <title>Datatypes</title>
			   </head>
			   <body>
			      <font face="Verdana, Helvetica, sans-serif">
			         <p><b>Datatypes</b></p>
			         <table>
			            <tbody>
								%s
			            </tbody>
			         </table>
			      </font>
			   </body>
			</html>
			""", 
			elements.toString().replaceAll("\n", "\n\t\t\t\t\t")); 

		final var file = new File(options.outputFolderPath+File.separator+"datatypes.html").getCanonicalFile();
		files.put(file, content.toString());
		return files;
	}

	private Map<File, String> generatePropertiesFile(List<? extends OntProperty> properties, String title) throws IOException {
		final var files = new HashMap<File, String>();
		final var elements = new StringBuffer();

		for (var s : properties) {
			var localName = localName(s);
			var iri = s.getURI();
			var uri = URI.create(iri);
            var relativePath = uri.getAuthority()+uri.getPath()+(uri.getFragment() == null ? "" : File.separator+uri.getFragment())+hashCode(iri);
            var path = options.outputFolderPath+File.separator+relativePath+".html";
			elements.append(String.format(
				"""
				<tr>
					<td><img border="0" src="icons/OWLNamedPropertyDefined.gif" width="16" height="16"></td>
					<td nowrap="nowrap"><font size="-1"><a href="%s" target="mainFrame">%s</a></font></td>
				</tr>
				""", 
				path, 
				localName));
			var axioms = getAxioms(s.listProperties());
			final var content = new StringBuffer(String.format(
				"""
				<html>
				   <head>
				      <title>%s</title>
				   </head>
				   <body>
				      <font face="Verdana, Helvetica, sans-serif">
				         <h2>
				            <font size="-1">%s</font><br>
				            Property %s
				         </h2>
				         <hr>
				         %s
				      </font>
				   </body>
				</html>
				""",
				abbreviatedIri(s),
				iri,
				abbreviatedIri(s),
				axioms));
			files.put(new File(path).getCanonicalFile(), content.toString());
		};

		String content = String.format(
			"""
			<html>
			   <head>
			      <title>%s</title>
			   </head>
			   <body>
			      <font face="Verdana, Helvetica, sans-serif">
			         <p><b>%s</b></p>
			         <table>
			            <tbody>
								%s
			            </tbody>
			         </table>
			      </font>
			   </body>
			</html>
			""",
			title,
			title,
			elements.toString().replaceAll("\n", "\n\t\t\t\t\t")); 

		final var file = new File(options.outputFolderPath+File.separator+title.replace(' ', '_').toLowerCase()+".html").getCanonicalFile();
		files.put(file, content.toString());
		return files;
	}

	private Map<File, String> generateIndividualsFile(OwlModel owlModel) throws IOException {
		final var files = new HashMap<File, String>();
		final var elements = new StringBuffer();

		for (var s : owlModel.individuals) {
			var localName = localName(s);
			var iri = s.getURI();
			var uri = URI.create(iri);
            var relativePath = uri.getAuthority()+uri.getPath()+(uri.getFragment() == null ? "" : File.separator+uri.getFragment())+hashCode(iri);
            var path = options.outputFolderPath+File.separator+relativePath+".html";
			elements.append(String.format(
				"""
				<tr>
					<td><img border="0" src="icons/OWLNamedIndividualDefined.gif" width="16" height="16"></td>
					<td nowrap="nowrap"><font size="-1"><a href="%s" target="mainFrame">%s</a></font></td>
				</tr>
				""", 
				path, 
				localName));
			var axioms = getAxioms(s.listProperties());
			final var content = new StringBuffer(String.format(
				"""
				<html>
				   <head>
				      <title>%s</title>
				   </head>
				   <body>
				      <font face="Verdana, Helvetica, sans-serif">
				         <h2>
				            <font size="-1">%s</font><br>
				            Individual %s
				         </h2>
				         <hr>
				         %s
				      </font>
				   </body>
				</html>
				""",
				abbreviatedIri(s),
				iri,
				abbreviatedIri(s),
				axioms));
			files.put(new File(path).getCanonicalFile(), content.toString());
		};

		String content = String.format(
			"""
			<html>
			   <head>
			      <title>Individuals</title>
			   </head>
			   <body>
			      <font face="Verdana, Helvetica, sans-serif">
			         <p><b>Individuals</b></p>
			         <table>
			            <tbody>
								%s
			            </tbody>
			         </table>
			      </font>
			   </body>
			</html>
			""", 
			elements.toString().replaceAll("\n", "\n\t\t\t\t\t")); 

		final var file = new File(options.outputFolderPath+File.separator+"individuals.html").getCanonicalFile();
		files.put(file, content.toString());
		return files;
	}

	private String getAxioms(StmtIterator i) {
		var statements = new TreeMap<Property, Set<RDFNode>>(new Comparator<Property>() {
			@Override
			public int compare(Property o1, Property o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});
				
		while (i.hasNext()) {
			var stmt = i.next();
			var predicate = stmt.getPredicate();
			var object = stmt.getObject();
			if (!object.isAnon()) {
				var objects = statements.get(predicate);
				if (objects == null) {
					statements.put(predicate, objects = new TreeSet<RDFNode>(new Comparator<RDFNode>() {
						@Override
						public int compare(RDFNode o1, RDFNode o2) {
							return o1.toString().compareTo(o2.toString());
						}
					}));
				}
				objects.add(object);
			}
		}
		
		var properties = new StringBuffer();

		for (var predicate : statements.keySet()) {
			var objects = new StringBuffer();
			for (var object : statements.get(predicate)) {
				objects.append(String.format(
					"""
		            <tr>
		               <td valign="TOP">
		                  <img border="0" src="icons/RDFSClass.greyed.gif" width="16" height="16">
		               </td>
		               <td>
		                  <font size="-1">%s</font>
		               </td>
		            </tr>
					""",
					asString(object)
				));
			}
			properties.append(String.format(
				"""
				<b><font size="-1">%s</font></b>
				<dl>
				   <dd>
				      <table>
				         <tbody>
				            %s
				         </tbody>
				      </table>
				   </dd>
				</dl>
				""",
				abbreviatedIri(predicate),
				objects
			));
		}
		
		return properties.toString();
	}

	private String getTerms(String title, String ontologyIri, List<? extends Resource> resources) {
		return getTerms(title, ontologyIri, resources, i -> asString(i));
	}
	
	private String getTerms(String title, String ontologyIri, List<? extends Resource> resources, Function<RDFNode, String> getLabel) {
		var terms = sortByName(resources).stream()
				.filter(i -> i.getURI().startsWith(ontologyIri))
				.collect(Collectors.toList());
		return getTerms(title, terms, getLabel);
	}

	private String getTerms(String title, List<? extends RDFNode> terms) {
		return getTerms(title, terms, i -> asString(i));
	}

	private String getTerms(String title, List<? extends RDFNode> terms, Function<RDFNode, String> getLabel) {
		var s = new StringBuffer();
		for (var term : terms) {
			s.append(String.format(
				"""
	            <tr>
	               <td valign="TOP">
	                  <img border="0" src="icons/RDFSClass.greyed.gif" width="16" height="16">
	               </td>
	               <td>
	                  <font size="-1">%s</font>
	               </td>
	            </tr>
				""",
				getLabel.apply(term)
			));
		}
		return s.length() == 0 ? "" : String.format(
			"""
			<b><font size="-1">%s</font></b>
			<dl>
			   <dd>
			      <table>
			         <tbody>
			            %s
			         </tbody>
			      </table>
			   </dd>
			</dl>
			""", title, s);
	}

	private static String getClassImage(OntClass ontClass) {
		var superClasses = String.join(",", ontClass.listSuperClasses(true).mapWith(i -> i.getLocalName()).filterKeep(i -> i != null) .toList());
		return plantUmlImage(String.format(
			"""
			class %s %s {}
			""",
			ontClass.getLocalName(),
			superClasses.length()>0 ? "extends "+superClasses : ""));
	}
	
	//----------------------------------------------------------------------------------

	private static <T extends Resource> List<T> sortByIri(List<T> resources) {
		var filtered = resources.stream().filter(i -> !i.isAnon()).collect(Collectors.toList());
		filtered.sort((x1, x2) -> x1.getURI().compareTo(x2.getURI()));
		return filtered;
	}
	
	private static <T extends Resource> List<T> sortByName(List<T> resources) {
		var filtered = resources.stream().filter(i -> !i.isAnon()).collect(Collectors.toList());
		filtered.sort((x1, x2) -> localName(x1).compareTo(localName(x2)));
		return filtered;
	}

	private static <T extends Resource> List<T> sortByAbbreviatedIri(List<T> resources) {
		var filtered = resources.stream().filter(i -> !i.isAnon()).collect(Collectors.toList());
		filtered.sort((x1, x2) -> abbreviatedIri(x1).compareTo(abbreviatedIri(x2)));
		return filtered;
	}

	private static final Pattern LINK = Pattern.compile("\\{\\{([^\\s\\}]+)\s*(.*?)\\}\\}");
	private String replaceLinks(String input, OntModel ontModel) {
		Matcher matcher = LINK.matcher(input);
		return matcher.replaceAll(match -> {
		    String url = match.group(1);
		    String text = match.group(2);
		    String name = url;
		    if (url.contains("#")) {
		    	String[] iri = url.split("#");
		    	name = iri[1];
		    } else if (url.contains("/")) {
		    	int index = url.lastIndexOf('/');
		    	name = url.substring(index+1);
		    } else {
			    String[] iri = url.split(":");
				if (iri.length == 2) {
					String prefix = iri[0];
					name = iri[1];
					String ns = null;
					for (Graph g : ontModel.getSubGraphs()) {
						ns = g.getPrefixMapping().getNsPrefixURI(prefix);
						if (ns != null) {
							break;
						}
					}
					url = ns+name;
				}
			}
			if (text.length() == 0) {
				text = name;
			}
		    return String.format("<a href=\"%s\">%s</a>", path(url), text);
		});
	}
		
	private static String plantUmlImage(String diagram) {
        try {
            return TranscoderUtil.getDefaultTranscoder().encode(diagram);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	private String hashCode(String s) {
		return options.outputCaseSensitive ? s : "_"+Math.abs(s.hashCode());
	}

	private static final String[] array = {"oml", "owl", "rdfs", "rdf", "xsd"};
	private static final List<String> default_namespaces = new ArrayList<>(Arrays.asList(array));
	private boolean isStandard(String qname) {
		return default_namespaces.stream().anyMatch(i -> qname.startsWith(i));
	}
	
	private String asString(RDFNode node) {
		if (node.isResource()) {
			var resource = node.asResource();
			var iri = resource.getURI();
            var path = path(iri);
			var qname = abbreviatedIri(resource);
			var label = qname.equals(iri)? iri : qname;
			return isStandard(qname) ? label : String.format("<a href=\"%s\">%s</a>", path, label); 
		}
		return replaceLinks(node.toString(), (OntModel)node.getModel());
	}

	private String asString(List<? extends RDFNode> nodes) {
		return String.join(" ^ ", nodes.stream()
				.map(i -> asString(i))
				.collect(Collectors.toList()));
	}
	
	private String path(String iri) {
		var uri = URI.create(iri);
        var relativePath = uri.getAuthority()+uri.getPath()+(uri.getFragment() == null ? "" : File.separator+uri.getFragment())+hashCode(iri);
        return options.outputFolderPath+File.separator+relativePath+".html";
	}
	
	private static String abbreviatedIri(Resource resource) {
		var localName = localName(resource);
		String prefix = prefix(resource);
		return (prefix != null) ? prefix+":"+localName : resource.getURI();
	}

	private static String prefix(Resource resource) {
		var namespace = namespace(resource);
		String prefix = null;
		for (Graph g : ((OntModel)resource.getModel()).getSubGraphs()) {
			prefix = g.getPrefixMapping().getNsURIPrefix(namespace);
			if (prefix != null) {
				break;
			}
		}
		return prefix;
	}
	
	private static String localName(Resource resource) {
		var iri = resource.getURI();
		int index = iri.lastIndexOf("#");
		if (index == -1) {
			index = iri.lastIndexOf("/");
		}
		return (index != -1) ? iri.substring(index+1) : resource.getLocalName();
	}

	private static String namespace(Resource resource) {
		var iri = resource.getURI();
		int index = iri.lastIndexOf("#");
		if (index == -1) {
			index = iri.lastIndexOf("/");
		}
		return (index != -1) ? iri.substring(0, index+1) : resource.getNameSpace();
	}

	//----------------------------------------------------------------------------------

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

	/**
	 * Get application version id from properties file.
	 * 
	 * @return version string from build.properties or UNKNOWN
	 */
	private String getAppVersion() {
    	var version = this.getClass().getPackage().getImplementationVersion();
    	return (version != null) ? version : "<SNAPSHOT>";
    }

}