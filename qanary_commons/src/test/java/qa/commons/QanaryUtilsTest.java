package qa.commons;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.jena.query.QueryParseException;
import eu.wdaqua.qanary.exceptions.NotEquivalentSparqlQueriesException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QanaryUtilsTest {
    private QanaryUtils utils;

    @BeforeEach
    public void setup() throws URISyntaxException {
        QanaryMessage message = new QanaryMessage(
                new URI("http://example.org/endpoint"),
                new URI("http://example.org/inGraph"),
                new URI("http://example.org/outGraph"));
        this.utils = new QanaryUtils(message, new NoOpTripleStoreConnector());
    }

    @Test
    /**
     * Test that compareSparqlQueries does not throw an exception when the queries
     * are equivalent but different formatting.
     * 
     * @throws URISyntaxException
     * @throws IOException
     */
    void compareSparqlUpdateQueries_equivalentQueries_doNotThrow() throws IOException {
        String expected = readResource("/qa/commons/update/expected-compare-sparql-0.sparql");
        String actual = readResource("/qa/commons/update/actual-compare-sparql-0.sparql");

        assertDoesNotThrow(() -> QanaryUtils.compareAsUpdate(expected, expected),
                "1a. compareSparqlQueries should not throw an exception: equivalent UPDATE queries." //
                        + "Expected:\n" + expected + "\nActual:\n" + expected);
        assertDoesNotThrow(() -> QanaryUtils.compareSparqlQueries(expected, expected),
                "1b. compareSparqlQueries should not throw an exception: equivalent UPDATE queries." //
                        + "Expected:\n" + expected + "\nActual:\n" + expected);

        assertDoesNotThrow(() -> QanaryUtils.compareAsUpdate(actual, actual),
                "2a. compareSparqlQueries should not throw an exception: equivalent UPDATE queries." //
                        + "Expected:\n" + actual + "\nActual:\n" + actual);
        assertDoesNotThrow(() -> QanaryUtils.compareSparqlQueries(actual, actual),
                "2b. compareSparqlQueries should not throw an exception: equivalent UPDATE queries." //
                        + "Expected:\n" + actual + "\nActual:\n" + actual);

        assertDoesNotThrow(() -> QanaryUtils.compareAsUpdate(expected, actual),
                "3a.compareSparqlQueries should not throw an exception: equivalent UPDATE queries." //
                        + "Expected:\n" + expected + "\nActual:\n" + actual);
        assertDoesNotThrow(() -> QanaryUtils.compareSparqlQueries(expected, actual),
                "3b.compareSparqlQueries should not throw an exception: equivalent UPDATE queries." //
                        + "Expected:\n" + expected + "\nActual:\n" + actual);

        assertDoesNotThrow(() -> QanaryUtils.compareAsUpdate(actual, expected),
                "4a.compareAsSelect should not throw an exception: equivalent UPDATE queries." //
                        + "Expected:\n" + actual + "\nActual:\n" + expected);
        assertDoesNotThrow(() -> QanaryUtils.compareSparqlQueries(actual, expected),
                "4b.compareSparqlQueries should not throw an exception: equivalent UPDATE queries." //
                        + "Expected:\n" + actual + "\nActual:\n" + expected);

        // SELECT queries cannot be parsed as UPDATE queries
        assertThrows(QueryParseException.class, () -> QanaryUtils.compareAsSelect(expected, actual),
                "5. compareAsUpdate should throw a QueryParseException: equivalent UPDATE queries cannot be parsed as SELECT queries." //
                        + "Expected:\n" + expected + "\nActual:\n" + actual);

    }

    @Test
    /**
     * Test that compareSparqlQueries throws an exception when the queries are not
     * equivalent.
     * 
     * @throws URISyntaxException
     * @throws IOException
     */
    void compareSparqlUpdateQueries_notEquivalentQueries_throwException() throws IOException {
        // two queries that are equivalent
        String expected = readResource("/qa/commons/update/expected-compare-sparql-0.sparql");
        String actual = readResource("/qa/commons/update/actual-compare-sparql-0.sparql");

        // two queries that are not equivalent, neither is equivalent to the valid
        // queries
        String falseQuery0 = readResource("/qa/commons/update/sparql-query-not-equivalent-0.sparql");
        String falseQuery1 = readResource("/qa/commons/update/sparql-query-not-equivalent-1.sparql");

        assertThrows(Exception.class, () -> QanaryUtils.compareAsUpdate(falseQuery0, falseQuery1));
        assertThrows(Exception.class, () -> QanaryUtils.compareAsUpdate(falseQuery1, falseQuery0));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(falseQuery0, falseQuery1));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(falseQuery1, falseQuery0));

        assertThrows(Exception.class, () -> QanaryUtils.compareAsUpdate(expected, falseQuery0));
        assertThrows(Exception.class, () -> QanaryUtils.compareAsUpdate(expected, falseQuery1));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(expected, falseQuery0));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(expected, falseQuery1));

        assertThrows(Exception.class, () -> QanaryUtils.compareAsUpdate(actual, falseQuery0));
        assertThrows(Exception.class, () -> QanaryUtils.compareAsUpdate(actual, falseQuery1));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(actual, falseQuery0));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(actual, falseQuery1));

        assertThrows(Exception.class, () -> QanaryUtils.compareAsUpdate(falseQuery0, expected));
        assertThrows(Exception.class, () -> QanaryUtils.compareAsUpdate(falseQuery1, expected));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(falseQuery0, expected));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(falseQuery1, expected));

        assertThrows(Exception.class, () -> QanaryUtils.compareAsUpdate(falseQuery0, actual));
        assertThrows(Exception.class, () -> QanaryUtils.compareAsUpdate(falseQuery1, actual));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(falseQuery0, actual));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(falseQuery1, actual));
    }

    @Test
    /**
     * Test that compareSparqlQueries throws no exception when the queries are
     * equivalent.
     * 
     * @throws IOException
     */
    void compareSparqlSelectQueries_equivalentQueries_doNotThrow() throws IOException {
        String select0a = readResource("/qa/commons/select/select-0a.sparql");
        String select0b = readResource("/qa/commons/select/select-0b.sparql");
        String select0c = readResource("/qa/commons/select/select-0c.sparql");

        assertDoesNotThrow(() -> QanaryUtils.compareAsSelect(select0a, select0b));
        assertDoesNotThrow(() -> QanaryUtils.compareAsSelect(select0b, select0a));
        assertDoesNotThrow(() -> QanaryUtils.compareAsSelect(select0a, select0c));
        assertDoesNotThrow(() -> QanaryUtils.compareAsSelect(select0c, select0a));
        assertDoesNotThrow(() -> QanaryUtils.compareAsSelect(select0b, select0c));
        assertDoesNotThrow(() -> QanaryUtils.compareAsSelect(select0c, select0b));

        assertDoesNotThrow(() -> QanaryUtils.compareSparqlQueries(select0a, select0b));
        assertDoesNotThrow(() -> QanaryUtils.compareSparqlQueries(select0b, select0a));
        assertDoesNotThrow(() -> QanaryUtils.compareSparqlQueries(select0a, select0c));
        assertDoesNotThrow(() -> QanaryUtils.compareSparqlQueries(select0c, select0a));
        assertDoesNotThrow(() -> QanaryUtils.compareSparqlQueries(select0b, select0c));
        assertDoesNotThrow(() -> QanaryUtils.compareSparqlQueries(select0c, select0b));
    }

    @Test
    /**
     * Test that compareSparqlQueries throws an exception when the queries are not
     * equivalent.
     * 
     * @throws IOException
     */
    void compareSparqlSelectQueries_notEquivalentQueries_throwException() throws IOException {
        String select0a = readResource("/qa/commons/select/select-0a.sparql");
        String select0b = readResource("/qa/commons/select/select-0b.sparql");
        String select0c = readResource("/qa/commons/select/select-0c.sparql");
        String select1a = readResource("/qa/commons/select/select-1a.sparql");
        String select1b = readResource("/qa/commons/select/select-1b.sparql");

        assertThrows(Exception.class, () -> QanaryUtils.compareAsSelect(select1a, select1b));
        assertThrows(Exception.class, () -> QanaryUtils.compareAsSelect(select1b, select1a));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(select1a, select1b));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(select1b, select1a));

        assertThrows(Exception.class, () -> QanaryUtils.compareAsSelect(select0a, select1a));
        assertThrows(Exception.class, () -> QanaryUtils.compareAsSelect(select0b, select1b));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(select0a, select1a));
        assertThrows(Exception.class, () -> QanaryUtils.compareSparqlQueries(select0b, select1b));
    }

    /**
     * Minimal no-op implementation to satisfy QanaryUtils construction.
     */
    private static class NoOpTripleStoreConnector extends QanaryTripleStoreConnector {
        @Override
        public void connect() {
            // no-op
        }

        @Override
        public ResultSet select(String sparql) throws SparqlQueryFailed {
            return null;
        }

        @Override
        public boolean ask(String sparql) throws SparqlQueryFailed {
            return false;
        }

        @Override
        public void update(String sparql, URI graph) throws SparqlQueryFailed {
            // no-op
        }

        @Override
        public void update(String sparql) throws SparqlQueryFailed {
            // no-op
        }

        @Override
        public Model construct(String sparql) {
            return null;
        }

        @Override
        public Model construct(String sparql, URI graph) {
            return null;
        }

        @Override
        public String getFullEndpointDescription() {
            return "";
        }
    }

    private String readResource(String path) throws IOException {
        try (InputStream in = this.getClass().getResourceAsStream(path)) {
            assertNotNull(in, "Resource not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
