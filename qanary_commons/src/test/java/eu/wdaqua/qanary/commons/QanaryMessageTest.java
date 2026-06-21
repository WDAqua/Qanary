package eu.wdaqua.qanary.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

class QanaryMessageTest {

    private static final URI ENDPOINT = URI.create("http://localhost:8890/sparql");
    private static final URI IN_GRAPH = URI.create("urn:graph:in");
    private static final URI OUT_GRAPH = URI.create("urn:graph:out");

    @Test
    void twoArgConstructorReusesInGraphAsOutGraph() throws URISyntaxException {
        QanaryMessage m = new QanaryMessage(ENDPOINT, IN_GRAPH);
        assertEquals(ENDPOINT, m.getEndpoint());
        assertEquals(IN_GRAPH, m.getInGraph());
        assertEquals(IN_GRAPH, m.getOutGraph());
    }

    @Test
    void threeArgConstructorStoresAllGraphs() throws URISyntaxException {
        QanaryMessage m = new QanaryMessage(ENDPOINT, IN_GRAPH, OUT_GRAPH);
        assertEquals(ENDPOINT, m.getEndpoint());
        assertEquals(IN_GRAPH, m.getInGraph());
        assertEquals(OUT_GRAPH, m.getOutGraph());
        assertEquals(3, m.getValues().size());
    }

    @Test
    void defaultConstructorThenSetValues() throws URISyntaxException {
        QanaryMessage m = new QanaryMessage();
        m.setValues(ENDPOINT, IN_GRAPH, OUT_GRAPH);
        assertEquals(ENDPOINT, m.getEndpoint());
        assertEquals(OUT_GRAPH, m.getOutGraph());
    }

    @Test
    void jsonStringConstructorParsesValues() throws URISyntaxException {
        String json = "{\"values\":{"
                + "\"urn:qanary#endpoint\":\"http://localhost:8890/sparql\","
                + "\"urn:qanary#inGraph\":\"urn:graph:in\","
                + "\"urn:qanary#outGraph\":\"urn:graph:out\""
                + "}}";
        QanaryMessage m = new QanaryMessage(json);
        assertEquals(ENDPOINT, m.getEndpoint());
        assertEquals(IN_GRAPH, m.getInGraph());
        assertEquals(OUT_GRAPH, m.getOutGraph());
    }

    @Test
    void asJsonStringSerialisesValuesAndRoundTrips() throws URISyntaxException {
        QanaryMessage m = new QanaryMessage(ENDPOINT, IN_GRAPH, OUT_GRAPH);
        String json = m.asJsonString();
        assertTrue(json.contains("urn:qanary#endpoint"));
        assertTrue(json.contains("http://localhost:8890/sparql"));

        QanaryMessage roundTripped = new QanaryMessage(json);
        assertEquals(ENDPOINT, roundTripped.getEndpoint());
        assertEquals(OUT_GRAPH, roundTripped.getOutGraph());
    }
}
