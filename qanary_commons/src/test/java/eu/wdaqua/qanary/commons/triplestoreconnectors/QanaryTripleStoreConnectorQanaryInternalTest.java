package eu.wdaqua.qanary.commons.triplestoreconnectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link QanaryTripleStoreConnectorQanaryInternal} against a real SPARQL
 * protocol endpoint on the loopback interface ({@link LocalSparqlEndpoint}): the
 * queries and updates really travel over HTTP, and the effects are checked in the
 * endpoint's data.
 */
class QanaryTripleStoreConnectorQanaryInternalTest {

    private static final String GRAPH = "urn:qanary:test:graph";
    private static final String QA = "http://www.wdaqua.eu/qa#";
    private static final String OA = "http://www.w3.org/ns/openannotation/core/";

    private LocalSparqlEndpoint endpoint;
    private QanaryTripleStoreConnectorQanaryInternal connector;

    @BeforeEach
    void start() throws Exception {
        endpoint = new LocalSparqlEndpoint();
        Model graph = endpoint.dataset().getNamedModel(GRAPH);
        graph.add(ResourceFactory.createResource("urn:s"), ResourceFactory.createProperty("urn:p"),
                ResourceFactory.createResource("urn:o1"));
        graph.add(ResourceFactory.createResource("urn:s"), ResourceFactory.createProperty("urn:p"),
                ResourceFactory.createResource("urn:o2"));
        connector = new QanaryTripleStoreConnectorQanaryInternal(endpoint.uri(), "TestComponent");
    }

    @AfterEach
    void stop() {
        endpoint.close();
    }

    private boolean endpointHas(String askQuery) {
        try (QueryExecution execution = QueryExecutionFactory.create(askQuery, endpoint.dataset())) {
            return execution.execAsk();
        }
    }

    @Test
    void applicationNameIsPrefixedAsQanaryUrn() {
        assertEquals("urn:qanary:TestComponent", connector.getApplicationName());
    }

    @Test
    void descriptionNamesTheEndpoint() {
        assertEquals("simple open endpoint without authorization: " + endpoint.uri(),
                connector.getFullEndpointDescription());
    }

    @Test
    void updatesAreExecutedOnTheRemoteEndpoint() throws Exception {
        connector.update("INSERT DATA { GRAPH <" + GRAPH + "> { <urn:new> <urn:p> <urn:o> } }");
        // the variant with a graph argument sends the update unchanged as well
        connector.update("INSERT DATA { GRAPH <" + GRAPH + "> { <urn:other> <urn:p> <urn:o> } }",
                URI.create(GRAPH));

        assertEquals(2, endpoint.receivedUpdates().size());
        assertTrue(endpointHas("ASK { GRAPH <" + GRAPH + "> { <urn:new> <urn:p> <urn:o> } }"));
        assertTrue(endpointHas("ASK { GRAPH <" + GRAPH + "> { <urn:other> <urn:p> <urn:o> } }"));
    }

    @Test
    void selectReturnsTheRemoteRowsAndLogsTheQueryAsAnnotation() throws Exception {
        String sparql = "SELECT ?o FROM <" + GRAPH + "> WHERE { <urn:s> <urn:p> ?o } ORDER BY ?o";

        ResultSet results = connector.select(sparql);

        List<String> objects = new ArrayList<>();
        results.forEachRemaining(row -> objects.add(row.getResource("o").getURI()));
        assertEquals(List.of("urn:o1", "urn:o2"), objects);
        assertEquals(List.of(sparql), endpoint.receivedQueries());
        // the query is logged into the graph named in its FROM clause, credited to the component
        assertEquals(1, endpoint.receivedUpdates().size());
        assertTrue(endpointHas("PREFIX qa: <" + QA + "> PREFIX oa: <" + OA + "> ASK { GRAPH <" + GRAPH + "> {"
                + " ?a a qa:AnnotationOfLogQuery ; oa:annotatedBy <urn:qanary:TestComponent> ;"
                + " oa:hasTarget <urn:qanary:currentQuestion> ; oa:hasBody ?body ; oa:annotatedAt ?time ."
                + " FILTER(STR(?body) = \"" + sparql.replace("\"", "\\\"") + "\") } }"));
    }

    @Test
    void askAnswersFromTheRemoteEndpointAndLogsTheQuery() throws Exception {
        assertTrue(connector.ask("ASK FROM <" + GRAPH + "> { <urn:s> <urn:p> <urn:o1> }"));
        assertFalse(connector.ask("ASK FROM <" + GRAPH + "> { <urn:s> <urn:p> <urn:missing> }"));

        assertEquals(2, endpoint.receivedQueries().size());
        assertEquals(2, endpoint.receivedUpdates().size(), "every ASK is logged as well");
    }

    @Test
    void aFailingExplanationLogDoesNotBreakTheQuery() throws Exception {
        endpoint.rejectUpdates();

        ResultSet results = connector.select("SELECT ?o FROM <" + GRAPH + "> WHERE { <urn:s> <urn:p> ?o }");

        assertEquals(1, endpoint.receivedUpdates().size(), "the log was attempted");
        int rows = 0;
        while (results.hasNext()) {
            results.next();
            rows++;
        }
        assertEquals(2, rows, "the query result is delivered although logging failed");
    }

    @Test
    void constructIsNotSupported() {
        assertNull(connector.construct("CONSTRUCT WHERE { ?s ?p ?o }"));
        assertNull(connector.construct("CONSTRUCT WHERE { ?s ?p ?o }", URI.create(GRAPH)));
    }
}
