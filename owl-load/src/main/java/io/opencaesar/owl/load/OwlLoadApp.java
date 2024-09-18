/**
 * Copyright 2019 California Institute of Technology ("Caltech").
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
package io.opencaesar.owl.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdfconnection.LibSec;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.util.FileManager;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * Utility for loading an OWL dataset to a Fuseki server
 */
public class OwlLoadApp {

    /**
     * Allowed input file extensions for ontologies.
     */
    public static String[] DEFAULT_EXTENSIONS = { "owl", "ttl" };

    @Parameter(
            names = {"--endpoint-url", "-e"},
            description = "URL (endpointURL) of the dataset in a triple store (Required)",
            required = true,
            order = 1)
    private String endpointURL;

    @Parameter(
            names = {"--query-service", "-q"},
            description = "Short name of the query service (Optional, default='sparql')",
            required = false,
            order = 2)
    private String queryService = "sparql";

    @Parameter(
            names = {"--username", "-u"},
            description = "Env var whose value is a username for authenticating the SPARQL endpoint (Optional)",
            required = false,
            order = 3)
    private String authenticationUsername;

    @Parameter(
            names = {"--password", "-p"},
            description = "Env var whose value is a password for authenticating the SPARQL endpoint (Optional)",
            required = false,
            order = 4)
    private String authenticationPassword;

    @Parameter(
            names = {"--catalog-path", "-c"},
            description = "Path to the OWL XML catalog file (Required)",
            validateWith = CatalogPath.class,
            required = true,
            order = 5)
    private String catalogPath;

    @Parameter(
            names = {"--iri", "-i"},
            description = "Root IRIs to load (Required if 'irisPath' is not set)",
            required = false,
            order = 6)
    private List<String> iris = new ArrayList<>();

    @Parameter(
            names = {"--iris-path", "-ip"},
            description = "Path to a txt file with the ontology IRIs to load (one per line) (Required if 'iris' is not set)",
            required = false,
            order = 7)
    private String irisPath;

    @Parameter(
            names = {"--file-extensions", "-f"},
            description = "File extensions of files that will be uploaded. Default is owl and ttl, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss (Optional)",
            validateWith = FileExtensionValidator.class,
            order = 8)
    private List<String> fileExtensions = new ArrayList<>();
    {
        fileExtensions.addAll(Arrays.asList(DEFAULT_EXTENSIONS));
    }

    @Parameter(
            names = {"--default", "-df"},
            description = "Load data into the default graph.",
            required = false,
            order = 9)
    private boolean loadToDefaultGraph;

    @Parameter(
            names = {"-d", "--debug"},
            description = "Shows debug logging statements",
            order = 10)
    private boolean debug;

    @Parameter(
            names = {"--help", "-h"},
            description = "Displays summary of options",
            help = true,
            order = 11)
    private boolean help;

    private final static Logger LOGGER = Logger.getLogger(OwlLoadApp.class);

    static {
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
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
        final OwlLoadApp app = new OwlLoadApp();

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
        if (app.iris.isEmpty() && app.irisPath == null) {
            throw new RuntimeException("Iris are not set");
        }
        app.run(deltas);
    }

    /**
     * Creates a new OwlLoadApp object
     */
    public OwlLoadApp() {
    }

    private void run(Collection<File> deltas) throws Exception {
        LOGGER.info("=================================================================");
        LOGGER.info("                        S T A R T");
        LOGGER.info("                     OWL Load " + getAppVersion());
        LOGGER.info("=================================================================");
        LOGGER.info(("Catalog path = " + catalogPath));
        LOGGER.info(("Endpoint URL = " + endpointURL));
        LOGGER.info(("File Extensions = " + fileExtensions));
        LOGGER.info(("IRIs = " + iris));
        LOGGER.info(("IRIs Path = " + irisPath));

        // Create Owl Catalog
        OwlCatalog catalog = OwlCatalog.create(new File(catalogPath), fileExtensions);

        // Get dataset Iris
        var dataset_iris = getDatasetIris(catalog);
        LOGGER.info("found " + dataset_iris.size() + " dataset iris");

        // Get Changed Iris
        var changed_iris = deltas != null ? getMappedIris(deltas, catalog) : dataset_iris;
        LOGGER.info("found " + changed_iris.size() + " changed iris "
                + ((deltas != null) ? "from mapping " + deltas.size() + " deltas" : "from dataset iris"));

        changed_iris.forEach(iri -> LOGGER.debug("Changed " + iri));
        
        // Get an RDF Connection
        RDFConnection conn = getRDFConnection();

        try {
	        // Load the dataset
	        if (loadToDefaultGraph) {
	            boolean load_everything = false;
	            var default_graph_size = getDefaultGraphSize(conn);
	            if (default_graph_size == 0) {
	                // load everything if there is nothing on the server.
	                load_everything = true;
	            } else if (default_graph_size > 0 && !changed_iris.isEmpty()) {
	                // load everything when there is something on the server and there are changes.
	                load_everything = true;
	            } else {
	            	// no need to reload any file
	            }
	
	            if (load_everything) {
	                // in incremental mode: one graphs have either been deleted, modified, or added.
	                // in batch mode: changed_iris = dataset_iris.
	                removeAllFromDefault(conn);
	                // load everything
	                dataset_iris.parallelStream().forEach(iri -> loadToDefault(conn, catalog, iri));
	                System.out.println("Loaded "+dataset_iris.size()+" owl file(s) to default graph");
	            } else {
	                System.out.println("Loaded no owl files to default graph");
	            }
	        } else {
	            // Get Loaded Iris
	            var loaded_iris = getLoadedIris(conn);
	            LOGGER.info("found " + loaded_iris.size() + " loaded iris");
	
	            List<String> to_load_iris = new ArrayList<>();  
	            
	            dataset_iris.stream().forEach(iri -> {
	                if (!loaded_iris.contains(iri)) {
	                	to_load_iris.add(iri);
	                } else if (changed_iris.contains(iri)) {
	                	to_load_iris.add(iri);
	                    loaded_iris.remove(iri);
	                } else {
	                    loaded_iris.remove(iri);
	                }
	            });
	            
	            to_load_iris.parallelStream().forEach(iri -> put(conn, catalog, iri));
	            loaded_iris.parallelStream().forEach(iri -> delete(conn, iri));
	            System.out.println("Loaded "+to_load_iris.size()+" owl file(s), unloaded "+loaded_iris.size()+" owl file(s)");
	        }
        } catch (HttpException e) {
        	if (e.getCause() instanceof ConnectException) {
        		LOGGER.error("Connection Exception: check that the endpoint ("+endpointURL+") is reachable");
        	}
    		throw e;
        } finally {
	        // Close connection
	        conn.close();
	        conn.end();
        }

        LOGGER.info("=================================================================");
        LOGGER.info("                          E N D");
        LOGGER.info("=================================================================");
    }

    private RDFConnection getRDFConnection() {
        RDFConnectionRemoteBuilder builder = RDFConnectionRemote.create()
                .queryEndpoint(queryService)
                .destination(endpointURL);

        final String username = authenticationUsername != null ? System.getenv(authenticationUsername) : null;
        final String password = authenticationPassword != null ? System.getenv(authenticationPassword) : null;

        if (null != username && null != password) {
            Authenticator authenticator = LibSec.authenticator(username, password);
            HttpClient client = HttpEnv.httpClientBuilder()
                    .authenticator(authenticator)
                    .build();
            builder = builder.httpClient(client);
        }

        return builder.build();
    }

    private Collection<String> getMappedIris(Collection<File> files, OwlCatalog catalog) {
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

    private Collection<String> getLoadedIris(RDFConnection conn) {
        var iris = new HashSet<String>();
        try {
	        var rs = conn.query("select ?g { graph ?g { ?o a <http://www.w3.org/2002/07/owl#Ontology> } }").execSelect();
	        rs.forEachRemaining(s -> iris.add(s.getResource("g").getURI()));
        } catch(Exception e) {
       		LOGGER.error("Error accessing endpoint "+endpointURL+"/"+queryService+": "+e.getMessage());
    		throw e;
        }
        return iris;
    }

    private int getDefaultGraphSize(RDFConnection conn) {
        var rs = conn.query("select (count(*) as ?count) {?s ?p ?o}").execSelect();
        // If there are results, retrieve the 'count' literal and return its integer
        // value
        if (rs.hasNext()) {
            var sol = rs.next();
            var count = sol.getLiteral("count").getInt();
            LOGGER.info("default graph has "+count+" triples");
            return count;
        } else {
        	LOGGER.info("getDefaultGraphSize - no results!");
            return 0;
        }
    }

    private Set<String> getDatasetIris(OwlCatalog catalog) throws Exception {
        if (!iris.isEmpty()) {
            return getIrisFromRoots(catalog);
        } else if (irisPath != null) {
            return getIrisFromPath();
        }
        return Collections.emptySet();
    }

    private Set<String> getIrisFromRoots(OwlCatalog catalog) throws Exception {
        OntDocumentManager mgr = new OntDocumentManager();
        Map<String, URI> fileMap = catalog.getFileUriMap();
        for (var entry : fileMap.entrySet()) {
            mgr.addAltEntry(entry.getKey(), entry.getValue().toString());
        }

        OntModelSpec s = new OntModelSpec(OntModelSpec.OWL_MEM);
        s.setDocumentManager(mgr);
        OntModel ontModel = ModelFactory.createOntologyModel(s);
        FileManager fileMgr = mgr.getFileManager();
        for (var iri : iris) {
        	try {
        		fileMgr.readModelInternal(ontModel, iri);
        	} catch (Exception e) {
        		if (e instanceof HttpException && ((HttpException) e).getStatusCode() == 404) {
            		LOGGER.error("Could not resolve iri <"+iri+"> using the catalog");
        		} else {
        			LOGGER.error("Error reading ontology with iri <"+iri+">");
        		}
        		throw e;
        	}
        }

        Set<String> allIris = new HashSet<>();
        ontModel.listOntologies().forEach(o -> allIris.add(o.getURI()));
        return allIris;
    }

    private Set<String> getIrisFromPath() throws Exception {
        Set<String> iris = new HashSet<>();

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

    private void loadToDefault(RDFConnection conn, OwlCatalog catalog, String iri) {
        LOGGER.info("Loading "+iri);
        String documentURI = catalog.resolveURI(iri);
        String documentFile = new File(URI.create(documentURI)).toString();
        try {
            Lang lang = RDFLanguages.filenameToLang(documentFile);
            if (RDFLanguages.isQuads(lang)) {
                Dataset ds = RDFDataMgr.loadDataset(documentFile.toString());
                Model model = ds.getNamedModel(iri);
                conn.load(model);
            } else {
                conn.load(documentFile);
            }
            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException("Error loading '" + documentURI + "' to default graph: "+e.getMessage(), e);
        }
    }

    private void removeAllFromDefault(RDFConnection conn) {
        LOGGER.info("Clearing default graph");
        try {
            conn.delete();
            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException("Error clearing default graph", e);
        }
    }

    private void put(RDFConnection conn, OwlCatalog catalog, String iri) {
        LOGGER.info("Loading " + iri);
        String documentURI = catalog.resolveURI(iri);
        String documentFile = new File(URI.create(documentURI)).toString();
        try {
            Lang lang = RDFLanguages.filenameToLang(documentFile);
            if (RDFLanguages.isQuads(lang)) {
                conn.putDataset(documentFile);
            } else {
                conn.put(iri, documentFile);
            }
            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException("Error loading '" + documentURI + "' to named graph: "+e.getMessage(), e);
        }
    }

    private void delete(RDFConnection conn, String iri) {
        LOGGER.info("Unloading " + iri);
        try {
            conn.delete(iri);
            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException("Error unloading graph '" + iri + "': "+e.getMessage(), e);
        }
    }

    private String getAppVersion() {
        var version = this.getClass().getPackage().getImplementationVersion();
        return (version != null) ? version : "<SNAPSHOT>";
    }

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
            if (!file.exists() || !file.getName().endsWith("catalog.xml")) {
                throw new ParameterException("Parameter " + name + " should be a valid OWL catalog path");
            }
        }
    }

}
