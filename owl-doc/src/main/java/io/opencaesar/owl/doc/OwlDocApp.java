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
package io.opencaesar.owl.doc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.ext.com.google.common.io.CharStreams;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QueryType;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class OwlDocApp {

	@Parameter(
		names = { "--endpoint-url", "-e" },
		description = "SPARQL endpoint URL (Required)",
		required = true,
		order = 1)
	private String sparqlEndpoint;

	@Parameter(
		names = { "--document-iri", "-i" },
		description = "Document IRI (Required)",
		required = true,
		order = 2)
	private String documentIri;

	@Parameter(
		names = { "--output-path", "-o" },
		description = "Output binding tree filename (Required)",
		required = true,
		order = 3)
	private String outputFilename;

	@Parameter(
		names = { "-d", "--debug" },
		description = "Shows debug logging statements",
		order = 4)
	private boolean debug;

	@Parameter(
		names = { "--help", "-h" },
		description = "Displays summary of options",
		help = true,
		order = 5)
	private boolean help;

	private static final String DOC_PREFIX = "http://opencaesar.io/document#";
	private static final String GET_VALUES_QUERY = getQuery("getValues.sparql");

	private Map<String, Map<String, RDFNode>> elementImpliedBindingValues = new HashMap<>();
	private Map<String, Set<RDFNode>> elementSparqlExpansions;
	private Map<String, Set<RDFNode>> elementSparqlMappings;
	private Map<String, Set<RDFNode>> elementChildren;
	private Map<String, Set<RDFNode>> elementRequiredBindings;
	private Map<String, Set<RDFNode>> elementOptionalBindings;
	private Map<String, Set<RDFNode>> elementForwardedBindings;
	private Map<String, Set<RDFNode>> elementTypes;
	
	private final Logger LOGGER = Logger.getLogger(OwlDocApp.class);
	{
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
	}

	public static void main(final String... args) throws Exception {
		final OwlDocApp app = new OwlDocApp();
		final JCommander builder = JCommander.newBuilder().addObject(app).build();
		builder.parse(args);
		if (app.help) {
			builder.usage();
			return;
		}
		if (app.debug) {
			LogManager.getRootLogger().setLevel(Level.DEBUG);
		}
		app.run();
	}

	private void run() throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                    OwlDoc " + getAppVersion());
		LOGGER.info("=================================================================");
		
		LOGGER.info("SPARQL Endpoint: " + sparqlEndpoint);
		LOGGER.info("Document IRI: " + documentIri);
		LOGGER.info("Output Filename: " + outputFilename);
		
		try (RDFConnection conn = RDFConnectionFactory.connect(sparqlEndpoint)) {
			loadImpliedBindings(conn);
			elementSparqlExpansions = getValues(conn, DOC_PREFIX + "hasSparqlExpansion");
			elementSparqlMappings = getValues(conn, DOC_PREFIX + "hasSparqlMapping");
			elementChildren = getValues(conn, DOC_PREFIX + "hasElement");
			elementRequiredBindings = getValues(conn, DOC_PREFIX + "hasRequiredBinding");
			elementOptionalBindings = getValues(conn, DOC_PREFIX + "hasOptionalBinding");
			elementForwardedBindings = getValues(conn, DOC_PREFIX + "hasForwardedBinding");
			
			elementTypes = getMap(conn, getQuery("getElementTypes.sparql"), documentIri);
			elementTypes.putAll(getMap(conn, getQuery("getDocumentTypes.sparql"), documentIri, documentIri));
			
			List<Node> nodes = createNodes(conn, documentIri, Collections.emptyMap());
			if (nodes.isEmpty()) {
				throw new IllegalStateException("No nodes produced from " + documentIri);
			}
			
			File outputFile = new File(outputFilename);
			if (!outputFile.getParentFile().exists()) {
				outputFile.getParentFile().mkdirs();
			}
			try (Writer output = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
				output.write(nodes.get(0).toJson());
			}
		}
		
	    LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}
	

	private List<Node> createNodes(RDFConnection conn, String elementIri, Map<String, RDFNode> forwardedBindings) {
		LOGGER.debug("Processing " + elementIri);
		Map<String, RDFNode> inputBindings = new HashMap<>(forwardedBindings);
		Map<String, Boolean> queryVars = new HashMap<>();
		List<Node> results;
		if (elementImpliedBindingValues.containsKey(elementIri)) {
			inputBindings.putAll(elementImpliedBindingValues.get(elementIri));
		}
		elementOptionalBindings.getOrDefault(elementIri, Collections.emptySet()).stream().map(o -> o.toString().split("\\s+")).forEach(splitName -> {
			if (splitName.length == 1) {
				queryVars.put(splitName[0], false);
			} else if (splitName.length == 3 && splitName[1].equalsIgnoreCase("as")) {
				queryVars.put(splitName[2], false);
				inputBindings.put(splitName[2], inputBindings.get(splitName[0]));
			} else {
				throw new IllegalStateException("Invalid @hasOptionalBinding format on " + elementIri);
			}
		});
		elementRequiredBindings.getOrDefault(elementIri, Collections.emptySet()).stream().map(o -> o.toString().trim().split("\\s+")).forEach(splitName -> {
			if (splitName.length == 1) {
				queryVars.put(splitName[0], true);
			} else if (splitName.length == 3 && splitName[1].equalsIgnoreCase("as")) {
				queryVars.put(splitName[2], true);
				inputBindings.put(splitName[2], inputBindings.get(splitName[0]));
			} else {
				throw new IllegalStateException("Invalid @hasRequiredBinding format on " + elementIri);
			}
		});
		if (elementSparqlExpansions.containsKey(elementIri)) {
			if (elementSparqlMappings.containsKey(elementIri)) {
				throw new IllegalStateException(elementIri + " has both expansion and mapping queries");
			}
			String query = createBindingQuery(elementIri, elementSparqlExpansions, inputBindings, queryVars);
			LOGGER.debug("Running Query: " + query);
			results = new ArrayList<>();
			try (QueryExecution exec = conn.query(query)) {
				exec.execSelect().forEachRemaining(solution -> {
					Node element = new Node(elementIri);
					for (Map.Entry<String, RDFNode> e : inputBindings.entrySet()) {
						element.bindings.put(e.getKey(), e.getValue());
					}
					solution.varNames().forEachRemaining(varName -> {
						element.bindings.put(varName, solution.get(varName));
					});
					results.add(element);
				});
			} catch (Exception e) {
				LOGGER.error("Failed in query: " + query, e);
				throw e;
			}
		} else {
			Node element = new Node(elementIri);
			element.iri = elementIri;
			for (Map.Entry<String, RDFNode> e : inputBindings.entrySet()) {
				element.bindings.put(e.getKey(), e.getValue());
			}
			if (elementSparqlMappings.containsKey(elementIri)) {
				String query = createBindingQuery(elementIri, elementSparqlMappings, inputBindings, queryVars);
				LOGGER.debug("Running Query: " + query);
				try (QueryExecution exec = conn.query(query)) {
					ResultSet resultSet = exec.execSelect();
					if (resultSet.hasNext()) {
						QuerySolution solution = resultSet.next();
						solution.varNames().forEachRemaining(varName -> {
							element.bindings.put(varName, solution.get(varName));
						});
					}
				} catch (Exception e) {
					LOGGER.error("Failed in query: " + query, e);
					throw e;
				}
			}
			results = Collections.singletonList(element);
		}
		for (Node result : results) {
			Map<String, RDFNode> forwardedToChildren = new HashMap<>();
			if (elementForwardedBindings.containsKey(elementIri)) {
				elementForwardedBindings.get(elementIri).stream().map(o -> o.toString().trim().split("\\s+")).forEach(bindingName -> {
					if (bindingName.length == 1) {
						if (bindingName[0].equals("*")) {
							forwardedToChildren.putAll(result.bindings);
						} else {
							forwardedToChildren.put(bindingName[0], result.bindings.get(bindingName[0]));
						}
					} else if (bindingName.length == 3 && bindingName[1].equalsIgnoreCase("as")) {
						forwardedToChildren.put(bindingName[2], result.bindings.get(bindingName[0]));
					} else {
						throw new IllegalStateException("Invalid @forwardedBinding format on " + elementIri);
					}
				});
			}
			elementChildren.getOrDefault(elementIri, Collections.emptySet()).stream().map(Object::toString).sorted().forEach(childIri -> {
				result.children.addAll(createNodes(conn, childIri, forwardedToChildren));
			});
		}
		return results;
	}

	private String createBindingQuery(String elementIri, Map<String, Set<RDFNode>> queryMap, Map<String, RDFNode> inputBindings, Map<String, Boolean> queryVars) {
		ParameterizedSparqlString sparql = new ParameterizedSparqlString(queryMap.get(elementIri).iterator().next().toString());
		
		for (Map.Entry<String, Boolean> queryVarAndRequired : queryVars.entrySet()) {
			String varName = queryVarAndRequired.getKey();
			RDFNode inputValue = inputBindings.get(varName);
			if (inputValue != null) {
				if (inputValue.isLiteral()) {
					sparql.setLiteral(varName, inputValue.asLiteral());
				} else if (inputValue.isResource()) {
					sparql.setIri(varName, inputValue.asResource().getURI());
				} else {
					throw new IllegalStateException("Unrecognized value type " + inputValue);
				}
			} else if ("DOCUMENT".equals(varName)) {
				sparql.setIri("DOCUMENT", documentIri);
			} else if ("SELF".equals(varName)) {
				sparql.setIri("SELF", elementIri);
			} else {
				if (queryVarAndRequired.getValue()) {
					throw new IllegalStateException("Required input binding " +  varName + " missing on " + elementIri);
				}
			}
		}
		return sparql.toString();
	}

    private static Map<String, Set<RDFNode>> getValues(RDFConnection conn, String property) {
        return getMap(conn, GET_VALUES_QUERY, property);
    }
    
    private static Map<String, Set<RDFNode>> getMap(RDFConnection conn, String query, String... parameterIris) {
    	Map<String, Set<RDFNode>> result = new HashMap<>();
    	ParameterizedSparqlString sparql = new ParameterizedSparqlString(query);
    	for (int i = 0; i < parameterIris.length; i++) {
    		sparql.setIri(i, parameterIris[i]);
    	}
        try (QueryExecution impliedBindings = conn.query(sparql.toString())) {
            impliedBindings.execSelect().forEachRemaining(solution -> {
                result.computeIfAbsent(solution.get("key").toString(), key -> new LinkedHashSet<>())
                        .add(solution.get("value"));
            });
        }
        return result;
    }

    /**
     * Collect all implied binding values
     */
    private void loadImpliedBindings(RDFConnection conn) {
        Map<String, Set<RDFNode>> propertyToImpliedBinding = getValues(conn, DOC_PREFIX + "impliesBinding");
        for (Map.Entry<String, Set<RDFNode>> propertyAndBindingNames : propertyToImpliedBinding.entrySet()) {
        	for (Map.Entry<String, Set<RDFNode>> subjectAndImpliedValues : getValues(conn, propertyAndBindingNames.getKey()).entrySet()) {
        		for (RDFNode bindingName : propertyAndBindingNames.getValue()) {
        			elementImpliedBindingValues.computeIfAbsent(subjectAndImpliedValues.getKey().toString(), key -> new HashMap<>())
                    	.put(bindingName.toString(), subjectAndImpliedValues.getValue().iterator().next());
        		}
        	}
        }
    }
    
    /**
     * Load a SPARQL query from a file in the io.opencaesar.owl.doc package
     */
    private static String getQuery(String fileName) {
        try (InputStream is = OwlDocApp.class.getClassLoader().getResourceAsStream("io/opencaesar/owl/doc/" + fileName)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
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


	public class Node {
		private String iri;
		private Map<String, RDFNode> bindings = new HashMap<>();
		private List<Node> children = new ArrayList<>();
		private Set<String> types;
		
		private Node(String iri) {
			this.iri = iri;
			types = elementTypes.getOrDefault(iri, Collections.emptySet()).stream().map(Object::toString).collect(Collectors.toSet());
			types.remove(DOC_PREFIX + "Element");
		}
		
		public String getIri() {
			return iri;
		}

		public Set<String> getTypes() {
			return types;
		}
		
		public Map<String, RDFNode> getBindings() {
			return bindings;
		}
		
		public List<Node> getChildren() {
			return children;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			toString(sb, "");
			return sb.toString();
		}
		
		private void toString(StringBuilder asString, String indent) {
			asString.append(indent).append("[").append(iri).append("]\n");
			asString.append(indent).append("  Types:");
			for (String type : types) {
				asString.append("\n").append(indent).append("    - ").append(type);
			}
			asString.append("\n").append(indent).append("  Bindings:");
			for (Map.Entry<String, RDFNode> binding : bindings.entrySet()) {
				asString.append("\n").append(indent).append("    ").append(binding.getKey()) .append(": ").append(binding.getValue().toString());
			}
			if (!children.isEmpty()) {
				asString.append("\n").append(indent).append("  Children:");
				String subIndent = indent + "    ";
				for (Node child : children) {
					asString.append("\n");
					child.toString(asString, subIndent + "  ");
				}
			}
		}
		
		public String toJson() throws JsonProcessingException {
			return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(toJsonData());
		}
		
		private Map<String, Object> toJsonData() {
			Map<String, Object> jsonNode = new HashMap<>();
			jsonNode.put("iri", iri);
			jsonNode.put("types", types);
			Map<String, Map<String,Object>> jsonBindings = new HashMap<>();
			jsonNode.put("bindings", jsonBindings);
			for (Map.Entry<String, RDFNode> binding : bindings.entrySet()) {
				Map<String, Object> jsonBinding = new HashMap<String, Object>();
				jsonBindings.put(binding.getKey(), jsonBinding);
				jsonBinding.put("value", binding.getValue().toString());
				if (binding.getValue().isLiteral()) {
					jsonBinding.put("type", "literal");
					Literal literal = binding.getValue().asLiteral();
					if (literal.getDatatype() != null) {
						jsonBinding.put("datatype", literal.getDatatypeURI());
					}
					if (StringUtils.isNotEmpty(literal.getLanguage())) {
						jsonBinding.put("xml:lang", literal.getLanguage());
					}
				} else if (binding.getValue().isResource()) {
					jsonBinding.put("type", "uri");
				} else if (binding.getValue().isAnon()) {
					jsonBinding.put("type", "bnode");
				}
			}
			jsonNode.put("children", children.stream().map(Node::toJsonData).collect(Collectors.toList()));
			return jsonNode;
		}
	}
}

