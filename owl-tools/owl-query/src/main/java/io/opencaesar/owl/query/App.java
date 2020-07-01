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
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;

import org.apache.jena.rdf.model.Model ;
import org.apache.jena.rdf.model.ModelFactory ;
import org.apache.jena.util.FileManager;

public class App {
  
	@Parameter(
		names = { "--endpoint", "-e" },
		description = "Sparql Endpoint URL. Must end in / (Required)",
		required = true,
		order = 1)
	String endpoint;
	
	@Parameter(
		names = { "--dataset-name", "-n" },
		description = "Name of the dataset (Required)",
		required = true,
		order = 2)
	String datasetName;
	
	@Parameter(
		names = { "--queries", "-q" },
		description = "Path to folder containing .sparql query files (Required)",
		required = true,
		order = 3)
	String queriesPath;
	
	@Parameter(
		names = { "--results", "-r" },
		description = "Path of folder to save results to (Required)",
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
	
	private final Logger LOGGER = LogManager.getLogger("Owl Reason"); {
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
	    	    
		// Get query files
		final File folder = new File(queriesPath);
		final Collection<Query> queries = collectSparqlFiles(folder);
		/*
		for (Query query: Query) {
			//System.out.println(file.getName());
		}
		*/
		
		/*
		//Create remote connection to Fuseki server
		RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create()
				.updateEndpoint("update")
				.queryEndpoint("sparql")
				.destination(endpoint + datasetName);
		*/
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
	
	// Given a File directory, return an Collection of Queries (implemented as ArrayList)
	private Collection<Query> collectSparqlFiles(final File directory) {
		ArrayList<Query> queries = new ArrayList<Query>();
		for (File file : directory.listFiles()) {
			if (file.isFile()) {
				//Edited to accept any of the given file extensions 
				if (getFileExtension(file).equals("sparql")) {
					//Read query from file and add it to collection 
					LOGGER.info(("File path: " + file.getPath()));
					//queries.add(QueryFactory.read(file.getPath()));
				}
			} else if (file.isDirectory()) {
				queries.addAll(collectSparqlFiles(file));
			}
		}
		return queries;
	}
	
	//Reused from owl-diff
	private String getFileExtension(final File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1)
        	return fileName.substring(fileName.lastIndexOf(".")+1);
        else 
        	return "";
	}
	
}