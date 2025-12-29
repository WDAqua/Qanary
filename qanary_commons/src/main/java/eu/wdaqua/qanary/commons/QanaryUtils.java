package eu.wdaqua.qanary.commons;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.exceptions.NotEquivalentSparqlQueriesException;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.sparql.modify.request.UpdateModify;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import java.util.Comparator;
import java.util.Arrays;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;

/**
 * the class is a covering standard tasks users of the Qanary methodology might
 * have
 * <p>
 * if you are missing helper methods than please request them via @see
 * <a href="https://github.com/WDAqua/Qanary/issues/new">Github</a>
 *
 * @author AnBo
 */
public class QanaryUtils {

    private static final Logger logger = LoggerFactory.getLogger(QanaryUtils.class);

    private final URI endpoint;
    private final URI inGraph;
    private final URI outGraph;
    private final QanaryTripleStoreConnector qanaryTripleStoreConnector;

    public QanaryUtils(QanaryMessage qanaryMessage, QanaryTripleStoreConnector myQanaryTripleStoreConnector) {
        this.endpoint = qanaryMessage.getEndpoint();
        this.inGraph = qanaryMessage.getInGraph();
        this.outGraph = qanaryMessage.getOutGraph();
        this.qanaryTripleStoreConnector = myQanaryTripleStoreConnector;
    }

    public QanaryUtils(QanaryQuestionAnsweringRun qanaryRun, QanaryTripleStoreConnector myQanaryTripleStoreConnector) {
        this.endpoint = qanaryRun.getEndpoint();
        this.inGraph = qanaryRun.getInGraph();
        this.outGraph = qanaryRun.getOutGraph();
        this.qanaryTripleStoreConnector = myQanaryTripleStoreConnector;
    }

    /**
     * get current time in milliseconds
     */
    public static long getTime() {
        return System.currentTimeMillis();
    }

    /**
     * returns the endpoint provided by the QanaryMessage object provided via
     * constructor
     */
    public URI getEndpoint() {
        return this.endpoint;
    }

    /**
     * returns the inGraph provided by the QanaryMessage object provided via
     * constructor
     */
    public URI getInGraph() {
        return this.inGraph;
    }

    /**
     * returns the outGraph provided by the QanaryMessage object provided via
     * constructor
     */
    public URI getOutGraph() {
        return this.outGraph;
    }

    /**
     * returns the instance of QanaryTripleStoreConnector
     *
     * @return
     */
    public QanaryTripleStoreConnector getQanaryTripleStoreConnector() {
        return this.qanaryTripleStoreConnector;
    }

    /**
     * query a SPARQL endpoint with a given SELECT query
     *
     * @throws SparqlQueryFailed
     */
    @Deprecated
    public ResultSet selectFromTripleStore(String sparqlQuery, String endpoint)
            throws SparqlQueryFailed, URISyntaxException {
        logger.warn(
                "This method is deprecated and will be removed in future versions. Please use the method QanaryTripleStoreConnector.select(String sparqlQuery) instead.");
        logger.warn("The parameter 'endpoint' is not longer used in this method !!!");
        return this.qanaryTripleStoreConnector.select(sparqlQuery);
    }

    /**
     * query a SPARQL endpoint with a given ASK query
     *
     * @throws SparqlQueryFailed
     */
    @Deprecated
    public boolean askTripleStore(String sparqlQuery, String endpoint) throws SparqlQueryFailed {
        logger.warn(
                "This method is deprecated and will be removed in future versions. Please use the method QanaryTripleStoreConnector.ask(String sparqlQuery) instead.");
        logger.warn("The parameter 'endpoint' is not longer used in this method !!!");
        return this.qanaryTripleStoreConnector.ask(sparqlQuery);
    }

    /**
     * wrapper for updateTripleStore
     *
     * @throws URISyntaxException
     * @throws SparqlQueryFailed
     */
    @Deprecated
    public void updateTripleStore(String sparqlQuery, QanaryConfigurator myQanaryConfigurator)
            throws URISyntaxException, SparqlQueryFailed {
        this.updateTripleStore(sparqlQuery, myQanaryConfigurator.getEndpoint());
    }

    /**
     * wrapper for updateTripleStore
     *
     * @param sparqlQuery
     * @param endpoint
     * @throws SparqlQueryFailed
     */
    @Deprecated
    public void updateTripleStore(String sparqlQuery, URL endpoint) throws SparqlQueryFailed {
        logger.warn(
                "This method is deprecated and will be removed in future versions. Please use the method QanaryTripleStoreConnector.update(String sparqlQuery) instead.");
        this.updateTripleStore(sparqlQuery, endpoint.toString());
    }

    /**
     * wrapper for updateTripleStore
     *
     * @param sparqlQuery
     * @param endpoint
     * @throws SparqlQueryFailed
     */
    @Deprecated
    public void updateTripleStore(String sparqlQuery, URI endpoint) throws SparqlQueryFailed {
        logger.warn(
                "This method is deprecated and will be removed in future versions. Please use the method QanaryTripleStoreConnector.update(String sparqlQuery) instead.");
        this.updateTripleStore(sparqlQuery, endpoint.toString());
    }

    /**
     * insert data into triplestore
     *
     * @throws SparqlQueryFailed
     */
    @Deprecated
    public void updateTripleStore(String sparqlQuery, String endpoint) throws SparqlQueryFailed {
        logger.warn(
                "This method is deprecated and will be removed in future versions. Please use the method QanaryTripleStoreConnector.update(String sparqlQuery) instead.");
        logger.debug("updateTripleStore on {}: {}", endpoint, sparqlQuery);
        this.getQanaryTripleStoreConnector().update(sparqlQuery);
    }

    /**
     * wrapper for retrieving the URI where the service is currently running
     */
    public String getComponentUri() {
        return QanaryConfiguration.getServiceUri().toString();
    }

    /**
     * wrapper for retrieving the URI where the service is currently running
     */
    public String getHostUri() {
        return QanaryConfiguration.getHostUri().toString();
    }

    /**
     * @param description
     * @param duration
     */
    private void logTime(long duration, String description) {
        logger.debug("runtime measurement: {} ms for {}", duration, description);
    }

    public static void compareSparqlQueries(String expectedQueryString, String actualQueryString)
            throws NotEquivalentSparqlQueriesException, QueryParseException {

        logger.debug("compareSparqlQueries -- expected query:\n{}", expectedQueryString);
        logger.debug("compareSparqlQueries -- actual query:\n{}", actualQueryString);
        try {
            compareAsSelect(expectedQueryString, actualQueryString);
            return;
        } catch (QueryParseException e1) {
            logger.debug(
                    "compareSparqlQueries: queries could not be parsed as SELECT queries, try with UPDATE query parsing",
                    e1);
        } catch (NotEquivalentSparqlQueriesException e2) {
            logger.debug("compareSparqlQueries: queries are not equivalent", e2);
            throw e2;
        } catch (Exception e3) {
            logger.debug("compareSparqlQueries: unexpected exception", e3);
            throw e3;
        }

        try {
            compareAsUpdate(expectedQueryString, actualQueryString);
        } catch (QueryParseException e4) {
            logger.debug("compareSparqlQueries: queries could not be parsed as UPDATE queries", e4);
            throw e4;
        } catch (NotEquivalentSparqlQueriesException e5) {
            logger.debug("compareSparqlQueries: UPDATE queries are not equivalent", e5);
            throw e5;
        } catch (Exception e6) {
            logger.debug("compareSparqlQueries: unexpected exception", e6);
            throw e6;
        }
    }

    /**
     * Try comparing queries as SELECT/ASK/CONSTRUCT/DESCRIBE. Returns true if
     * comparison completed.
     * 
     * @throws NotEquivalentSparqlQueriesException
     */
    public static void compareAsSelect(String expectedQueryString, String actualQueryString)
            throws NotEquivalentSparqlQueriesException, QueryParseException {
        try {
            logger.debug("compareAsSelect: try SELECT query parsing");
            Query expectedParsedQuery = QueryFactory.create(expectedQueryString);
            Op expectedOp = Algebra.compile(expectedParsedQuery);
            Query expectedQueryAlgebra = OpAsQuery.asQuery(expectedOp);

            Query actualParsedQuery = QueryFactory.create(actualQueryString);
            Op actualOp = Algebra.compile(actualParsedQuery);
            Query actualQueryAlgebra = OpAsQuery.asQuery(actualOp);

            logger.debug("expected SELECT query after algebra compilation: {}", expectedQueryAlgebra.serialize());
            logger.debug("actual SELECT query after algebra compilation: {}", actualQueryAlgebra.serialize());
            if (!expectedQueryAlgebra.serialize().equals(actualQueryAlgebra.serialize())) {
                throw new NotEquivalentSparqlQueriesException(
                        "The SELECT queries do not match:\nexpected query\n" + expectedQueryString + "\nactual query\n"
                                + actualQueryString,
                        expectedQueryString, actualQueryString);
            }
        } catch (QueryParseException e) {
            logger.debug("compareAsSelect: queries could not be parsed as SELECT queries", e);
            throw e;
        }
    }

    /**
     * Compare queries as SPARQL UPDATE statements (INSERT/DELETE/etc.).
     */
    public static void compareAsUpdate(String expectedUpdateQueryString, String actualUpdateQueryString)
            throws NotEquivalentSparqlQueriesException, QueryParseException {
        logger.debug("compareAsUpdate: falling back to UPDATE query parsing");
        try {
            logger.debug("compareAsUpdate: try UPDATE query parsing");

            // parse the queries
            UpdateRequest expectedParsedUpdate = UpdateFactory.create(expectedUpdateQueryString);
            UpdateRequest actualParsedUpdate = UpdateFactory.create(actualUpdateQueryString);

            String expectedParsedUpdateString = expectedParsedUpdate.toString();
            String actualParsedUpdateString = actualParsedUpdate.toString();

            /**
             * Heuristic to normalize the queries for comparison.
             */

            // replace all newlines with spaces in the queries iteratively
            while (expectedParsedUpdateString.contains("\n")) {
                expectedParsedUpdateString = expectedParsedUpdateString.replace("\n", " ");
            }
            while (actualParsedUpdateString.contains("\n")) {
                actualParsedUpdateString = actualParsedUpdateString.replace("\n", " ");
            }

            // replace all tabs with spaces in the queries iteratively
            while (expectedParsedUpdateString.contains("\t")) {
                expectedParsedUpdateString = expectedParsedUpdateString.replace("\t", " ");
            }
            while (actualParsedUpdateString.contains("\t")) {
                actualParsedUpdateString = actualParsedUpdateString.replace("\t", " ");
            }

            // replace all carriage returns with spaces in the queries iteratively
            while (expectedParsedUpdateString.contains("\r")) {
                expectedParsedUpdateString = expectedParsedUpdateString.replace("\r", " ");
            }
            while (actualParsedUpdateString.contains("\r")) {
                actualParsedUpdateString = actualParsedUpdateString.replace("\r", " ");
            }

            // replace all form feeds with spaces in the queries iteratively
            while (expectedParsedUpdateString.contains("\f")) {
                expectedParsedUpdateString = expectedParsedUpdateString.replace("\f", " ");
            }
            while (actualParsedUpdateString.contains("\f")) {
                actualParsedUpdateString = actualParsedUpdateString.replace("\f", " ");
            }

            // replace all non-printable characters with spaces in the queries
            while (expectedParsedUpdateString.contains("\u0000")) {
                expectedParsedUpdateString = expectedParsedUpdateString.replace("\u0000", " ");
            }
            while (actualParsedUpdateString.contains("\u0000")) {
                actualParsedUpdateString = actualParsedUpdateString.replace("\u0000", " ");
            }

            // replace all double space with single space in the queries iteratively
            while (expectedParsedUpdateString.contains("  ")) {
                expectedParsedUpdateString = expectedParsedUpdateString.replace("  ", " ");
            }
            while (actualParsedUpdateString.contains("  ")) {
                actualParsedUpdateString = actualParsedUpdateString.replace("  ", " ");
            }

            // make it lowercase to avoid case-sensitive comparisons
            expectedParsedUpdateString = expectedParsedUpdateString.toLowerCase();
            actualParsedUpdateString = actualParsedUpdateString.toLowerCase();

            // split the queries into lines at the spaces and compare the sorted lines
            String[] expectedLines = Arrays.stream(expectedParsedUpdateString.split(" "))
                    .sorted(Comparator.naturalOrder()).toArray(String[]::new);
            String[] actualLines = Arrays.stream(actualParsedUpdateString.split(" "))
                    .sorted(Comparator.naturalOrder()).toArray(String[]::new);
            for (int i = 0; i < expectedLines.length; i++) {
                if (!expectedLines[i].equals(actualLines[i])) {
                    throw new NotEquivalentSparqlQueriesException(
                            "The UPDATE queries do not match:\nexpected query\n" + expectedUpdateQueryString
                                    + "\nactual query\n"
                                    + actualUpdateQueryString,
                            expectedUpdateQueryString, actualUpdateQueryString);
                }
            }

        } catch (QueryParseException e) {
            logger.debug("compareAsUpdate: queries could not be parsed as UPDATE queries", e);
            throw e;
        } catch (NotEquivalentSparqlQueriesException e) {
            logger.debug("compareAsUpdate: UPDATE queries parsed but are not equivalent", e);
            throw e;
        } catch (Exception e) {
            logger.debug("compareAsUpdate: unexpected exception", e);
            throw e;
        }
    }

    private static boolean quadSetsEqual(Iterable<Quad> expected, Iterable<Quad> actual) {
        Set<String> expectedSet = new HashSet<>();
        for (Quad q : expected) {
            expectedSet.add(NodeFmtLib.str(q));
        }
        Set<String> actualSet = new HashSet<>();
        for (Quad q : actual) {
            actualSet.add(NodeFmtLib.str(q));
        }
        return expectedSet.equals(actualSet);
    }
}
