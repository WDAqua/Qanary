package eu.wdaqua.qanary.commons.triplestoreconnectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import eu.wdaqua.qanary.commons.QanaryUtils;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class QanaryTripleStoreConnector {
	private static final Logger logger = LoggerFactory.getLogger(QanaryTripleStoreConnector.class);
	
	private static final long MAX_ACCEPTABLE_QUERY_EXECUTION_TIME = 10000;

	public abstract void connect(); // TODO: add exception

	public abstract ResultSet select(String sparql) throws SparqlQueryFailed;

	public abstract boolean ask(String sparql) throws SparqlQueryFailed;

	public abstract void update(String sparql, URI graph) throws SparqlQueryFailed;

	public abstract void update(String sparql) throws SparqlQueryFailed;

	/**
	 * return a readable description of the triplestore endpoint
	 * 
	 * @return
	 */
	public abstract String getFullEndpointDescription();

	/**
	 * get current time in milliseconds
	 */
	protected static long getTime() {
		return System.currentTimeMillis();
	}

	/**
	 *
	 * @param description
	 * @param duration
	 */
	protected void logTime(long duration, String description) {
		if( duration > MAX_ACCEPTABLE_QUERY_EXECUTION_TIME) {
			logger.warn("runtime measurement: {} ms for {} (was very long)", duration, description);
		} else {
			logger.info("runtime measurement: {} ms for {}", duration, description);
		}
	}

	/**
	 * 
	 * @param duration
	 * @param description
	 */
	protected void logTime(long duration, String description, String endpoint) {
		logger.info("runtime measurement: {} ms on {} for {}", duration, endpoint, description);
	}

	protected static Logger getLogger() {
		return logger;
	}

	/**
	 * read SPARQL query from files in resources folder
	 * 
	 * @param filenameWithRelativePath
	 * @return
	 * @throws IOException
	 */
	public static String readFileFromResources(String filenameWithRelativePath) throws IOException {
		InputStream in = QanaryTripleStoreConnector.class.getResourceAsStream(filenameWithRelativePath);
		getLogger().info("filenameWithRelativePath: {}, {}", filenameWithRelativePath, in);

		if (in == null) {
			return null;
		} else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			return reader.lines().collect(Collectors.joining("\n"));
		}
	}

	/**
	 * get SELECT query to count the number of triples in a graph
	 * 
	 * @param graph
	 * @return
	 * @throws IOException
	 */
	public static String getCountAllTriplesInGraph(URI graph) throws IOException {
		String sparqlQueryString = readFileFromResources("/queries/select_count_all_triples.rq");

		QuerySolutionMap bindings = new QuerySolutionMap();
		bindings.add("graph", ResourceFactory.createResource(graph.toASCIIString()));

		ParameterizedSparqlString pq = new ParameterizedSparqlString(sparqlQueryString, bindings);
		Query query = QueryFactory.create(pq.toString());
		logger.info("generated query:\n{}", query.toString());

		return query.toString();
	}

	/**
	 * get SELECT query to count the number AnnotationOfAnswer in a graph
	 * 
	 * @param graph
	 * @return
	 * @throws IOException
	 */
	public static String getAllAnnotationOfAnswerInGraph(URI graph) throws IOException {
		String sparqlQueryString = readFileFromResources("/queries/select_all_AnnotationOfAnswerJson.rq");

		QuerySolutionMap bindings = new QuerySolutionMap();
		bindings.add("graph", ResourceFactory.createResource(graph.toASCIIString()));

		ParameterizedSparqlString pq = new ParameterizedSparqlString(sparqlQueryString, bindings);
		Query query = QueryFactory.create(pq.toString());
		logger.info("generated query:\n{}", query.toString());

		return query.toString();
	}

	/**
	 * get SELECT query to get the answer with the highest score in a graph
	 *
	 * @param graph
	 * @return
	 * @throws IOException
	 */
	public static String getHighestScoreAnnotationOfAnswerInGraph(URI graph) throws IOException {
		String sparqlQueryString = readFileFromResources("/queries/select_highestScore_AnnotationOfAnswerSPARQL.rq");

		QuerySolutionMap bindings = new QuerySolutionMap();
		bindings.add("graph", ResourceFactory.createResource(graph.toASCIIString()));

		ParameterizedSparqlString pq = new ParameterizedSparqlString(sparqlQueryString, bindings);
		Query query = QueryFactory.create(pq.toString());
		logger.info("generated query:\n{}", query.toString());

		return query.toString();
	}

	/**
	 * add AnnotationAnswer and AnnotationOfAnswerType to allow annotating typed literals
	 * as it may be required by Qanary QueryBuilder components
	 *
	 * @param bindings
	 * @return
	 * @throws IOException
	 */
	public static String insertAnnotationOfTypedLiteral(QuerySolutionMap bindings) throws IOException {
		return readFileFromResourcesWithMap("/queries/insert_one_AnnotationOfTypedLiteral.rq", bindings);
	}

	/**
	 * add AnnotationOfAnswerSPARQL as it is done typically in Qanary QueryBuilder
	 * components
	 *
	 * @param bindings
	 * @return
	 * @throws IOException
	 */
	public static String insertAnnotationOfAnswerSPARQL(QuerySolutionMap bindings) throws IOException {
		return readFileFromResourcesWithMap("/queries/insert_one_AnnotationOfAnswerSPARQL.rq", bindings);
	}

	/**
	 * add AnnotationOfAnswerJson as it is done typically in Qanary QueryExecutor
	 * components
	 *
	 * @param bindings
	 * @return
	 * @throws IOException
	 */
	public static String insertAnnotationOfAnswerJson(QuerySolutionMap bindings) throws IOException {
		return readFileFromResourcesWithMap("/queries/insert_one_AnnotationOfAnswerJson.rq", bindings);
	}

	public static String getAnnotationOfAnswerSPARQL(QuerySolutionMap bindings) throws IOException {
		return readFileFromResourcesWithMap("/queries/select_all_AnnotationOfAnswerSPARQL.rq", bindings);
	}

	/**
	 * read query from file and apply bindings
	 *
	 * @param filenameWithRelativePath
	 * @param bindings
	 * @return
	 * @throws IOException
	 */
	public static String readFileFromResourcesWithMap(String filenameWithRelativePath, QuerySolutionMap bindings)
			throws IOException {
		String sparqlQueryString = readFileFromResources(filenameWithRelativePath);
		
		if(logger.isDebugEnabled()) {
			logger.debug("readFileFromResourcesWithMap sparqlQueryString: {}", sparqlQueryString);
			String debugMessage = "Try to apply the variables to the SPARQL query template:";
			for( String varName : bindings.asMap().keySet() ) {
				debugMessage += String.format("\n\t%s -> %s", varName, bindings.asMap().get(varName));
			}
			logger.debug(debugMessage);
		}
		
		ParameterizedSparqlString pq = new ParameterizedSparqlString(sparqlQueryString, bindings);
		
		logger.debug("create SPARQL query text before QueryFactory: {}", pq.toString());

		if ((sparqlQueryString).contains("\nSELECT ") || sparqlQueryString.startsWith("SELECT")) {
			Query query = QueryFactory.create(pq.toString());
			logger.info("generated SELECT query:\n{}", query.toString());
			return query.toString();
		} else if (sparqlQueryString.contains("\nASK ") || sparqlQueryString.startsWith("ASK")) {
			Query query = QueryFactory.create(pq.toString());
			logger.info("generated ASK query:\n{}", query.toString());
			return query.toString();
		} else {
			UpdateRequest query = UpdateFactory.create(pq.toString());
			logger.info("generated UPDATE query:\n{}", query.toString());
			query.toString();
			return query.toString();
		}

	}

	/**
	 * ensures that files exists in the resources and are non-empty
	 * 
	 * e.g., useful for component constructors to ensure that SPRARQL query template
	 * files (*.rq) are valid
	 * 
	 * @param filenameInResources
	 */
	public static void guardNonEmptyFileFromResources(String filenameInResources) {
		String message = null;
		try {
			String readFileContent = readFileFromResources(filenameInResources);

			if (readFileContent == null) {
				message = "file content was null (does the file exist?): " + filenameInResources;
			} else if (readFileContent.isBlank()) {
				message = "no content: " + filenameInResources;
			} else {
				return; // ok
			}
			logger.error(message);
			throw new RuntimeException(message);

		} catch (IOException e) {
			// should not happen as readFileContent should always be readable (as null)
			message = "not available: " + filenameInResources;
			logger.error(message);
			throw new RuntimeException(message);
		}
	}

}
