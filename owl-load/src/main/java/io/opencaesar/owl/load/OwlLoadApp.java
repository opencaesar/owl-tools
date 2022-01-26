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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.jena.query.Dataset;
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
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class OwlLoadApp {

    @Parameter(
            names = {"--iri", "-i"},
            description = "IRIs to load (Required)",
            required = true,
            order = 1)
    private List<String> iris = new ArrayList<>();

    @Parameter(
            names = {"--catalog-path", "-c"},
            description = "Path to the OWL XML catalog file (Required)",
            validateWith = CatalogPath.class,
            required = true,
            order = 2)
    private String catalogPath;

    @Parameter(
            names = {"--endpoint-url", "-e"},
            description = "URL (endpointURL) of the dataset in a triple store (Required)",
            required = true,
            order = 3)
    private String endpointURL;

    @Parameter(
            names = {"--file-extensions", "-f"},
            description = "File extensions of files that will be uploaded. Default is owl and ttl, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss (Optional)",
        	validateWith = FileExtensionValidator.class,
            order = 4)
    private List<String> fileExtensions = new ArrayList<>();
    {
        fileExtensions.add("owl");
        fileExtensions.add("ttl");
    }

    @Parameter(
            names = {"-d", "--debug"},
            description = "Shows debug logging statements",
            order = 5)
    private boolean debug;

    @Parameter(
            names = {"--help", "-h"},
            description = "Displays summary of options",
            help = true,
            order = 6)
    private boolean help;

    private final static Logger LOGGER = Logger.getLogger(OwlLoadApp.class);

    static {
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
    }

    public static void main(final String... args) throws Exception {
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
        app.run();
    }

    private void run() throws Exception {
        LOGGER.info("=================================================================");
        LOGGER.info("                        S T A R T");
        LOGGER.info("                     OWL Load " + getAppVersion());
        LOGGER.info("=================================================================");
        LOGGER.info(("Catalog path = " + catalogPath));
        LOGGER.info(("Endpoint URL = " + endpointURL));
        LOGGER.info(("File Extensions = " + fileExtensions));
        LOGGER.info(("IRIs = " + iris));

        // Delete all existing models of the dataset before loading anything.
        RDFConnectionRemoteBuilder builder = RDFConnectionRemote.create()
                .updateEndpoint("update")
                .queryEndpoint("sparql")
                .destination(endpointURL);
        RDFConnection conn = builder.build();
        try {
            Dataset ds = conn.fetchDataset();
            List<String> names = new ArrayList<>();
            ds.listNames().forEachRemaining(names::add);
            names.forEach(conn::delete);
            conn.commit();
        } finally {
            conn.close();
            conn.end();
        }

        LOGGER.info("create ontology manager");
        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        if (manager == null) {
            throw new RuntimeException("couldn't create owl ontology manager");
        }
        LOGGER.debug("add location mappers");
        XMLCatalogIRIMapper mapper = new XMLCatalogIRIMapper(new File(catalogPath), fileExtensions);
        manager.getIRIMappers().add(mapper);

        iris.forEach(iri -> {
            LOGGER.info("load ontology " + iri);
            try {
                final OWLOntology ont = manager.loadOntology(IRI.create(iri));
                if (ont == null) {
                    throw new RuntimeException("couldn't load ontology at IRI=" + iri);
                }
            } catch (OWLOntologyCreationException e) {
            	throw new RuntimeException(e);
            }
        });

        final Set<OWLOntology> allOntologies = manager.ontologies().flatMap(manager::importsClosure).collect(Collectors.toUnmodifiableSet());
        LOGGER.info("Loading "+allOntologies.size()+" ontologies...");

        // Creates a work-stealing thread pool using all available processors as its target parallelism level.
        // For debugging, use Executors.newSingleThreadExecutor();
        final ExecutorService pool = Executors.newWorkStealingPool();
        CompletableFuture<Void> allLoaded = CompletableFuture.allOf(allOntologies.stream().map(ont -> loadOntology(ont, pool)).toArray(CompletableFuture[]::new));

        allLoaded.get();
        LOGGER.info("All ontologies loaded.");

        shutdownAndAwaitTermination(pool);

        LOGGER.info("=================================================================");
        LOGGER.info("                          E N D");
        LOGGER.info("=================================================================");
    }

    /**
     * @param pool An ExecutionService
     * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ExecutorService.html">ExecutorService Usage</a>
     */
    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private CompletableFuture<Void> loadOntology(final OWLOntology ont, final ExecutorService pool) {
        return CompletableFuture.runAsync(() -> {
            //Create remote connection to Fuseki server
            RDFConnectionRemoteBuilder builder = RDFConnectionRemote.create()
                    .updateEndpoint("update")
                    .queryEndpoint("sparql")
                    .destination(endpointURL);
            RDFConnection conn = builder.build();
            IRI documentIRI = ont.getOWLOntologyManager().getOntologyDocumentIRI(ont);
            //noinspection CommentedOutCode
            try {
                String documentFile = documentIRI.toURI().toURL().getFile();
                // With Jena 4.3.+, it is necessary to remove the ':' character otherwise there is an illegal path exception.
                // Unfortunately, this is still not sufficient as we get an exception like this:
                //         ... 95 more
                //Caused by: java.lang.RuntimeException: Error occurred loading ontology 'file:/C:/opt/local/github.opencaesar/metrology-vocabularies/build/owl/iso.org/iso-80000-4.3.owl'
                //        at io.opencaesar.owl.load.OwlLoadApp.lambda$loadOntology$3(OwlLoadApp.java:243)
                //Caused by: org.apache.jena.atlas.web.HttpException: 400 - Bad Request
                //        at org.apache.jena.http.HttpLib.exception(HttpLib.java:231)
                //        at org.apache.jena.http.HttpLib.handleHttpStatusCode(HttpLib.java:161)
                //        at org.apache.jena.http.HttpLib.handleResponseNoBody(HttpLib.java:199)
                //        at org.apache.jena.http.HttpLib.httpPushData(HttpLib.java:578)
                //        at org.apache.jena.sparql.exec.http.GSP.pushFile(GSP.java:605)
                //        at org.apache.jena.sparql.exec.http.GSP.uploadTriples(GSP.java:546)
                //        at org.apache.jena.sparql.exec.http.GSP.POST(GSP.java:309)
                //        at org.apache.jena.rdflink.RDFLinkHTTP.load(RDFLinkHTTP.java:407)
                //        at org.apache.jena.rdflink.RDFConnectionAdapter.load(RDFConnectionAdapter.java:146)

//                LOGGER.info("orig. documentFile = "+documentFile);
//                if (documentFile.startsWith("/C:")) {
//                    documentFile = documentFile.replace("/C:", "//C");
//                    LOGGER.info("fixed documentFile = "+documentFile);
//                } else if (documentFile.startsWith("/D:")) {
//                    documentFile = documentFile.replace("/D:", "//D");
//                    LOGGER.info("fixed documentFile = "+documentFile);
//                }
                Optional<IRI> defaultDocumentIRI = ont.getOntologyID().getDefaultDocumentIRI();
                assert(defaultDocumentIRI.isPresent());
                String graphName = defaultDocumentIRI.get().getIRIString();
                Lang lang = RDFLanguages.filenameToLang(documentFile);
                if (RDFLanguages.isQuads(lang)) {
                    LOGGER.info("Load dataset = "+documentFile);
                    conn.loadDataset(documentFile);
                } else {
                    LOGGER.info("Load file    = "+documentFile);
                    conn.load(graphName, documentFile);
                }
                conn.commit();
           } catch (Exception e) {
				throw new RuntimeException("Error occurred loading ontology '"+documentIRI+"'", e);
           } finally {
                conn.end();
                conn.close();
            }
        }, pool);
    }

    private String getAppVersion() {
    	var version = this.getClass().getPackage().getImplementationVersion();
    	return (version != null) ? version : "<SNAPSHOT>";
    }

	public static class FileExtensionValidator implements IParameterValidator {
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			Lang lang = RDFLanguages.fileExtToLang(value);
			if (lang == null) {
				throw new ParameterException("File extension " + name + " is not a valid one");
			}
		}
		
	}

	public static class CatalogPath implements IParameterValidator {
        @Override
        public void validate(final String name, final String value) throws ParameterException {
            File file = new File(value);
            if (!file.exists()) {
                throw new ParameterException("Catalog not found, please give a valid catalog. Does not exist at: " + value);
            }
            if (!file.getName().endsWith("catalog.xml")) {
                throw new ParameterException("Parameter " + name + " should be a valid OWL catalog path");
            }
        }
    }

}