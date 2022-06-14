package eu.wdaqua.qanary.commons.triplestoreconnectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.stream.Collectors;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

public abstract class QanaryTripleStoreConnector {
	private static final Logger logger = LoggerFactory.getLogger(QanaryTripleStoreConnector.class);

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
		logger.info("runtime measurement: {} ms for {}", duration, description);
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
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		return reader.lines().collect(Collectors.joining());
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

}
