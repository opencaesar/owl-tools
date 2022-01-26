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
package io.opencaesar.owl.query;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryType;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.sparql.resultset.ResultsFormat;
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

public class OwlQueryApp {
	
	@Parameter(
		names = {"--endpoint-url", "-e"},
		description = "Sparql Endpoint URL (Required)",
		required = true,
		order = 1)
	private String endpointURL;
	
	@Parameter(
		names = {"--query-path", "-q"},
		description = "Path to the .sparql query file or directory (Required)",
		validateWith = QueryPath.class,
		required = true,
		order = 2)
	private String queryPath;
	
	@Parameter(
		names = {"--result-path", "-r"},
		description = "Path to the folder to save the result to (Required)",
		validateWith = ResultFolderPath.class, 
		required = true,
		order = 3)
	private String resultPath;
	
	@Parameter(
		names = {"--format", "-f"},
		description = "Format of the results. Default is xml. Options: xml, json, csv, n3, ttl, n-triple or tsv (Optional)",
		validateWith = FormatType.class,
		order = 4)
	private String format = "xml";

	@Parameter(
		names = {"--debug", "-d"},
		description = "Shows debug logging statements",
		order = 5)
	private boolean debug;

	@Parameter(
		names = {"--help", "-h"},
		description = "Displays summary of options",
		help = true,
		order =6)
	private boolean help;
	
	private static final Logger LOGGER = Logger.getLogger(OwlQueryApp.class);
	static {
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
	}

	public static void main(final String... args) throws Exception {
		final OwlQueryApp app = new OwlQueryApp();
		final JCommander builder = JCommander.newBuilder().addObject(app).build();
		//final File queryFile = new File(app.queryPath);

		builder.parse(args);
		if (app.help) {
			builder.usage();
			return;
		}
		if (app.debug) {
			final Appender appender = LogManager.getRootLogger().getAppender("stdout");
			((AppenderSkeleton) appender).setThreshold(Level.DEBUG);
		}
		app.run(); // exceptions are not caught to allow the Gradle task to handle
	}

	private void run() throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                     OWL Query " + getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info("Endpoint URL: " + endpointURL);
		LOGGER.info("Query path: " + queryPath);
		LOGGER.info("Result location: " + resultPath);
		LOGGER.info("Format Type: " + format);
		
		final File queryFile = new File(queryPath);
		
		// Collect the queries (single file and directory handled the same way) 
		// Key = fileName value = query
		HashMap<String, Query> queries = getQueries(queryFile);
		// Check for any issues 
		if (queries == null || queries.isEmpty()) {
			throw new Exception("No .sparql queries found");
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

    private String getAppVersion() {
    	var version = this.getClass().getPackage().getImplementationVersion();
    	return (version != null) ? version : "<SNAPSHOT>";
    }
	
	/**
	 * Executes a given query and outputs the result to result/outputName.frame
	 * @param outputName name of the output file 
	 * @param query query to be executed
	 */
	private CompletableFuture<Void> executeQuery (String outputName, Query query) {
        return CompletableFuture.runAsync(() -> {
			// Create remote connection to query endpoint
			RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create()
					.updateEndpoint("update")
					.queryEndpoint("sparql")
					.destination(endpointURL);
				
			File output = new File(resultPath + File.separator + outputName + "."+format);
			if (output.exists()) {
				//noinspection ResultOfMethodCallIgnored
				output.delete();
			}
			try {
				//noinspection ResultOfMethodCallIgnored
				output.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} 

			// Execute queries on the endpoint
			try (RDFConnection conn = builder.build();
				 FileOutputStream res = new FileOutputStream(output)) {
	
				QueryType type = query.queryType();
				//Given the type of query, execute it
				switch (type) {
					case ASK:
						LOGGER.info("Query Type: Ask");
						byte[] b = Boolean.toString(conn.queryAsk(query)).getBytes();
						res.write(b); 
						break; 
					case CONSTRUCT_JSON:
					case CONSTRUCT:
						LOGGER.info("Query Type: Construct");
						String constructFmt = getOutType(format); 
						conn.queryConstruct(query).write(res, constructFmt); 
						break; 
					case DESCRIBE:
						LOGGER.info("Query Type: Describe");
						String describeFmt = getOutType(format); 
						conn.queryDescribe(query).write(res, describeFmt);
						break; 
					case SELECT:
						LOGGER.info("Query Type: Select");
						// Certain formats call the function directly for better formatting 
						// EX: Calling the xml function directly gives W3 standard format
						switch (format.toLowerCase()) {
							case "xml":
								conn.queryResultSet(query, (resultSet)-> ResultSetFormatter.outputAsXML(res, resultSet));
								break;
							case "tsv":
								conn.queryResultSet(query, (resultSet)-> ResultSetFormatter.outputAsTSV(res, resultSet));
								break;	
							case "csv":
								conn.queryResultSet(query, (resultSet)-> ResultSetFormatter.outputAsCSV(res, resultSet));
								break;	
							case "json":
								conn.queryResultSet(query, (resultSet)-> ResultSetFormatter.outputAsJSON(res, resultSet));
								break;	 
							case "n-triple": 
								conn.queryResultSet(query, (resultSet)-> ResultSetFormatter.output(res, resultSet, ResultsFormat.FMT_RDF_NT));
								break;
							case "ttl":
								conn.queryResultSet(query, (resultSet)-> ResultSetFormatter.output(res, resultSet, ResultsFormat.FMT_RDF_TURTLE));
								break;
							default:
								throw new RuntimeException(format + " is not a valid output format for select queries. Please use one of the listed formats: xml, ttl, csv, json, tsv");
						}
						break; 
					case UNKNOWN:
						LOGGER.info("Unknown query. Please reformat");
						break;
					default:
						LOGGER.info("Default reached? Please reformat query");
						break;
				}
				LOGGER.info("Result saved at: " + output);
			} catch (IOException e) {
				throw new RuntimeException("Error occurred executing query '"+outputName+"'", e);
			}
			// ideally, we should check to ensure that output was created...
        });
	}

	//Modified from owl-diff
	private String getFileName(final File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1)
        	return fileName.substring(0, fileName.lastIndexOf("."));
        else 
        	return "";
	}
	
	private HashMap<String, Query> getQueries(final File file) {
		if (file.isFile()) {
			//Edited to accept any of the given file extensions 
			if (getFileExtension(file).equals("sparql")) {
				//Read query from file and add it to collection 
				try {
					HashMap<String, Query> queries = new HashMap<>();
					queries.put(getFileName(file), QueryFactory.read(file.toURI().getPath()));
					return queries;
				} catch (QueryException e) {
					String errorMsg = "File: " + file.getName() + " . Error with parsing this file's query: " + e;
					LOGGER.error(errorMsg, e);
					return null; 
				}
			} else {
				throw new ParameterException("Please give an input query of type .sparql");
			}
		} else if (file.isDirectory()) {
			return collectQueries(file);
		} else {
			//Neither file nor directory? 
			throw new ParameterException("Given input is not valid (not a file nor directory");
		}
	}
	
	// Helper to getQueries: Given a File directory, return an HashMap of <FileName, Query> pairs
	private HashMap<String, Query> collectQueries(final File directory) throws QueryException {
		HashMap<String, Query> queries = new HashMap<>();
		final File[] files = directory.listFiles();
		if (null != files)
			for (File file : files) {
				if (file.isFile()) {
					//Edited to accept any of the given file extensions
					if (getFileExtension(file).equals("sparql")) {
						//Read query from file and add it to collection
						try {
							queries.put(getFileName(file), QueryFactory.read(file.toURI().getPath()));
						} catch (QueryException e) {
							throw new QueryException("File: " + file.getName() + " . Error with parsing this file's query: ", e);
						}
					}
				} else if (file.isDirectory()) {
					queries.putAll(collectQueries(file));
				}
			}
		return queries;
	}
	
	public String getFileExtension(final File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1)
        	return fileName.substring(fileName.lastIndexOf(".")+1);
        else 
        	return "";
	}

	//Get proper conversion for format string for describe/construct queries 
	private String getOutType(String formatType) throws ParameterException {
		switch (formatType.toLowerCase()) {
			case "xml":
				return "RDF/XML";
			case "n3":
				return "N3";
			case "n-triple":
				return "N-TRIPLE";
			case "ttl":
				return "TURTLE";
			default:
				//Not a valid output type for describe/construct queries 
				throw new ParameterException(formatType + " is not a valid output type for describe/construct queries. Please use xml, n3, n-triple or ttl");
		}
	}

	//------------
	

	public static class FormatType implements IParameterValidator {
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			final List<String> formatTypes = Arrays.asList("xml", "json", "csv", "tsv", "n3", "ttl", "n-triple");
			if (!formatTypes.contains(value.toLowerCase())) {
				throw new ParameterException("Parameter '" + name + "' must be either xml, json, csv, n3, ttl, n-triple or tsv");
			}
		}
	}
	
	public static class ResultFolderPath implements IParameterValidator {
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File directory = new File(value);
			if (!directory.exists()) {
				//noinspection ResultOfMethodCallIgnored
				directory.mkdirs();
				if (!directory.isDirectory()) {
					throw new ParameterException("Path '" + value + "' is not a valid folder path");
				}
			}
	  	}
	}
	
	public static class QueryPath implements IParameterValidator {
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File input = new File(value);
			if (!input.exists()) {
				throw new ParameterException("Paramter " + name + " does not exist at " + value
						+ "\n Please give an existing input");
			}
		}
	}

}

