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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.io.CharStreams;

import org.apache.jena.query.*;
import org.apache.jena.query.QueryType;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;

import org.apache.jena.rdf.model.Model ;
import org.apache.jena.rdf.model.ModelFactory ;
import org.apache.jena.util.FileManager;

public class App {
  
	@Parameter(
		names = { "--endpoint", "-e" },
		description = "Sparql Endpoint URL.  (Required)",
		required = true,
		order = 1)
	String endpoint;
	
	@Parameter(
		names = { "--query", "-q" },
		description = "Path to the .sparql query file (Required)",
		required = true,
		order = 3)
	String queriesPath;
	
	@Parameter(
		names = { "--result", "-r" },
		description = "Path to the folder to save the result to (Required)",
		required = true,
		order = 4)
	String resultsPath;
	
	@Parameter(
		names = { "--format", "-f" },
		description = "Format of the results. Must be either xml, json, csv, or tsv (Required)",
		validateWith = FormatType.class, 
		required = true,
		order = 4)
	String formatType;

	@Parameter(
		names = { "-d", "--debug" },
		description = "Shows debug logging statements",
		order = 9)
	private boolean debug;

	@Parameter(
		names = { "--help", "-h" },
		description = "Displays summary of options",
		help = true,
		order =10)
	private boolean help;
	
	private final Logger LOGGER = LogManager.getLogger("Owl Query"); {
		LOGGER.setLevel(Level.INFO);
		PatternLayout layout = new PatternLayout("%r [%t] %-5p %c %x - %m%n");
		LOGGER.addAppender(new ConsoleAppender(layout));
	}

	public static void main(final String... args) {
		final App app = new App();
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
	    try {
			app.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                     OWL Query " + getAppVersion());
		LOGGER.info("=================================================================");
	    	    
		// Create query from the given file 
		final File queryFile = new File(queriesPath);
		String fileName = queryFile.getName();
		Query query = QueryFactory.create(); 
		try {
			query = QueryFactory.read(queryFile.toURI().getPath());
		} catch (QueryException e) {
			String errorMsg = "File: " + fileName + " . Error with parsing this file's query. ";
			LOGGER.error(errorMsg, e);
			System.exit(1);
		}
		
		// Create remote connection to Fuseki server
		RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create()
				.updateEndpoint("update")
				.queryEndpoint("sparql")
				.destination(endpoint);
		
		// Execute queries on the server
		try (RDFConnection conn = builder.build()) {
			String outputName = getFileName(queryFile);
			File output = new File("results/"+outputName+".frame");
			if (output.exists()) {
				output.delete(); 
			}
			output.createNewFile(); 
			FileOutputStream res = new FileOutputStream(output);
			QueryType type = query.queryType();
			//Given the type of query, execute it
			switch (type) {
				case ASK:
					LOGGER.info("Ask");
					byte b[] = Boolean.toString(conn.queryAsk(query)).getBytes(); 
					res.write(b); 
					break; 
				case CONSTRUCT_JSON:
				case CONSTRUCT_QUADS:
				case CONSTRUCT:
					LOGGER.info("Construct");
					conn.queryConstruct(query).write(res); 
					break; 
				case DESCRIBE:
					LOGGER.info("Describe");
					conn.queryDescribe(query).write(res);
					break; 
				case SELECT:
					LOGGER.info("Select");
					conn.queryResultSet(query, (resultSet)-> {
						ResultSetFormatter.outputAsXML(res, resultSet);
					});
					break; 
				case UNKNOWN:
					LOGGER.info("Unknown query. Please reformat");
					break;
				default:
					LOGGER.info("Default reached? Please reformat query");
					break;
			}
			//Close the writer 
			res.close(); 
		} catch (IOException e) {
			LOGGER.info("Failed to create open file"); 
			e.printStackTrace(); 
		}
	    LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
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

	public static class FormatType implements IParameterValidator {
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			final ArrayList<String> formatTypes = new ArrayList<String>() {{
				add("xml");
				add("json");
				add("csv");
				add("tsv");
			}};
			if (!formatTypes.contains(value.toLowerCase())) {
				throw new ParameterException("Paramter " + name + " must be either xml, json, csv, or tsv");
			}
		}
	}
	
	//Modified from owl-diff
	private String getFileName(final File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1)
        	return fileName.substring(0, fileName.lastIndexOf("."));
        else 
        	return "";
	}
	
}