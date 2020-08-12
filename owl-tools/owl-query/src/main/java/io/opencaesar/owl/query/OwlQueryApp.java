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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.ext.com.google.common.io.CharStreams;
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
		description = "Path to the .sparql query file (Required)",
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
		description = "Format of the results. Must be either xml, json, csv, n3, ttl, n-triple or tsv (Required)",
		validateWith = FormatType.class, 
		required = false,
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
	
	private final Logger LOGGER = Logger.getLogger(OwlQueryApp.class);
	{
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
	}

	public static void main(final String... args) throws Exception {
		final OwlQueryApp app = new OwlQueryApp();
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
		LOGGER.info("                     OWL Query " + getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info("Endpoint URL: " + endpointURL);
		LOGGER.info("Query path: " + queryPath);
		LOGGER.info("Result location: " + resultPath);
		LOGGER.info("Format Type: " + format);
		
		// Create query from the given file 
		final File queryFile = new File(queryPath);
		String fileName = queryFile.getName();
		Query query = QueryFactory.create(); 
		try {
			query = QueryFactory.read(queryFile.toURI().getPath());
		} catch (QueryException e) {
			String errorMsg = "File: " + fileName + " . Error with parsing this file's query. ";
			LOGGER.error(errorMsg, e);
			System.exit(1);
		}
		
		// Create remote connection to query endpoint
		RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create()
				.updateEndpoint("update")
				.queryEndpoint("sparql")
				.destination(endpointURL);
		
		// Execute queries on the endpoint
		try (RDFConnection conn = builder.build()) {
			String outputName = getFileName(queryFile);
			File output = new File(resultPath + File.separator + outputName + ".frame");
			if (output.exists()) {
				output.delete(); 
			}
			output.createNewFile(); 
			FileOutputStream res = new FileOutputStream(output);
			QueryType type = query.queryType();
			//Given the type of query, execute it
			switch (type) {
				case ASK:
					LOGGER.info("Query Type: Ask");
					//ResultSetFormatter.out(res, conn.queryAsk(query));
					byte b[] = Boolean.toString(conn.queryAsk(query)).getBytes();
					res.write(b); 
					break; 
				case CONSTRUCT_JSON:
				case CONSTRUCT_QUADS:
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
					ResultsFormat selectFmt = getSelectType(format);
					conn.queryResultSet(query, (resultSet)-> {
						ResultSetFormatter.output(res, resultSet, selectFmt);
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
			conn.close(); 
			LOGGER.info("Result saved at: " + resultPath + File.separator + outputName + ".frame");
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
	private String getAppVersion() {
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

	//Modified from owl-diff
	private String getFileName(final File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1)
        	return fileName.substring(0, fileName.lastIndexOf("."));
        else 
        	return "";
	}

	//Get proper conversion for format string for describe/construct queries 
	private String getOutType(String formatType) {
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
				LOGGER.error(formatType + " is not a valid output type for describe/construct queries. Please use xml, n3, n-triple or ttl");
				System.exit(1);
				return "NULL";
		}
	}
	
	//Get proper conversion for format for select queries 
	private ResultsFormat getSelectType (String formatType) {
		switch (formatType.toLowerCase()) {
			case "xml":
				return ResultsFormat.FMT_RDF_XML; 
			case "n-triple": 
				return ResultsFormat.FMT_RDF_NT;
			case "ttl":
				return ResultsFormat.FMT_RDF_TURTLE;
			case "csv":
				return ResultsFormat.FMT_RS_CSV;
			case "json":
				return ResultsFormat.FMT_RS_JSON; 
			case "tsv":
				return ResultsFormat.FMT_RS_TSV;
			default:
				LOGGER.error(formatType + " is not a valid output format for select queries. Please use one of the listed formats: xml, ttl, csv, json, tsv");
				System.exit(1);
				return ResultsFormat.FMT_NONE;
		}
	}

	//------------
	
	public static class FormatType implements IParameterValidator {
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			final List<String> formatTypes = Arrays.asList("xml", "json", "csv", "tsv", "n3", "ttl", "n-triple");
			if (!formatTypes.contains(value.toLowerCase())) {
				throw new ParameterException("Paramter " + name + " must be either xml, json, csv, n3, ttl, n-triple or tsv");
			}
		}
	}
	
	public static class ResultFolderPath implements IParameterValidator {
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			File directory = new File(value);
			if (!directory.exists()) {
				directory.mkdir();
			}
	  	}
	}

}

