package eu.wdaqua.qanary.commons.triplestoreconnectors;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

public abstract class QanaryTripleStoreConnector {
    private static final Logger logger = LoggerFactory.getLogger(QanaryTripleStoreConnector.class);

    private static final long MAX_ACCEPTABLE_QUERY_EXECUTION_TIME = 10000;

    /**
     * Get current time in milliseconds.
     */
    protected static long getTime() {
        return System.currentTimeMillis();
    }

    protected static Logger getLogger() {
        return logger;
    }

    /**
     * Read SPARQL query from files in resources folder.
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
     * Get SELECT query to count the number of triples in a graph.
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
     * Get SELECT query to count the number AnnotationOfAnswer in a graph.
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
     * Get SELECT query to get the answer SPARQL with the highest score in a graph.
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
     * Get SELECT query to get the answer SPARQL with the lowest index in a graph <br/>
     * (for implementations where order of created query candidates matters).
     *
     * @param graph
     * @return
     * @throws IOException
     */
    public static String getLowestIndexAnnotationOfAnswerInGraph(URI graph) throws IOException {
        String sparqlQueryString = readFileFromResources("/queries/select_lowestIndex_AnnotationOfAnswerSPARQL.rq");

        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource(graph.toASCIIString()));

        ParameterizedSparqlString pq = new ParameterizedSparqlString(sparqlQueryString, bindings);
        Query query = QueryFactory.create(pq.toString());
        logger.info("generated query:\n{}", query.toString());

        return query.toString();
    }

    /**
     * Get SELECT query to get all answer SPARQL in a graph.
     *
     * @param graph
     * @return
     * @throws IOException
     */
    public static String getAllAnnotationOfAnswerSPARQL(URI graph) throws IOException {
        String sparqlQueryString = readFileFromResources("/queries/select_all_AnnotationOfAnswerSPARQL.rq");

        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource(graph.toASCIIString()));

        ParameterizedSparqlString pq = new ParameterizedSparqlString(sparqlQueryString, bindings);
        Query query = QueryFactory.create(pq.toString());
        logger.info("generated query:\n{}", query.toString());

        return query.toString();
    }

    /**
     * Get INSERT query for AnnotationAnswer and AnnotationOfAnswerType to annotate typed literals.
     *
     * @param bindings
     * @return
     * @throws IOException
     * @deprecated Annotation of literal values using AnnotationAnswer, as it is done in this method, is
     * discouraged. Use {@link #insertAnnotationOfAnswerJson(QuerySolutionMap)} instead, to annotate the
     * result JSON of a SPARQL query.
     */
    @Deprecated
    public static String insertAnnotationOfTypedLiteral(QuerySolutionMap bindings) throws IOException {
        return readFileFromResourcesWithMap("/queries/insert_one_AnnotationOfTypedLiteral.rq", bindings);
    }

    /**
     * Get INSERT query to annotate the answer data type.
     *
     * @param bindings required bindings for the query: <br/>
     *                 ?graph, ?targetQuestion, ?answerDataType, ?confidence, ?application
     * @return
     * @throws IOException
     */
    public static String insertAnnotationOfAnswerDataType(QuerySolutionMap bindings) throws IOException {
        return readFileFromResourcesWithMap("/queries/insert_one_AnnotationOfAnswerDataType.rq", bindings);
    }

    /**
     * get INSERT query to annotate SPARQL query that should compute the answer.
     *
     * @param bindings required bindings for the query: <br/>
     *                 ?graph, ?targetQuestion, ?selectQueryThatShouldComputeTheAnswer, <br/>
     *                 ?confidence, ?index, ?application
     * @return
     * @throws IOException
     */
    public static String insertAnnotationOfAnswerSPARQL(QuerySolutionMap bindings) throws IOException {
        return readFileFromResourcesWithMap("/queries/insert_one_AnnotationOfAnswerSPARQL.rq", bindings);
    }

    /**
     * Get INSERT query to annotate answer JSON.
     *
     * @param bindings required bindings for the query: <br/>
     *                 ?graph, ?targetQuestion, ?jsonAnswer, ?confidence, ?application
     * @return
     * @throws IOException
     */
    public static String insertAnnotationOfAnswerJson(QuerySolutionMap bindings) throws IOException {
        return readFileFromResourcesWithMap("/queries/insert_one_AnnotationOfAnswerJson.rq", bindings);
    }

    /**
     * Get INSERT query to annotate an improved question.
     *
     * @param bindings required bindings for the query: <br/>
     *                 ?graph, ?targetQuestion, ?improvedQuestionText, ?confidence, ?application
     * @return
     * @throws IOException
     */
    public static String insertAnnotationOfImprovedQuestion(QuerySolutionMap bindings) throws IOException {
        return readFileFromResourcesWithMap("/queries/insert_one_AnnotationOfImprovedQuestion.rq", bindings);
    }

    /**
     * Read query from file and apply bindings.
     *
     * @param filenameWithRelativePath
     * @param bindings
     * @return
     * @throws IOException
     */
    public static String readFileFromResourcesWithMap(String filenameWithRelativePath, QuerySolutionMap bindings)
            throws IOException {
        String sparqlQueryString = readFileFromResources(filenameWithRelativePath);

        if (logger.isDebugEnabled()) {
            logger.debug("readFileFromResourcesWithMap sparqlQueryString: {}", sparqlQueryString);
            String debugMessage = "Try to apply the variables to the SPARQL query template:";
            for (String varName : bindings.asMap().keySet()) {
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
        } else if (sparqlQueryString.contains("\nCONSTRUCT") || sparqlQueryString.startsWith("CONSTRUCT")) {
            Query query = QueryFactory.create(pq.toString()); // TODO: Lookup possibility to create SPARQL and use model for virtuoso only
            logger.info("generated CONSTRUCT query: \n{}", query.toString());
            return query.toString();
        } else {
            UpdateRequest query = UpdateFactory.create(pq.toString());
            logger.info("generated UPDATE query:\n{}", query.toString());
            query.toString();
            return query.toString();
        }

    }

    /**
     * Ensures that files exists in the resources and are non-empty.
     * <p>
     * Useful for component constructors to ensure that SPRARQL query template
     * files (*.rq) are valid.
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

    public abstract void connect(); // TODO: add exception

    public abstract ResultSet select(String sparql) throws SparqlQueryFailed;

    public abstract boolean ask(String sparql) throws SparqlQueryFailed;

    public abstract void update(String sparql, URI graph) throws SparqlQueryFailed;

    public abstract void update(String sparql) throws SparqlQueryFailed;

    public abstract Model construct(String sparql);

    public abstract Model construct(String sparql, URI graph);

    /**
     * Return a readable description of the triplestore endpoint.
     *
     * @return
     */
    public abstract String getFullEndpointDescription();

    /**
     * @param description
     * @param duration
     */
    protected void logTime(long duration, String description) {
        if (duration > MAX_ACCEPTABLE_QUERY_EXECUTION_TIME) {
            logger.warn("runtime measurement: {} ms for {} (was very long)", duration, description);
        } else {
            logger.info("runtime measurement: {} ms for {}", duration, description);
        }
    }

    /**
     * @param duration
     * @param description
     */
    protected void logTime(long duration, String description, String endpoint) {
        logger.info("runtime measurement: {} ms on {} for {}", duration, endpoint, description);
    }

}
