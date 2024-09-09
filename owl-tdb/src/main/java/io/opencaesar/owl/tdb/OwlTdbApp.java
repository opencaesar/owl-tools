/**
 * Copyright 2024 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opencaesar.owl.tdb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.TDBLoader;
import org.apache.jena.tdb.base.file.Location;
import org.apache.jena.tdb.store.GraphTDB;
import org.apache.jena.tdb.sys.TDBInternal;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * Utility for loading and saving OWL files to and from a TDB dataset
 */
public class OwlTdbApp {

	/**
	 * Allowed input file extensions for ontologies.
	 */
	public static String[] DEFAULT_EXTENSIONS = {"ttl", "owl" };

	enum Command {
		load,
		save
	}

	@Parameter(
			names = {"--command", "-cm"},
			description = "An enumerated command: load or save (Required)",
			converter = CommandConverter.class,
			required = true,
			order = 1)
	private Command command;

	@Parameter(
			names = {"--dataset-path", "-ds"},
			description = "Path to a folder representing the TDB dataset (Required)",
			required = true)
	private String datasetPath;

	@Parameter(
			names = {"--catalog-path", "-c"},
			description = "Path to the OWL XML catalog file (Required)",
			validateWith = CatalogPath.class,
			required = true)
	private String catalogPath;

	@Parameter(
			names = {"--file-extension", "-e"},
			description = "Extensions of the OWL files. Default is owl and ttl, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss (Optional)",
			validateWith = FileExtensionValidator.class)
	private List<String> fileExtensions = new ArrayList<>();
	{
		fileExtensions.addAll(Arrays.asList(DEFAULT_EXTENSIONS));
	}

	@Parameter(
			names = {"--iri", "-i"},
			description = "IRIs to load (Optional)",
			required = false)
	private List<String> iris = new ArrayList<>();

	@Parameter(
			names = {"--iris-path", "-p"},
			description = "Path to a txt file with the ontology IRIs to load (one per line) (Optional)",
			required = false)
	private String irisPath;

	@Parameter(
			names = {"--named-graph", "-ng"},
			description = "Load data into named graphs.",
			required = false,
			arity = 1)
	private boolean loadToNamedGraphs = true;

	@Parameter(
			names = {"--default-graph", "-dg"},
			description = "Load data into the default graph.",
			required = false,
			arity = 1)
	private boolean loadToDefaultGraph = true;

	@Parameter(
			names = {"-d", "--debug"},
			description = "Shows debug logging statements")
	private boolean debug;

	@Parameter(
			names = {"--help", "-h"},
			description = "Displays summary of options",
			help = true)
	private boolean help;

	private final static Logger LOGGER = Logger.getLogger(OwlTdbApp.class);

	static {
		DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
	}

    private final static Map<String, String> languages = new HashMap<>();
    
    static {
    	languages.put("owl", RDFLanguages.RDFXML.getName());
    	languages.put("rdf", RDFLanguages.RDFXML.getName());
    	languages.put("xml", RDFLanguages.RDFXML.getName());
    	languages.put("rj", RDFLanguages.RDFJSON.getName());
    	languages.put("ttl", RDFLanguages.TTL.getName());
    	languages.put("n3", RDFLanguages.N3.getName());
    	languages.put("nt", RDFLanguages.NT.getName());
    	languages.put("trig", RDFLanguages.TRIG.getName());
    	languages.put("nq", RDFLanguages.NQ.getName());
    	languages.put("trix", RDFLanguages.TRIX.getName());
    	languages.put("jsonld", RDFLanguages.JSONLD.getName());
    }

    /**
	 * Application for loading ontologies to a Fuseki server.
	 * 
	 * @param args Application arguments.
	 * @throws Exception Error
	 */
	public static void main(final String... args) throws Exception {
		mainWithDeltas(null, args);
	}

	/**
	 * Application for loading ontologies to a Fuseki server.
	 * 
	 * @param deltas The set of changed files
	 * @param args   Application arguments.
	 * @throws Exception Error
	 */
	public static void mainWithDeltas(Collection<File> deltas, final String... args) throws Exception {
		final OwlTdbApp app = new OwlTdbApp();

		final JCommander builder = JCommander.newBuilder().addObject(app).build();
		builder.parse(args);
		if (app.help) {
			builder.usage();
			return;
		}
		if (app.debug) {
			final Appender appender = LogManager.getRootLogger().getAppender("stdout");
			((AppenderSkeleton) appender).setThreshold(Level.DEBUG);
		}
		if (app.command == Command.load) {
			app.load(deltas);
		} else if (app.command == Command.save) {
			app.save();
		}
	}

	/**
	 * Creates a new OwlTdbApp object
	 */
	public OwlTdbApp() {
	}

	private void load(Collection<File> deltas) throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("						S T A R T");
		LOGGER.info("					 OWL TDB Load " + getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info(("Dataset path = " + datasetPath));
		LOGGER.info(("Catalog path = " + catalogPath));
		LOGGER.info(("File Extensions = " + fileExtensions));
		LOGGER.info(("IRIs = " + iris));
		LOGGER.info(("IRIs Path = " + irisPath));
		LOGGER.info(("Load to Default Graph = " + loadToDefaultGraph));
		LOGGER.info(("Load to Named Graphs = " + loadToNamedGraphs));

		if (deltas == null || deltas.size() > 0) {
			// Create Owl Catalog
			OwlCatalog catalog = OwlCatalog.create(new File(catalogPath), fileExtensions);
	
			// Get relevant Iris
			var relevant_iris = getRelevantIris(catalog); 
			LOGGER.info("found " + relevant_iris.size() + " relevant iris");
	
			// Get Changed Iris
			var changed_iris = (deltas != null) ? getChangedIris(deltas, catalog) : relevant_iris;
			LOGGER.info("found " + changed_iris.size() + " changed iris "
					+ ((deltas != null) ? "from mapping " + deltas.size() + " deltas" : "from relevant iris"));
			
			// Create the dataset
			Dataset dataset = null;
	
			// Load the dataset
			try {
				dataset = createDataset();
				dataset.begin(ReadWrite.WRITE);

				if (loadToNamedGraphs) {
					// Get Loaded Iris
					var loaded_iris = getLoadedNamedGraphs(dataset);
					LOGGER.info("found " + loaded_iris.size() + " loaded iris");
		
					List<String> to_load_iris = new ArrayList<>();  
					
					relevant_iris.stream().forEach(iri -> {
						if (!loaded_iris.contains(iri)) {
							to_load_iris.add(iri);
						} else if (changed_iris.contains(iri)) {
							to_load_iris.add(iri);
							loaded_iris.remove(iri);
						} else {
							loaded_iris.remove(iri);
						}
					});
					
					for (var iri : to_load_iris) {
						loadNamedGraph(dataset, catalog, iri);
					}
					for (var iri : loaded_iris) {
						removeNamedGraph(dataset, iri);
					}
					System.out.println("Loaded "+to_load_iris.size()+" owl file(s), unloaded "+loaded_iris.size()+" owl file(s)");
					
					if (loadToDefaultGraph) {
						loadToDefaultGraph(dataset);
					}
				} else if (loadToDefaultGraph) {
					if (!changed_iris.isEmpty()) {
						loadToDefaultGraph(dataset, catalog, relevant_iris);
						System.out.println("Loaded "+relevant_iris.size()+" owl file(s) to default graph");
					} else {
						System.out.println("Loaded no owl files to default graph");
					}
				} 
			} catch (Exception e) {
				LOGGER.error(e);
			} finally {
				if (dataset != null) {
					dataset.commit();
					dataset.end();
					dataset.close();
					TDBFactory.release(dataset);
				}
				LOGGER.info("closing the dataset");
			}
		}
		
		LOGGER.info("=================================================================");
		LOGGER.info("						  E N D");
		LOGGER.info("=================================================================");
	}

	private void save() throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("						S T A R T");
		LOGGER.info("					 OWL TDB Save " + getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info(("Dataset path = " + datasetPath));
		LOGGER.info(("Catalog path = " + catalogPath));
		LOGGER.info(("File Extension = " + fileExtensions));

		// Load Owl Catalog
		final File catalogFile = new File(catalogPath);
		if (!catalogFile.exists() ) {
			createOutputCatalog(catalogFile);
		}
        // Create Owl Catalog
		OwlCatalog catalog = OwlCatalog.create(new File(catalogPath), fileExtensions);
		
		// get the output file extension
		var fileExtension = fileExtensions.get(0);
		
		// Create the dataset
		Dataset dataset = null;

		// Load the dataset
		try {
			dataset = createDataset();
			dataset.begin(ReadWrite.READ);
			
            // save the ontologies
            int count = 0;
            Iterator<String> resources = dataset.listNames();
            while (resources.hasNext()) {
            	count++;
            	String uri = resources.next();
            	String resolved = catalog.resolveURI(uri);
            	
                File dst = new File(new URI(resolved+"."+fileExtension));
                dst.getParentFile().mkdirs();
                dst.createNewFile();
            	OutputStream out = new FileOutputStream(dst, false);
            	LOGGER.info("Saving "+dst.getCanonicalPath());
            	
            	Model model = dataset.getNamedModel(uri);
            	model.write(out, languages.get(fileExtension));
            }
            System.out.println("Saved "+count+" owl file(s)");
		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			if (dataset != null) {
				dataset.end();
				dataset.close();
				TDBFactory.release(dataset);
			}
			LOGGER.info("closing the dataset");
		}

		LOGGER.info("=================================================================");
		LOGGER.info("						  E N D");
		LOGGER.info("=================================================================");
	}

	private String getAppVersion() {
		var version = this.getClass().getPackage().getImplementationVersion();
		return (version != null) ? version : "<SNAPSHOT>";
	}

	private List<String> getRelevantIris(OwlCatalog catalog) throws Exception {
		var allIris = new ArrayList<String>(iris);
		if (irisPath != null) {
			allIris.addAll(getIrisFromPath());
		}
		if (allIris.isEmpty()) { // if no specific iris, load all catalog iris
			allIris.addAll(catalog.getFileUriMap().keySet());
		}
		return allIris;
	}

	private List<String> getIrisFromPath() throws Exception {
		List<String> iris = new ArrayList<>();

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(irisPath)));
			String iri = reader.readLine();
			while (iri != null) {
				iris.add(iri);
				iri = reader.readLine();
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}

		return iris;
	}

	private Collection<String> getChangedIris(Collection<File> files, OwlCatalog catalog) {
		var iris = new HashSet<String>();
		files.forEach(f -> {
			if (f.isFile()) {
				String iri = catalog.deresolveURI(f.getAbsolutePath());
				if (iri != null) {
					iris.add(iri);
				}
			}
		});
		return iris;
	}

	private void createOutputCatalog(final File outputCatalogFile) throws Exception {
		LOGGER.info(("Saving: " + outputCatalogFile));
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputCatalogFile));
        bw.write("<?xml version='1.0'?>\n");
        bw.write("<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" prefer=\"public\">\n");
       	bw.write("\t<rewriteURI uriStartString=\"http://\" rewritePrefix=\"./\" />\n");
       	bw.write("\t<rewriteURI uriStartString=\"https://\" rewritePrefix=\"./\" />\n");
        bw.write("</catalog>");
        bw.close();
	}

	//---------------

	private Dataset createDataset() {
		LOGGER.info("Creating dataset");
		var location = Location.create(datasetPath);
		int pid = location.getLock().getOwner();
		if (pid != 0) {
			var process = ProcessHandle.allProcesses().filter(p -> p.pid() == pid).findFirst();
			if (process.isPresent()) {
				if (!process.get().destroyForcibly()) {
					throw new IllegalArgumentException("Failed to kill a Fuseki server process with pid=" + pid);
				} else {
					LOGGER.warn("Fuseki server with pid=" + pid + " has been stopped");
				}
			}
		}
		return TDBFactory.createDataset(location);
	}
  
	private void loadToDefaultGraph(Dataset dataset) {
		LOGGER.info("loading to default graph");
		dataset.setDefaultModel(dataset.getUnionModel());
	}
	
	private void loadToDefaultGraph(Dataset dataset, OwlCatalog catalog, List<String> iris) {
		LOGGER.info("Loading to default graph");
		List<String> uris = new ArrayList<>();
		iris.forEach(iri -> {
			try {
				uris.add(new File(new URI(catalog.resolveURI(iri))).toString());
			} catch (URISyntaxException e) {
				LOGGER.error(e);
			}
		});
		
		var graph = TDBInternal.getBaseDatasetGraphTDB(dataset.asDatasetGraph());
		graph.clear();
		TDBLoader.load(graph, uris, true, true);
	}

	private List<String> getLoadedNamedGraphs(Dataset dataset) {
		var coll = new ArrayList<String>();
		var i = dataset.listNames();
		while (i.hasNext()) {
			coll.add(i.next());
		}
		return coll;
	}

	private void loadNamedGraph(Dataset dataset, OwlCatalog catalog, String iri) {
		LOGGER.info("Loading to " + iri);
		try {
			var uri = new File(new URI(catalog.resolveURI(iri))).toString();
			Model model = dataset.getNamedModel(iri);
			GraphTDB graph = (GraphTDB) model.getGraph();
			graph.clear();
			
			TDBLoader.load(graph, uri, true);
		} catch (URISyntaxException e) {
			LOGGER.error(e);
		}
   }

	private void removeNamedGraph(Dataset dataset, String iri) {
		LOGGER.info("Removing " + iri);
		dataset.removeNamedModel(iri);
	}

	//---------------

	/**
	 * A parameter validator for a file extension for an RDF language syntax.
	 */
	public static class FileExtensionValidator implements IParameterValidator {
		/**
		 * Creates a new FileExtensionValidator object
		 */
		public FileExtensionValidator() {
		}

		@Override
		public void validate(final String name, final String value) throws ParameterException {
			Lang lang = RDFLanguages.fileExtToLang(value);
			if (lang == null) {
				throw new ParameterException("File extension " + name + " is not a valid one");
			}
		}

	}

	/**
	 * A parameter validator for an OASIS XML catalog path.
	 */
	public static class CatalogPath implements IParameterValidator {
		/**
		 * Creates a new FileExtensionValidator object
		 */
		public CatalogPath() {
		}

		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File file = new File(value);
			if (!file.getName().endsWith("catalog.xml")) {
				throw new ParameterException("Parameter " + name + " should be a valid OWL catalog path");
			}
		}
	}

	/**
	 * A parameter converter for the command enumeration (start/stop).
	 */
	public static class CommandConverter implements IStringConverter<Command> {

		/**
		 * Creates a new CommandConverter object
		 */
		public CommandConverter() {
		}
		
		@Override
		public Command convert(String value) {
			try {
				return Command.valueOf(value);
			} catch (IllegalArgumentException e) {
				throw new ParameterException("Value " + value + " is not valid (only: load or save)");
			}
		}

	}

}
