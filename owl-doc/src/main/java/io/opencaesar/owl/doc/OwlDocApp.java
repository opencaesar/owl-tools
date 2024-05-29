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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
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
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.alg.cycle.TiernanSimpleCycles;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import net.sourceforge.plantuml.code.TranscoderUtil;

/**
 * A utility for running a reasoner on the set of ontologies in scope of an OASIS XML catalog.
 */
public class OwlDocApp {

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
		private URL cssFilePath = OwlDocApp.class.getClassLoader().getResource(CSS_DEFAULT);

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
			ontologies = sortByIri(ontModel.listOntologies().filterKeep(i-> hasTerms(i)).toList());
			classes = sortByName(ontModel.listNamedClasses().toList());
			datatypes = sortByName(ontModel.listSubjectsWithProperty(RDF.type, RDFS.Datatype).toList());
			annotationProperties = sortByName(ontModel.listAnnotationProperties().toList());
			objectProperties = sortByName(ontModel.listObjectProperties().toList());
			datatypeProperties = sortByName(ontModel.listDatatypeProperties().toList());
			individuals = sortByName(ontModel.listIndividuals().toList());
			
			// Treat the DC terms ontology as other standard ontologies to avoid clutter
			ontologies.removeIf(i -> i.hasURI("http://purl.org/dc/elements/1.1"));
			annotationProperties.removeIf(i -> i.getURI().startsWith("http://purl.org/dc/elements/1.1"));
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
		
		outputFiles.putAll(copyCssFile());
		
		// create output files
		outputFiles.putAll(generateIndexFile(owlModel));
		outputFiles.putAll(generateSummaryFile(owlModel));
		outputFiles.putAll(generateNavigationFile(owlModel));
		outputFiles.putAll(generateClassHierarchyFile(owlModel));
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
	
	private Map<File, String> copyCssFile() throws IOException {
		var inputStream = options.cssFilePath.openStream();
		 Scanner s = new Scanner(inputStream);
		 String content = s.useDelimiter("\\A").hasNext() ? s.next() : "";
		 s.close();
		var file = new File(options.outputFolderPath+File.separator+CSS_MAIN);
		return Collections.singletonMap(file, content);
	}
	
	private Map<File, String> generateIndexFile(OwlModel owlModel) throws IOException {
		final var content = new StringBuffer();
		
		content.append(
			"""
			<html>
			   <head>
			      <title>Ontology Documentation</title>
			   </head>
			   <frameset cols="30%,70%" title="" onLoad="top.loadFrames()">
			      <frameset rows="40%,60%" title="" onLoad="top.loadFrames()">
			         <frame src="navigation.html" title="Navigation">
			         <frame src="classes.html" name="navigationFrame" title="Elements">
			      </frameset>
			      <frame src="summary.html" name="mainFrame" title="Details" scrolling="yes"/>
			   </frameset>
			</html>
			""");
		
		final var file = new File(options.outputFolderPath+File.separator+"index.html").getCanonicalFile();
		return Collections.singletonMap(file, content.toString());
	}

	private Map<File, String> generateSummaryFile(OwlModel owlModel) throws IOException {
		final var content = new StringBuffer();
		final var path = options.outputFolderPath+File.separator+"summary.html";

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
	    LocalDateTime now = LocalDateTime.now();  
	    var timestamp = dtf.format(now); 
	    var image = getOntologiesImage(CSS_DEFAULT, owlModel);

		content.append(String.format(
			"""
			<html>
			   <head>
			      <title>%s</title>
			      <link rel="stylesheet" href="%s">
			   </head>
			   <body>
		          <h1>%s v%s</h1>
		          <p class="date">Updated on: %s</p>
		          <hr>
			      <img src="http://www.plantuml.com/plantuml/svg/%s"/>
			   </body>
			</html>
			""",
			options.inputCatalogTitle,
			getRelativeCSSPath(path),
			options.inputCatalogTitle,
			options.inputCatalogVersion,
			timestamp,
			image));
				
		final var file = new File(path).getCanonicalFile();
		return Collections.singletonMap(file, content.toString());
	}
	
	private Map<File, String> generateNavigationFile(OwlModel owlModel) throws IOException {
		final var files = new HashMap<File, String>();
		final var path = options.outputFolderPath+File.separator+"navigation.html";
		final var elements = new StringBuffer();

		for (var s : owlModel.ontologies) {
			var iri = s.getURI();
			var uri = URI.create(iri);
            var relativePath = uri.getAuthority()+uri.getPath()+hashCode(iri);
            var path1 = options.outputFolderPath+File.separator+relativePath+".html";
            var path2 = options.outputFolderPath+File.separator+relativePath+"_2.html";
			elements.append(String.format(
				"""
				<tr>
				   <td><img border="0" src="%s" width="16" height="16"></td>
				   <td nowrap="nowrap"><a href="%s" target="navigationFrame">%s</a></td>
				</tr>
				""", 
				IMG_ONTOLOGY,
				getRelativePath(path, path2), 
				iri));
			
			var axioms = getAxioms(path1, s.listProperties());
			var classes = getTerms(path1, "Classes", iri, IMG_CLASS, owlModel.classes);
			var datatypes = getTerms(path1, "Datatypes", iri, IMG_DATATYPE, owlModel.datatypes);
			var annotationProperties = getTerms(path1, "Annotation Properties", iri, IMG_PROPERTY, owlModel.annotationProperties);
			var datatypeProperties = getTerms(path1, "Datatype Properties", iri, IMG_PROPERTY, owlModel.datatypeProperties);
			var objectProperties = getTerms(path1, "Object Properties", iri, IMG_PROPERTY, owlModel.objectProperties);
			var individuals = getTerms(path1, "Individuals", iri, IMG_INDIVIDUAL, owlModel.individuals);
			
			final var content1 = new StringBuffer(String.format(
					"""
					<html>
					   <head>
					      <title>%s</title>
						  <link rel="stylesheet" href="%s">
					   </head>
					   <body>
					      <p><a href="%s" target="mainFrame">%s</a></p>
						  %s
						  %s
						  %s
						  %s
						  %s
						  %s
					   </body>
					</html>
					""",
					iri,
					getRelativeCSSPath(path2),
					getRelativePath(path2, path1),
					iri,
					classes,
					datatypes,
					annotationProperties,
					datatypeProperties,
					objectProperties,
					individuals
				));
				files.put(new File(path2).getCanonicalFile(), content1.toString());

				final var content2 = new StringBuffer(String.format(
					"""
					<html>
					   <head>
					      <title>%s</title>
						  <link rel="stylesheet" href="%s">
					   </head>
					   <body>
						  <h3>Ontology %s</h3>
					      <hr>
					      %s
					   </body>
					</html>
					""",
					iri,
					getRelativeCSSPath(path1),
					iri,
					axioms
				));
				files.put(new File(path1).getCanonicalFile(), content2.toString());
		};

		String content = String.format(
			"""
			<html>
			   <head>
			      <title>Ontologies</title>
				  <link rel="stylesheet" href="%s">
			   </head>
			   <body>
			         <h3>Index</h3>
			         &nbsp;<a href="class-hierarchy.html" target="navigationFrame">Class Hierarchy</a><br>
			         &nbsp;<a href="classes.html" target="navigationFrame">All Classes</a><br>
			         &nbsp;<a href="datatypes.html" target="navigationFrame">All Datatypes</a><br>
			         &nbsp;<a href="annotation_properties.html" target="navigationFrame">All Annotation Properties</a><br>
			         &nbsp;<a href="datatype_properties.html" target="navigationFrame">All Datatype Properties</a><br>
			         &nbsp;<a href="object_properties.html" target="navigationFrame">All Object Properties</a><br>
			         &nbsp;<a href="individuals.html" target="navigationFrame">All Individuals</a><br>
			         <h3>Ontologies</h3>
			         <table>
			            <tbody>
								%s
			            </tbody>
			         </table>
			   </body>
			</html>
			""", 
			getRelativeCSSPath(path),
			elements.toString().replaceAll("\n", "\n\t\t\t\t\t")); 

		final var file = new File(path).getCanonicalFile();
		files.put(file, content.toString());
		return files;
	}

	private Map<File, String> generateClassesFile(OwlModel owlModel) throws IOException {
		final var files = new HashMap<File, String>();
		final var path = options.outputFolderPath+File.separator+"classes.html";
		final var elements = new StringBuffer();

		Function<Restriction, Resource> qualifiedType = i -> {
			if (i.getOnProperty().isDatatypeProperty())
				return i.getProperty(OWL2.onDataRange).getObject().asResource();
			else
				return i.getProperty(OWL2.onClass).getObject().asResource();
		};
		
		for (var s : owlModel.classes) {
			var localName = localName(s);
			var iri = s.getURI();
			var uri = URI.create(iri);
            var relativePath = uri.getAuthority()+uri.getPath()+(uri.getFragment() == null ? "" : File.separator+uri.getFragment())+hashCode(iri);
            var path1 = options.outputFolderPath+File.separator+relativePath+".html";
			elements.append(String.format(
				"""
				<tr>
					<td><img border="0" src="%s" width="16" height="16"></td>
					<td nowrap="nowrap"><a href="%s" target="mainFrame">%s</a></td>
				</tr>
				""",
				IMG_CLASS,
				getRelativePath(path, path1), 
				localName));

			Function<RDFNode, String> restrictionFunc = i -> {
				var restriction = (Restriction)i;
				var property = asString(path1, restriction.getOnProperty());
				if (restriction.isAllValuesFromRestriction()) {
					property += " → all values from " + asString(path1, restriction.asAllValuesFromRestriction().getAllValuesFrom());
				} else if (restriction.isSomeValuesFromRestriction()) {
					property += " has some values from " + asString(path1, restriction.asSomeValuesFromRestriction().getSomeValuesFrom());
				} else if (restriction.isHasValueRestriction()) {
					property += " → value of "+asString(path1, restriction.asHasValueRestriction().getHasValue());
				} else if (restriction.isMinCardinalityRestriction()) {
					property += " → min cardinality of "+restriction.asMinCardinalityRestriction().getMinCardinality();
				} else if (restriction.isMaxCardinalityRestriction()) {
					property += " → max cardinality of "+restriction.asMaxCardinalityRestriction().getMaxCardinality();
				} else if (restriction.isCardinalityRestriction()) {
					property += " → exact cardinality of "+restriction.asCardinalityRestriction().getCardinality();
				} else if (restriction.getProperty(OWL2.minQualifiedCardinality) != null) {
					property += " → min cardinality of "+restriction.getProperty(OWL2.minQualifiedCardinality).getInt();
					property += " "+ asString(path1, qualifiedType.apply(restriction));
				} else if (restriction.getProperty(OWL2.maxQualifiedCardinality) != null) {
					property += " → max cardinality of "+restriction.getProperty(OWL2.maxQualifiedCardinality).getInt();
					property += " "+ asString(path1, qualifiedType.apply(restriction));
				} else if (restriction.getProperty(OWL2.qualifiedCardinality) != null) {
					property += " → exact cardinality of "+restriction.getProperty(OWL2.qualifiedCardinality).getInt();
					property += " "+ asString(path1, qualifiedType.apply(restriction));
				}
				return property;
			};

			var image = getClassImage(path1, s);
			var axioms = getAxioms(path1, s.listProperties());
			var superClasses = getNodes(path1, "directSuperClassOf", IMG_CLASS, s.listSubClasses(true).filterDrop(i -> i.isAnon()).toList());
			var properties = getNodes("directDomainOf", owlModel.ontModel.listSubjectsWithProperty(RDFS.domain, s).toList(), IMG_PROPERTY, i -> asString(path1, i)+" → "+asString(path1, owlModel.ontModel.getOntProperty(i.asResource().getURI()).listRange().toList()));
			var restrictedProperties = getNodes("directHasRestrictionOn", s.listSuperClasses(true).filterKeep(i -> i.isRestriction()).mapWith(i -> i.asRestriction()).toList(), IMG_PROPERTY, restrictionFunc);
			final var content = new StringBuffer(String.format(
				"""
				<html>
				   <head>
				      <title>%s</title>
					  <link rel="stylesheet" href="%s">
				   </head>
				   <body>
			          <h2><font size="-1">%s</font><br>Class %s</h2>
			          <img src="http://www.plantuml.com/plantuml/svg/%s"/>
			          <hr>
			          %s
			          %s
			          %s
			          %s
				   </body>
				</html>
				""",
				abbreviatedIri(s),
				getRelativeCSSPath(path1),
				iri,
				abbreviatedIri(s),
				image,
				axioms,
				superClasses,
				properties,
				restrictedProperties));
			files.put(new File(path1).getCanonicalFile(), content.toString());
		};

		String content = String.format(
			"""
			<html>
			   <head>
			      <title>All Classes</title>
				  <link rel="stylesheet" href="%s">
			   </head>
			   <body>
		          <h3>All Classes</h3>
		          <table>
		             <tbody>
			 				%s
		             </tbody>
		          </table>
			   </body>
			</html>
			""",
			getRelativeCSSPath(path),
			elements.toString().replaceAll("\n", "\n\t\t\t\t\t")); 

		final var file = new File(path).getCanonicalFile();
		files.put(file, content.toString());
		return files;
	}

	private Map<File, String> generateClassHierarchyFile(OwlModel owlModel) throws IOException {
		final var path = options.outputFolderPath+File.separator+"class-hierarchy.html";
		final var elements = new StringBuffer();

        Deque<OntClass> stack = new ArrayDeque<>();
        Map<OntClass, Integer> levels = new HashMap<>();
        stack.addAll(owlModel.ontModel.listHierarchyRootClasses().filterDrop(i -> i.isAnon()).toList());
        stack.forEach(i -> levels.put(i, 0));

        while (!stack.isEmpty()) {
        	OntClass aClass = stack.pop();

        	StringBuffer spaces = new StringBuffer();
            int indentLevel = levels.get(aClass);
            for (int i = 0; i < indentLevel; i++) {
                spaces.append("&nbsp;&nbsp;&nbsp;&nbsp;");
            }

			var iri = aClass.getURI();
			var uri = URI.create(iri);
            var relativePath = uri.getAuthority()+uri.getPath()+(uri.getFragment() == null ? "" : File.separator+uri.getFragment())+hashCode(iri);
            var path1 = options.outputFolderPath+File.separator+relativePath+".html";
			elements.append(String.format(
				"""
				<tr>
				   <td>%s<img border="0" src="%s" width="16" height="16">&nbsp;<a href="%s" target="mainFrame">%s</a></td>
				</tr>
				""",
				spaces.toString(),
				IMG_CLASS,
				getRelativePath(path, path1), 
				aClass.getLocalName()));

            List<OntClass> subClasses = aClass.listSubClasses(true).filterDrop(i -> i.isAnon()).toList();

            for (OntClass subClass : subClasses) {
                levels.put(subClass, indentLevel+1);
                stack.push(subClass);
            }
        }

        final var contents = String.format(
			"""
			<html>
			   <head>
			      <title>Class Hierarchy</title>
				  <link rel="stylesheet" href="%s">
			   </head>
			   <body>
		          <h3>Class Hierarchy</h3>
		          <table>
		             <tbody>
			 				%s
		             </tbody>
		          </table>
			   </body>
			</html>
			""",
			getRelativeCSSPath(path),
			elements.toString().replaceAll("\n", "\n\t\t\t\t\t")); 
		
		final var file = new File(path).getCanonicalFile();
		return Collections.singletonMap(file, contents.toString());
	}
	
	private Map<File, String> generateDatatypesFile(OwlModel owlModel) throws IOException {
		final var files = new HashMap<File, String>();
		final var path = options.outputFolderPath+File.separator+"datatypes.html";
		final var elements = new StringBuffer();

		for (var s : owlModel.datatypes) {
			var localName = localName(s);
			var iri = s.getURI();
			var uri = URI.create(iri);
            var relativePath = uri.getAuthority()+uri.getPath()+(uri.getFragment() == null ? "" : File.separator+uri.getFragment())+hashCode(iri);
            var path1 = options.outputFolderPath+File.separator+relativePath+".html";
			elements.append(String.format(
				"""
				<tr>
					<td><img border="0" src="%s" width="16" height="16"></td>
					<td nowrap="nowrap"><a href="%s" target="mainFrame">%s</a></td>
				</tr>
				""",
				IMG_DATATYPE,
				getRelativePath(path, path1), 
				localName));
			
			var listOfLiterals = owlModel.ontModel.listObjectsOfProperty(s, OWL2.equivalentClass).toList().stream()
					.map(i -> i.asResource())
					.filter(i -> i.hasProperty(OWL2.oneOf))
					.flatMap(i -> i.getProperty(OWL2.oneOf).getObject().as(RDFList.class).asJavaList().stream())
					.collect(Collectors.toList());
						
			var axioms = getAxioms(path1, s.listProperties());
			var literals = getNodes(path1, "rdf:typeOf", IMG_ITEM, listOfLiterals);
			
			final var content = new StringBuffer(String.format(
				"""
				<html>
				   <head>
				      <title>%s</title>
					  <link rel="stylesheet" href="%s">
				   </head>
				   <body>
			          <h2><font size="-1">%s</font><br>Datatype %s</h2>
			          <hr>
			          %s
			          %s
				   </body>
				</html>
				""",
				abbreviatedIri(s),
				getRelativeCSSPath(path1),
				iri,
				abbreviatedIri(s),
				axioms,
				literals));
			files.put(new File(path1).getCanonicalFile(), content.toString());
		};

		String content = String.format(
			"""
			<html>
			   <head>
			      <title>All Datatypes</title>
				  <link rel="stylesheet" href="%s">
			   </head>
			   <body>
		          <h3>All Datatypes</h3>
		          <table>
		             <tbody>
			 				%s
		             </tbody>
		          </table>
			   </body>
			</html>
			""", 
			getRelativeCSSPath(path),
			elements.toString().replaceAll("\n", "\n\t\t\t\t\t")); 

		final var file = new File(path).getCanonicalFile();
		files.put(file, content.toString());
		return files;
	}

	private Map<File, String> generatePropertiesFile(List<? extends OntProperty> properties, String title) throws IOException {
		final var files = new HashMap<File, String>();
		final var path = options.outputFolderPath+File.separator+title.replace(' ', '_').toLowerCase()+".html";
		final var elements = new StringBuffer();

		for (var s : properties) {
			var localName = localName(s);
			var iri = s.getURI();
			var uri = URI.create(iri);
            var relativePath = uri.getAuthority()+uri.getPath()+(uri.getFragment() == null ? "" : File.separator+uri.getFragment())+hashCode(iri);
            var path1 = options.outputFolderPath+File.separator+relativePath+".html";
			elements.append(String.format(
				"""
				<tr>
					<td><img border="0" src="%s" width="16" height="16"></td>
					<td nowrap="nowrap"><a href="%s" target="mainFrame">%s</a></td>
				</tr>
				""",
				IMG_PROPERTY,
				getRelativePath(path, path1), 
				localName));
			var axioms = getAxioms(path1, s.listProperties());
			final var content = new StringBuffer(String.format(
				"""
				<html>
				   <head>
				      <title>%s</title>
				      <link rel="stylesheet" href="%s">
				   </head>
				   <body>
			          <h2><font size="-1">%s</font><br>Property %s</h2>
			          <hr>
			          %s
				   </body>
				</html>
				""",
				abbreviatedIri(s),
				getRelativeCSSPath(path1),
				iri,
				abbreviatedIri(s),
				axioms));
			files.put(new File(path1).getCanonicalFile(), content.toString());
		};

		String content = String.format(
			"""
			<html>
			   <head>
			      <title>All %s</title>
				  <link rel="stylesheet" href="%s">
			   </head>
			   <body>
		          <h2>All %s</h2>
		          <table>
		             <tbody>
			 				%s
		             </tbody>
		          </table>
			   </body>
			</html>
			""",
			title,
			getRelativeCSSPath(path),
			title,
			elements.toString().replaceAll("\n", "\n\t\t\t\t\t")); 

		final var file = new File(path).getCanonicalFile();
		files.put(file, content.toString());
		return files;
	}

	private Map<File, String> generateIndividualsFile(OwlModel owlModel) throws IOException {
		final var files = new HashMap<File, String>();
		final var path = options.outputFolderPath+File.separator+"individuals.html";
		final var elements = new StringBuffer();

		for (var s : owlModel.individuals) {
			var localName = localName(s);
			var iri = s.getURI();
			var uri = URI.create(iri);
            var relativePath = uri.getAuthority()+uri.getPath()+(uri.getFragment() == null ? "" : File.separator+uri.getFragment())+hashCode(iri);
            var path1 = options.outputFolderPath+File.separator+relativePath+".html";
			elements.append(String.format(
				"""
				<tr>
					<td><img border="0" src="%s" width="16" height="16"></td>
					<td nowrap="nowrap"><a href="%s" target="mainFrame">%s</a></td>
				</tr>
				""",
				IMG_INDIVIDUAL,
				getRelativePath(path, path1), 
				localName));
			var axioms = getAxioms(path1, s.listProperties());
			final var content = new StringBuffer(String.format(
				"""
				<html>
				   <head>
				      <title>%s</title>
				      <link rel="stylesheet" href="%s">
				   </head>
				   <body>
			          <h2><font size="-1">%s</font><br>Individual %s</h2>
			          <hr>
			          %s
				   </body>
				</html>
				""",
				abbreviatedIri(s),
				getRelativeCSSPath(path1),
				iri,
				abbreviatedIri(s),
				axioms));
			files.put(new File(path1).getCanonicalFile(), content.toString());
		};

		String content = String.format(
			"""
			<html>
			   <head>
			      <title>All Individuals</title>
				  <link rel="stylesheet" href="%s">
			   </head>
			   <body>
		          <h2>All Individuals</h2>
		          <table>
		             <tbody>
			 				%s
		             </tbody>
		          </table>
			   </body>
			</html>
			""", 
			getRelativeCSSPath(path),
			elements.toString().replaceAll("\n", "\n\t\t\t\t\t")); 

		final var file = new File(path).getCanonicalFile();
		files.put(file, content.toString());
		return files;
	}

	private String getAxioms(String contextFilePath, StmtIterator i) {
		var propertyToValues = new TreeMap<Property, Set<RDFNode>>(new Comparator<Property>() {
			@Override
			public int compare(Property o1, Property o2) {
				return o1.getURI().compareTo(o2.getURI());
			}
		});
				
		while (i.hasNext()) {
			var stmt = i.next();
			var property = stmt.getPredicate();
			var object = stmt.getObject();
			if (!object.isAnon()) {
				var values = propertyToValues.get(property);
				if (values == null) {
					propertyToValues.put(property, values = new TreeSet<RDFNode>(new Comparator<RDFNode>() {
						@Override
						public int compare(RDFNode o1, RDFNode o2) {
							return o1.toString().compareTo(o2.toString());
						}
					}));
				}
				values.add(object);
			}
		}
		
		var propertiesWithValues = propertyToValues.keySet().stream()
				.filter(j -> !propertyToValues.get(j).isEmpty())
				.collect(Collectors.toList());

		var axioms = new StringBuffer();
		
		for (var property : propertiesWithValues.stream().filter(j -> propertyToValues.get(j).iterator().next().isLiteral()).collect(Collectors.toList())) {
			axioms.append(getNodes(contextFilePath, abbreviatedIri(property), IMG_ITEM, propertyToValues.get(property)));
		}

		for (var property : propertiesWithValues.stream().filter(j -> propertyToValues.get(j).iterator().next().isResource()).collect(Collectors.toList())) {
			axioms.append(getNodes(contextFilePath, abbreviatedIri(property), IMG_ITEM, propertyToValues.get(property)));
		}

		return axioms.toString();
	}

	private String getTerms(String contextFilePath, String title, String ontologyIri, String image, Collection<? extends Resource> terms) {
		var ontoloyTerms = terms.stream()
				.filter(i -> i.getURI().startsWith(ontologyIri))
				.collect(Collectors.toList());

		var table = getTable(ontoloyTerms, image, i -> asLocalName(contextFilePath, (Resource)i));

		return ontoloyTerms.isEmpty() ? "" : String.format(
				"""
				<h3>%s</h3>
				%s
				""",
				title,
				table);
	}

	private String getNodes(String contextFilePath, String title, String image, Collection<? extends RDFNode> terms) {
		return getNodes(title, terms, image, i -> asString(contextFilePath, i));
	}

	private String getNodes(String title, Collection<? extends RDFNode> terms, String image, Function<RDFNode, String> getLabel) {
		var table = getTable(terms, image, getLabel);
		return terms.isEmpty() ? "" : String.format(
				"""
				<h3>%s</h3>
				<dl>
				   <dd>
				     %s
				   </dd>
				</dl>
				""",
				title,
				table);
	}

	private String getTable(Collection<? extends RDFNode> nodes, String image, Function<RDFNode, String> getLabel) {
		var s = new StringBuffer();
		for (var node : sortNodes(nodes, getLabel)) {
			s.append(String.format(
				"""
	            <tr>
	               <td valign="TOP">
	                  <img border="0" src="%s" width="16" height="16">
	               </td>
	               <td>
	                 %s
	               </td>
	            </tr>
				""",
				image,
				getLabel.apply(node)
			));
		}
		return s.length() == 0 ? "" : String.format(
			"""
		    <table>
			   <tbody>
		            %s
		       </tbody>
			</table>
			<p/>
			""", s);
	}

	private String getClassImage(String contextFilePath, OntClass ontClass) {
		String thisClass = String.format(
				"""
				class %s [[%s]]
				""",
				ontClass.getLocalName(),
				path(contextFilePath, ontClass.getURI()));
		
		
		var superClasses = String.join("\n", ontClass.listSuperClasses(true).filterKeep(i -> i.isURIResource()).mapWith(i -> String.format(
				"""
				class %s [[%s]]
				%s -up-|> %s
				""",
				i.getLocalName(),
				path(contextFilePath, ontClass.getURI()),
				ontClass.getLocalName(),
				i.getLocalName()
		)).toList());

		var subClasses = String.join("\n", ontClass.listSubClasses(true).filterKeep(i -> i.isURIResource()).mapWith(i -> String.format(
				"""
				class %s [[%s]]
				hide %s members
				%s -up-|> %s
				""",
				i.getLocalName(),
				path(contextFilePath, ontClass.getURI()),
				i.getLocalName(),
				i.getLocalName(),
				ontClass.getLocalName()
		)).toList());

		return plantUmlImage(thisClass+superClasses+subClasses);
	}

	/**
	 * Import Edge
	 */
	public static class Import extends DefaultEdge {
		private static final long serialVersionUID = 1L;
		/** Default Constructor */
		public Import() {}
		public Ontology getSource() {return (Ontology)super.getSource();}
		public Ontology getTarget() {return (Ontology)super.getTarget();}
	};

	private String getOntologiesImage(String contextFilePath, OwlModel owlModel) {
		var graph = new SimpleDirectedGraph<Ontology, Import>(Import.class);

		var packages = String.join("\n", owlModel.ontologies.stream().map(i -> {
			graph.addVertex(i);
			return String.format(
				"""
				package "%s" {}
				""",
				i.getURI());
		}).toList());

		owlModel.ontologies.stream().forEach(i -> i.listImports().filterKeep(k -> owlModel.ontologies.contains(k)).mapWith(k -> k.asOntology()).forEach(j -> {
			graph.addEdge(i, j);
		}));
		
		var algorithm = new TiernanSimpleCycles<Ontology, Import>(graph);
		var cycles = algorithm.findSimpleCycles();
		while (!cycles.isEmpty()) {
			var maxCycle = cycles.stream().max(Comparator.comparingInt(List::size)).orElse(null);
			graph.removeEdge(maxCycle.get(0), maxCycle.get(1));
			cycles = algorithm.findSimpleCycles();
		}
		TransitiveReduction.INSTANCE.reduce(graph);
		
		var imports = String.join("\n", graph.edgeSet().stream().map(i -> String.format(
			"""
			"%s" -.> "%s"
			""",  
			i.getSource().getURI(), 
			i.getTarget().getURI())
		).collect(Collectors.toList()));

		return plantUmlImage("set separator none\n"+ packages + imports);
	}

	//----------------------------------------------------------------------------------

	private static <T extends RDFNode> List<T> sortNodes(Collection<T> nodes, Function<RDFNode, String> getLabel) {
		var sorted = new ArrayList<>(nodes);
		sorted.sort((x1, x2) -> getLabel.apply(x1).compareTo(getLabel.apply(x2)));
		return sorted;
	}

	private static <T extends Resource> List<T> sortResourcesi(List<T> resources, Function<RDFNode, String> getLabel) {
		var filtered = resources.stream().filter(i -> !i.isAnon()).collect(Collectors.toList());
		filtered.sort((x1, x2) -> getLabel.apply(x1).compareTo(getLabel.apply(x2)));
		return filtered;
	}

	private static <T extends Resource> List<T> sortByIri(List<T> resources) {
		return sortResourcesi(resources, i -> ((Resource)i).getURI());
	}

	private static <T extends Resource> List<T> sortByName(List<T> resources) {
		return sortResourcesi(resources, i -> localName((Resource)i));
	}

	private static final Pattern LINK = Pattern.compile("\\{\\{([^\\s\\}]+)\s*(.*?)\\}\\}");
	private String replaceLinks(String contextFilePath, String input, OntModel ontModel) {
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
		    return String.format("<a href=\"%s\">%s</a>", path(contextFilePath, url), text);
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
		return options.outputCaseSensitive ? "" : "_"+Math.abs(s.hashCode());
	}

	private String asString(String contextFilePath, RDFNode node) {
		if (node.isResource()) {
			var resource = node.asResource();
			var iri = resource.getURI();
			var label = abbreviatedIri(resource);
			if (isExternalResource(resource)) {
				return String.format("<a href=\"%s\">%s</a>", iri, label);
			} else {
				var path = path(contextFilePath, iri);
				return String.format("<a href=\"%s\">%s</a>", path, label);
			}
		}
		return replaceLinks(contextFilePath, wrapUntaggedTextWithPreTag(node.toString()), (OntModel)node.getModel());
	}

	private String asLocalName(String contextFilePath, Resource resource) {
		var iri = resource.getURI();
		var label = resource.getLocalName();
		if (isExternalResource(resource)) {
			return String.format("<a href=\"%s\">%s</a>", iri, label);
		} else {
			var path = path(contextFilePath, iri);
			return String.format("<a href=\"%s\" target=\"mainFrame\">%s</a>", path, label);
		}
	}

	private String asString(String contextFilePath, List<? extends RDFNode> nodes) {
		return String.join(" & ", nodes.stream()
				.map(i -> asString(contextFilePath, i))
				.collect(Collectors.toList()));
	}
	
	private boolean isExternalResource(Resource r) {
		var uri = r.getURI();
		var ontModel = (OntModel) r.getModel();
		var ontologies = ontModel.listOntologies().toList();
		for (var ontology : ontologies) {
			var ontologyURI = ontology.getURI();
			if (uri.startsWith(ontologyURI)) {
				return false;
			}
		}
		return true;
	}
	
	private String path(String contextFilePath, String iri) {
		var uri = URI.create(iri);
        var relativePath = uri.getAuthority()+uri.getPath()+(uri.getFragment() == null ? "" : File.separator+uri.getFragment())+hashCode(iri);
        var path = options.outputFolderPath+File.separator+relativePath+".html";
        return getRelativePath(contextFilePath, path);
	}
	
	private static String abbreviatedIri(Resource resource) {
		var localName = localName(resource);
		String prefix = prefix(resource);
		return (prefix != null) ? prefix+":"+localName : resource.getURI();
	}

	private static String prefix(Resource resource) {
		var namespace = namespace(resource);
		String prefix = resource.getModel().getGraph().getPrefixMapping().getNsURIPrefix(namespace);
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

    private static String wrapUntaggedTextWithPreTag(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        StringBuilder processedHtml = new StringBuilder();

        if (document.body().childNodes().size() == 1) {
        	return html;
        }
        
        for (Node node : document.body().childNodes()) {
            if (node instanceof TextNode) {
                String untaggedText = ((TextNode)node).getWholeText();
                if (!untaggedText.isEmpty()) {
                    processedHtml.append("<pre>").append(untaggedText).append("</pre>");
                }
            } else {
                processedHtml.append(node.outerHtml());
            }
        }

        return processedHtml.toString();
    }

    private String getRelativeCSSPath(String contextFilePath) {
    	return getRelativePath(contextFilePath, options.outputFolderPath+File.separator+CSS_MAIN);
    }
    
    private String getRelativePath(String contextFilePath, String targetFilePath) {
    	var path1 = Paths.get(contextFilePath).getParent();
    	var path2 = Paths.get(targetFilePath);
    	return path1.relativize(path2).toString();
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
	 * The converter for URL params
	 */
	public static class URLConverter implements IStringConverter<URL>{
		/**
		 * Creates a new URLConverter object
		 */
		public URLConverter() {}

		@Override
		public URL convert(String value) {
			try {
				return new URL(value);
			} catch (MalformedURLException e) {
				return OwlDocApp.class.getClassLoader().getResource(CSS_DEFAULT);
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