package eu.wdaqua.qanary.commons.triplestoreconnectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtModel;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

/**
 * Tests {@link QanaryTripleStoreConnectorVirtuoso} without a Virtuoso server. The
 * Virtuoso JDBC driver is replaced where it would touch the network: the
 * {@link VirtGraph} connection is a mock, and query executions evaluate the
 * connector's queries on an in-memory Jena model. What is tested is the
 * connector's own behaviour: configuration of the connection, result handling,
 * the retry on Virtuoso's "Read timed out" errors and the error mapping.
 * <p>
 * Failing connection attempts interrupt the test thread before they throw, so
 * that the connector's five-second wait between attempts returns at once.
 */
class QanaryTripleStoreConnectorVirtuosoTest {

    private static final String URL = "jdbc:virtuoso://127.0.0.1:1111";
    private static final String TIMEOUT = "Virtuoso Communications Link Failure: Read timed out";

    private final Model data = ModelFactory.createDefaultModel();
    private final List<List<?>> connectionArguments = new ArrayList<>();
    private MockedConstruction<VirtGraph> connections;
    private MockedStatic<VirtuosoQueryExecutionFactory> queryFactory;
    private QanaryTripleStoreConnectorVirtuoso connector;

    @BeforeEach
    void setUp() {
        data.add(ResourceFactory.createResource("urn:s"), ResourceFactory.createProperty("urn:p"), "o1");
        data.add(ResourceFactory.createResource("urn:s"), ResourceFactory.createProperty("urn:p"), "o2");
        connections = mockConstruction(VirtGraph.class,
                (graph, context) -> connectionArguments.add(context.arguments()));
        queryFactory = mockStatic(VirtuosoQueryExecutionFactory.class);
        connector = new QanaryTripleStoreConnectorVirtuoso(URL, "dba", "secret", 30);
    }

    @AfterEach
    void tearDown() {
        queryFactory.close();
        connections.close();
        Thread.interrupted(); // never leak an interrupt into other tests
    }

    /** let query executions answer from the in-memory data; the first {@code failures} fail */
    private void answerQueriesFromData(List<RuntimeException> failures) {
        AtomicInteger calls = new AtomicInteger();
        queryFactory.when(() -> VirtuosoQueryExecutionFactory.create(any(Query.class), any(VirtGraph.class)))
                .thenAnswer(invocation -> {
                    Query query = invocation.getArgument(0);
                    VirtuosoQueryExecution execution = mock(VirtuosoQueryExecution.class);
                    int call = calls.getAndIncrement();
                    if (call < failures.size()) {
                        when(execution.execSelect()).thenThrow(failures.get(call));
                        when(execution.execAsk()).thenThrow(failures.get(call));
                    } else {
                        when(execution.execSelect()).thenAnswer(x -> QueryExecutionFactory.create(query, data).execSelect());
                        when(execution.execAsk()).thenAnswer(x -> QueryExecutionFactory.create(query, data).execAsk());
                    }
                    return execution;
                });
    }

    private static boolean causeChainMentions(Throwable failure, String text) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (String.valueOf(t.getMessage()).contains(text)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> objects(ResultSet results) {
        List<String> values = new ArrayList<>();
        results.forEachRemaining(row -> values.add(row.getLiteral("o").getString()));
        return values;
    }

    @Test
    void connectsWithTheConfiguredCredentialsAndQueryTimeoutAndReadsAllGraphs() {
        assertEquals(List.of(List.of(URL, "dba", "secret")), connectionArguments);
        verify(connections.constructed().get(0)).setQueryTimeout(30);
        verify(connections.constructed().get(0)).setReadFromAllGraphs(true);
        assertEquals(URL, connector.getVirtuosoUrl());
        String description = connector.getFullEndpointDescription(); // production text has a typo ("tiplestore")
        assertTrue(description.startsWith("Virtuoso ") && description.endsWith(" connected on the endpoint " + URL), description);
    }

    @Test
    void givesUpAfterThreeFailedConnectionAttempts() {
        AtomicInteger attempts = new AtomicInteger();
        connections.close();
        try (MockedConstruction<VirtGraph> refusing = mockConstruction(VirtGraph.class, (graph, context) -> {
            attempts.incrementAndGet();
            Thread.currentThread().interrupt(); // makes the 5 s wait between attempts return at once
            throw new IllegalStateException("Connection refused");
        })) {
            RuntimeException failure = assertThrows(RuntimeException.class,
                    () -> new QanaryTripleStoreConnectorVirtuoso(URL, "dba", "secret", 30));
            assertEquals(3, attempts.get());
            assertTrue(causeChainMentions(failure, "Connection refused"), "the last connection error is the cause");
        } finally {
            connections = mockConstruction(VirtGraph.class); // closed again in tearDown
        }
    }

    @Test
    void selectReturnsRewindableResults() throws Exception {
        answerQueriesFromData(List.of());

        ResultSet results = connector.select("SELECT ?o WHERE { <urn:s> <urn:p> ?o } ORDER BY ?o");

        assertTrue(results instanceof ResultSetRewindable);
        assertEquals(2, ((ResultSetRewindable) results).size());
        assertEquals(List.of("o1", "o2"), objects(results));
    }

    @Test
    void selectReconnectsAndRetriesAfterATimeout() throws Exception {
        answerQueriesFromData(List.of(new RuntimeException(TIMEOUT)));

        ResultSet results = connector.select("SELECT ?o WHERE { <urn:s> <urn:p> ?o } ORDER BY ?o");

        assertEquals(List.of("o1", "o2"), objects(results));
        assertEquals(2, connections.constructed().size(), "the connection was re-established once");
    }

    @Test
    void selectReportsOtherErrorsWithoutRetry() {
        answerQueriesFromData(List.of(new RuntimeException("Syntax error in SPARQL")));

        SparqlQueryFailed failure = assertThrows(SparqlQueryFailed.class,
                () -> connector.select("SELECT ?o WHERE { <urn:s> <urn:p> ?o }"));

        assertEquals(URL, failure.getTriplestore());
        assertEquals("SELECT ?o WHERE { <urn:s> <urn:p> ?o }", failure.getSparqlQuery());
        assertEquals(1, connections.constructed().size(), "no reconnect for non-timeout errors");
    }

    @Test
    void askAnswersAndRetriesAfterATimeout() throws Exception {
        answerQueriesFromData(List.of(new RuntimeException(TIMEOUT)));

        assertTrue(connector.ask("ASK { <urn:s> <urn:p> \"o1\" }"));
        assertFalse(connector.ask("ASK { <urn:s> <urn:p> \"missing\" }"));
        assertEquals(2, connections.constructed().size());
    }

    @Test
    void askReportsOtherErrors() {
        answerQueriesFromData(List.of(new RuntimeException("permission denied")));

        SparqlQueryFailed failure = assertThrows(SparqlQueryFailed.class,
                () -> connector.ask("ASK { ?s ?p ?o }"));
        assertTrue(failure.getMessage().contains("permission denied"));
    }

    @Test
    void updatesAreSentOnTheConnectionAndRetriedAfterATimeout() throws Exception {
        String sparql = "INSERT DATA { GRAPH <urn:g> { <urn:s> <urn:p> <urn:o> } }";
        VirtuosoUpdateRequest request = mock(VirtuosoUpdateRequest.class);
        org.mockito.Mockito.doThrow(new RuntimeException(TIMEOUT)).doNothing().when(request).exec();
        try (MockedStatic<VirtuosoUpdateFactory> updateFactory = mockStatic(VirtuosoUpdateFactory.class)) {
            List<String> sent = new ArrayList<>();
            updateFactory.when(() -> VirtuosoUpdateFactory.create(anyString(), any(VirtGraph.class)))
                    .thenAnswer(invocation -> {
                        sent.add(invocation.getArgument(0));
                        return request;
                    });

            connector.update(sparql);

            assertEquals(List.of(sparql, sparql), sent, "sent again after the timeout");
            verify(request, times(2)).exec();
            assertEquals(2, connections.constructed().size());
        }
    }

    @Test
    void updateReportsOtherErrors() {
        VirtuosoUpdateRequest request = mock(VirtuosoUpdateRequest.class);
        org.mockito.Mockito.doThrow(new RuntimeException("SR133: Can not insert")).when(request).exec();
        try (MockedStatic<VirtuosoUpdateFactory> updateFactory = mockStatic(VirtuosoUpdateFactory.class)) {
            updateFactory.when(() -> VirtuosoUpdateFactory.create(anyString(), any(VirtGraph.class))).thenReturn(request);

            SparqlQueryFailed failure = assertThrows(SparqlQueryFailed.class,
                    () -> connector.update("CLEAR GRAPH <urn:g>", URI.create("urn:g")));

            assertEquals("CLEAR GRAPH <urn:g>", failure.getSparqlQuery());
            assertTrue(failure.getMessage().contains("SR133"));
        }
    }

    @Test
    void constructOpensTheRequestedGraphWithTheConfiguredCredentials() {
        String sparql = "CONSTRUCT { ?s <urn:copy> ?o } WHERE { ?s <urn:p> ?o }";
        VirtModel graphModel = mock(VirtModel.class);
        try (MockedStatic<VirtModel> models = mockStatic(VirtModel.class)) {
            models.when(() -> VirtModel.openDatabaseModel("urn:g", URL, "dba", "secret")).thenReturn(graphModel);
            queryFactory.when(() -> VirtuosoQueryExecutionFactory.create(sparql, (Model) graphModel))
                    .thenAnswer(invocation -> QueryExecutionFactory.create(sparql, data));

            Model result = connector.construct(sparql, URI.create("urn:g"));

            assertEquals(2, result.size());
            assertTrue(result.contains(ResourceFactory.createResource("urn:s"),
                    ResourceFactory.createProperty("urn:copy"), "o1"));
            models.verify(() -> VirtModel.openDatabaseModel("urn:g", URL, "dba", "secret"));
            queryFactory.verify(() -> VirtuosoQueryExecutionFactory.create(sparql, (Model) graphModel));
        }
    }
}
