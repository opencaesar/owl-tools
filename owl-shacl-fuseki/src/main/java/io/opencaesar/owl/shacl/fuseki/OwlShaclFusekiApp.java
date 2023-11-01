/*
  Copyright 2019 California Institute of Technology ("Caltech").
  U.S. Government sponsorship acknowledged.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package io.opencaesar.owl.shacl.fuseki;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.jena.query.QueryException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
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
import com.github.jsonldjava.shaded.com.google.common.base.Suppliers;

/**
 * Utility for querying a Fuseki server using SHACL queries.
 */
public class OwlShaclFusekiApp {

	@Parameter(
		names = { "--endpoint-url", "-e" },
		description = "Sparql Endpoint URL (Required)",
		required = true, 
		validateWith = URIValidator.class, 
		order = 1)
	private String endpointURL;

    @Parameter(
        names = {"--shacl-service", "-s"},
        description = "Short name of the shacl service (Optional, default='shacl')",
        required = false,
        order = 2)
    private String shaclService = "shacl";
    
	@Parameter(
		names = { "--query-path", "-q" },
		description = "Path to a .shacl query file or directory (Required)",
		validateWith = QueryPath.class,
		required = true,
		order = 3)
	private String queryPath;

	@Parameter(
		names = { "--result-path", "-r" },
		description = "Path to the folder to save the result to (Required)",
		validateWith = ResultFolderPath.class,
		required = true,
		order = 4)
	private String resultPath;

	@Parameter(
		names = { "--debug", "-d" },
		description = "Shows debug logging statements",
		order = 5)
	private boolean debug;

	@Parameter(
		names = { "--help", "-h" },
		description = "Displays summary of options",
		help = true,
		order = 6)
	private boolean help;

	/**
	 * Default output file extension
	 */
	public static String OUTPUT_FORMAT = "ttl";


	private final Logger LOGGER = Logger.getLogger(OwlShaclFusekiApp.class);
	static {
		DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
	}

	/**
	 * Application for querying a Fuseki server.
	 * @param args Application arguments.
	 * @throws Exception Error
	 */
	public static void main(final String... args) throws Exception {
		final OwlShaclFusekiApp app = new OwlShaclFusekiApp();
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
	 * Creates a new OwlShaclFusekiApp object
	 */
	public OwlShaclFusekiApp() {
	}

	private void run() throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                     OWL Query " + getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info("Endpoint URL: " + endpointURL);
		LOGGER.info("Query path: " + queryPath);
		LOGGER.info("Result location: " + resultPath);

		final File queryFile = new File(queryPath);

		// Collect the queries (single file and directory handled the same way)
		// Key = fileName value = query
		HashMap<String, File> queries = getQueries(queryFile);
		// Check for any issues
		if (queries == null) {
			System.exit(1);
		}
		// Check for no given sparql files in a directory
		if (queries.size() == 0) {
			LOGGER.error("Warning: no .ttl files were found in the given directory.");
			System.exit(10);
		}

        final ExecutorService pool = Executors.newWorkStealingPool();
        CompletableFuture<Void> allExecuted= CompletableFuture.allOf(queries.entrySet().stream().map(e -> executeQuery(e.getKey(), e.getValue())).toArray(CompletableFuture[]::new));
        allExecuted.get();
        LOGGER.info("All queries executed.");
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

    private String getAppVersion() throws Exception {
    	var version = this.getClass().getPackage().getImplementationVersion();
    	return (version != null) ? version : "<SNAPSHOT>";
    }

	/**
	 * Executes a given query and outputs the result to result/outputName.frame
	 * 
	 * @param outputName name of the output file
	 * @param query      query to be executed
	 */
	private CompletableFuture<Void> executeQuery (String outputName, File query) {
        return CompletableFuture.runAsync(() -> {
			var client = HttpClient.newHttpClient();
			try {
				var request = HttpRequest.newBuilder(URI.create(endpointURL+"/"+shaclService+"?graph=default")).header("Content-Type", "text/turtle")
						.header("Accept", "text/turtle")
						.POST(HttpRequest.BodyPublishers.ofInputStream(Suppliers.ofInstance(new FileInputStream(query))))
						.build();
	
				final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
	
				final Model model = ModelFactory.createDefaultModel();
				LOGGER.info("Parfsing result of " + query + "\n" + response.body());
				RDFParser.create().fromString(response.body()).lang(RDFLanguages.TURTLE).build().parse(model);
				LOGGER.info("Finished parsing result: " + model);
	
				File output = new File(resultPath + File.separator + outputName + "." + OUTPUT_FORMAT);
	
				if (output.exists()) {
					output.delete();
				}
				output.createNewFile();
				FileOutputStream res = new FileOutputStream(output);
	
				model.write(res, "turtle");
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}
        });
	}

	// Modified from owl-diff
	private String getFileName(final File file) {
		String fileName = file.getName();
		if (fileName.lastIndexOf(".") != -1)
			return fileName.substring(0, fileName.lastIndexOf("."));
		else
			return "";
	}

	private HashMap<String, File> getQueries(final File file) {
		if (file.isFile()) {
			// Edited to accept any of the given file extensions
			if (getFileExtension(file).equals("ttl")) {
				// Read query from file and add it to collection
				try {
					HashMap<String, File> queries = new HashMap<>();
					queries.put(getFileName(file), file);
					return queries;
				} catch (QueryException e) {
					String errorMsg = "File: " + file.getName() + " . Error with parsing this file's query: " + e;
					LOGGER.error(errorMsg, e);
					return null;
				}
			} else {
				LOGGER.error("Please give an input query of type .ttl");
				return null;
			}
		} else if (file.isDirectory()) {
			return collectQueries(file);
		} else {
			// Neither file nor directory?
			LOGGER.error("Given input is not valid (not a file nor directory");
			return null;
		}
	}

	// Helper to getQueries: Given a File directory, return an HashMap of <FileName,
	// Query> pairs
	private HashMap<String, File> collectQueries(final File directory) {
		HashMap<String, File> queries = new HashMap<>();
		for (File file : Objects.requireNonNull(directory.listFiles())) {
			if (file.isFile()) {
				// Edited to accept any of the given file extensions
				if (getFileExtension(file).equals("ttl")) {
					// Read query from file and add it to collection
					try {
						queries.put(getFileName(file), file);
					} catch (QueryException e) {
						String errorMsg = "File: " + file.getName() + " . Error with parsing this file's query: " + e;
						LOGGER.error(errorMsg, e);
					}
				}
			} else if (file.isDirectory()) {
				queries.putAll(collectQueries(file));
			}
		}
		return queries;
	}

	/**
	 * @param file a file
	 * @return the file extension without the period.
	 */
	private String getFileExtension(final File file) {
		String fileName = file.getName();
		if (fileName.lastIndexOf(".") != -1)
			return fileName.substring(fileName.lastIndexOf(".") + 1);
		else
			return "";
	}

	// ------------

	/**
	 * A parameter validator for an URI string
	 */
	public static class URIValidator implements IParameterValidator {
		/**
		 * Creates a new URIValidator object
		 */
		public URIValidator() {
		}
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			try {
				new URI(value);
			} catch (URISyntaxException e) {
				throw new ParameterException("Invalid URI syntax", e);
			}
		}
	}

	/**
	 * A parameter validator for an output result folder path.
	 */
	public static class ResultFolderPath implements IParameterValidator {
		/**
		 * Creates a new ResultFolderPath object
		 */
		public ResultFolderPath() {
		}
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File directory = new File(value);
			if (!directory.exists()) {
				directory.mkdir();
			}
		}
	}

	/**
	 * A parameter validator for an existing query file.
	 */
	public static class QueryPath implements IParameterValidator {
		/**
		 * Creates a new QueryPath object
		 */
		public QueryPath() {
		}
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File input = new File(value);
			if (!input.exists()) {
				throw new ParameterException(
						"Paramter " + name + " does not exist at " + value + "\n Please give an existing input");
			}
		}
	}

}
