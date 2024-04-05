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
package io.opencaesar.owl.save;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.LibSec;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
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
 * Utility for saving an OWL dataset from a Fuseki server 
 */
public class OwlSaveApp {

    /**
     * Allowed input file extensions for ontologies.
     */
    private final static String DEFAULT_EXTENSION = "ttl";
    
    @Parameter(
            names = {"--endpoint-url", "-e"},
            description = "URL (endpointURL) of the dataset in a triple store (Required)",
            required = true,
            order = 1)
    private String endpointURL;

    @Parameter(
            names = {"--username", "-u"},
            description = "Env var whose value is a username for authenticating the SPARQL endpoint (Optional)",
            required = false,
            order = 2)
    private String authenticationUsername;

    @Parameter(
            names = {"--password", "-p"},
            description = "Env var whose value is a password for authenticating the SPARQL endpoint (Optional)",
            required = false,
            order = 3)
    private String authenticationPassword;

    @Parameter(
            names = {"--catalog-path", "-c"},
            description = "Path to the OWL XML catalog file (Required)",
            validateWith = CatalogPath.class,
            required = true,
            order = 4)
    private String catalogPath;

    @Parameter(
            names = {"--file-extension", "-f"},
            description = "File extension of the saved files. Default is ttl, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld (Optional)",
            validateWith = FileExtensionValidator.class,
            required = false,
            order = 7)
    private String fileExtension = DEFAULT_EXTENSION;

    @Parameter(
            names = {"-d", "--debug"},
            description = "Shows debug logging statements",
            order = 9)
    private boolean debug;

    @Parameter(
            names = {"--help", "-h"},
            description = "Displays summary of options",
            help = true,
            order = 10)
    private boolean help;

    private final static Logger LOGGER = Logger.getLogger(OwlSaveApp.class);

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
        mainWithDeltas(args);
    }

    /**
     * Application for loading ontologies to a Fuseki server.
     * 
     * @param args   Application arguments.
     * @throws Exception Error
     */
    public static void mainWithDeltas(final String... args) throws Exception {
        final OwlSaveApp app = new OwlSaveApp();

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
        app.run();
    }

    /**
     * Creates a new OwlSaveApp object
     */
    public OwlSaveApp() {
    }

    private void run() throws Exception {
        LOGGER.info("=================================================================");
        LOGGER.info("                        S T A R T");
        LOGGER.info("                     OWL Save " + getAppVersion());
        LOGGER.info("=================================================================");
        LOGGER.info(("Catalog path = " + catalogPath));
        LOGGER.info(("Endpoint URL = " + endpointURL));
        LOGGER.info(("File Extension = " + fileExtension));

        // Get an RDF Connection
        RDFConnection conn = getRDFConnection();

		// Load Owl Catalog
		final File catalogFile = new File(catalogPath);
		if (!catalogFile.exists() ) {
			createOutputCatalog(catalogFile);
		}
        OwlCatalog catalog = OwlCatalog.create(new File(catalogPath));

        try {
            // Fetches the dataset
        	LOGGER.info("Fetching dataset at "+endpointURL);
            Dataset dataset = conn.fetchDataset();
            
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
       } catch (HttpException e) {
        	if (e.getCause() instanceof ConnectException) {
        		System.out.println("Connection Exception: check that the endpoint ("+endpointURL+") is reachable");
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

	private String getAppVersion() {
        var version = this.getClass().getPackage().getImplementationVersion();
        return (version != null) ? version : "<SNAPSHOT>";
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
			final File folder = file.getParentFile();
			folder.mkdirs();
        }
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

}
