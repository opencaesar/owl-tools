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
package io.opencaesar.owl.load;

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
		names = { "--catalog", "-c" },
		description = "path to the OWL catalog file (Required)",
		validateWith = CatalogPath.class,
		required = true, 
		order = 1)
	private String catalogPath;
	
	@Parameter(
			names = { "--endpoint", "-e" },
			description = "Sparql Endpoint URL, including the name of the dataset (Required)",
			required = true,
			order = 2)
	private String endpoint;
	
	@Parameter(
			names = { "--file-extensions", "-f" },
			description = "File extensions of files that will be uploaded. Default is only .owl (Not Required)",
			required = false,
			order = 3)
	private List<String> fileExt = new ArrayList<String>() {{
		add("owl");
	}};
	
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
		
	private final Logger LOGGER = LogManager.getLogger("Owl Load"); {
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
		LOGGER.info("                     OWL Load " + getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info(("OWL Catalog = " + catalogPath));	    
		LOGGER.info(("File Extensions = " + fileExt)); 
		LOGGER.info(("Endpoint = " + endpoint));

		// Get files from catalog - Reused from owl-diff
		File catalogFile = new File(catalogPath); 
		final File folder = catalogFile.getParentFile();
		final Collection<File> files = collectOwlFiles(folder, fileExt);
		
		// Load the files into the dataset in parallel
		String relativeDirectory = catalogFile.getParent().replaceAll("\\\\", "/");
		if (relativeDirectory.length() > 0) {
			relativeDirectory = relativeDirectory.concat("/");
		}
		ArrayList<Thread> threads = new ArrayList<Thread>(); 
		for (File file: files) {
			String relativePath = folder.toURI().relativize(file.toURI()).getPath();
			String finalPath = relativeDirectory.concat(relativePath);
			Thread thread = new Thread(new Runnable() {
				public void run() {
					//Create remote connection to Fuseki server
					RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create()
							.updateEndpoint("update")
							.queryEndpoint("sparql")
							.destination(endpoint);
					RDFConnection conn = builder.build();
					try {
						conn.load(finalPath);
					} finally {
						conn.commit();
						conn.close();
						conn.end();
					}
				}
			});
			threads.add(thread); 
			thread.start(); 
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

	public static class CatalogPath implements IParameterValidator {
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File file = new File(value);
			if (!file.getName().endsWith("catalog.xml")) {
				throw new ParameterException("Parameter " + name + " should be a valid OWL catalog path");
			}
		}
	}
	
	// Given a File directory, return an Collection of Files (implemented as ArrayList) - Reused from owl-diff
	// Add parameter to account for multiple file extensions 
	private Collection<File> collectOwlFiles(final File directory, List<String> fileExt) {
		ArrayList<File> omlFiles = new ArrayList<File>();
		for (File file : directory.listFiles()) {
			if (file.isFile()) {
				//Edited to accept any of the given file extensions 
				if (fileExt.contains(getFileExtension(file))) {
					omlFiles.add(file);
				}
			} else if (file.isDirectory()) {
				omlFiles.addAll(collectOwlFiles(file, fileExt));
			}
		}
		return omlFiles;
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