package eu.wdaqua.qanary.commons.triplestoreconnectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link QanaryTripleStoreProxy}: delegation to the external connector, and
 * the "pipeline as component" mode in which reads that find nothing locally are
 * forwarded to the parent pipeline's endpoint with the process graph rewritten.
 * The parent pipeline is played by a {@link LocalSparqlEndpoint}; the external
 * store is the in-memory connector.
 */
class QanaryTripleStoreProxyTest {

    private static final URI CHILD_GRAPH = URI.create("urn:qanary:graph:child");
    private static final URI PARENT_GRAPH = URI.create("urn:qanary:graph:parent");

    private LocalSparqlEndpoint parent;
    private QanaryTripleStoreProxy proxy;

    @BeforeEach
    void setUp() throws Exception {
        parent = new LocalSparqlEndpoint();
        parent.dataset().getNamedModel(PARENT_GRAPH.toString()).add(ResourceFactory.createResource("urn:q"),
                ResourceFactory.createProperty("urn:text"), "What is the capital of Saxony?");
        proxy = new QanaryTripleStoreProxy();
        proxy.setExternalConnector(new QanaryTripleStoreConnectorInMemory());
    }

    @AfterEach
    void tearDown() {
        parent.close();
    }

    private static String selectText(URI graph) {
        return "SELECT ?text FROM <" + graph + "> WHERE { <urn:q> <urn:text> ?text }";
    }

    private void becomeComponentOfParent() throws Exception {
        proxy.setInternalConnector(parent.uri(), "ChildPipeline");
        proxy.setExternalEndpointGraph(CHILD_GRAPH);
        proxy.setInternEndpointGraph(PARENT_GRAPH);
    }

    @Test
    void withoutParentReadsAndWritesGoToTheExternalStore() throws Exception {
        proxy.update("INSERT DATA { GRAPH <" + CHILD_GRAPH + "> { <urn:q> <urn:text> \"local\" } }");

        ResultSet results = proxy.select(selectText(CHILD_GRAPH));

        assertEquals("local", results.next().getLiteral("text").getString());
        assertTrue(proxy.ask("ASK FROM <" + CHILD_GRAPH + "> { <urn:q> <urn:text> \"local\" }"));
        assertFalse(proxy.ask("ASK FROM <" + CHILD_GRAPH + "> { <urn:q> <urn:text> \"other\" }"));
        assertTrue(parent.receivedQueries().isEmpty());
    }

    @Test
    void emptyLocalSelectIsAnsweredByTheParentWithTheParentGraph() throws Exception {
        becomeComponentOfParent();

        ResultSet results = proxy.select(selectText(CHILD_GRAPH));

        assertEquals("What is the capital of Saxony?", results.next().getLiteral("text").getString());
        assertEquals(List.of(selectText(PARENT_GRAPH)), parent.receivedQueries(),
                "the child's graph is replaced by the parent's graph");
    }

    @Test
    void localResultsAreNotForwardedToTheParent() throws Exception {
        becomeComponentOfParent();
        proxy.update("INSERT DATA { GRAPH <" + CHILD_GRAPH + "> { <urn:q> <urn:text> \"local\" } }");

        assertEquals("local", proxy.select(selectText(CHILD_GRAPH)).next().getLiteral("text").getString());
        assertTrue(proxy.ask("ASK FROM <" + CHILD_GRAPH + "> { <urn:q> <urn:text> \"local\" }"));
        assertTrue(parent.receivedQueries().isEmpty());
    }

    @Test
    void falseLocalAskIsAnsweredByTheParent() throws Exception {
        becomeComponentOfParent();

        assertTrue(proxy.ask("ASK FROM <" + CHILD_GRAPH + "> { <urn:q> <urn:text> ?text }"));
        assertEquals(List.of("ASK FROM <" + PARENT_GRAPH + "> { <urn:q> <urn:text> ?text }"),
                parent.receivedQueries());
    }

    @Test
    void withoutKnownGraphsTheQueryIsForwardedUnchanged() throws Exception {
        becomeComponentOfParent();
        proxy.clearEndpointGraphs();

        assertFalse(proxy.select(selectText(CHILD_GRAPH)).hasNext());
        assertFalse(proxy.ask("ASK FROM <" + CHILD_GRAPH + "> { <urn:q> <urn:text> ?text }"));
        assertEquals(List.of(selectText(CHILD_GRAPH), "ASK FROM <" + CHILD_GRAPH + "> { <urn:q> <urn:text> ?text }"),
                parent.receivedQueries());
    }

    @Test
    void updateInternalWritesToTheParent() throws Exception {
        becomeComponentOfParent();

        proxy.updateInternal("INSERT DATA { GRAPH <" + PARENT_GRAPH + "> { <urn:q> <urn:answer> \"Dresden\" } }");

        assertTrue(parent.dataset().getNamedModel(PARENT_GRAPH.toString())
                .contains(ResourceFactory.createResource("urn:q"), ResourceFactory.createProperty("urn:answer"),
                        "Dresden"));
    }

    @Test
    void updateWithGraphAndConstructAreDelegatedToTheExternalConnector() throws Exception {
        QanaryTripleStoreConnector external = mock(QanaryTripleStoreConnector.class);
        Model model = ModelFactory.createDefaultModel();
        when(external.construct("CONSTRUCT WHERE { ?s ?p ?o }")).thenReturn(model);
        when(external.construct("CONSTRUCT WHERE { ?s ?p ?o }", CHILD_GRAPH)).thenReturn(model);
        proxy.setExternalConnector(external);

        // used to recurse into itself forever; must reach the external connector
        proxy.update("INSERT DATA { <urn:s> <urn:p> <urn:o> }", CHILD_GRAPH);

        verify(external).update("INSERT DATA { <urn:s> <urn:p> <urn:o> }", CHILD_GRAPH);
        assertSame(model, proxy.construct("CONSTRUCT WHERE { ?s ?p ?o }"));
        assertSame(model, proxy.construct("CONSTRUCT WHERE { ?s ?p ?o }", CHILD_GRAPH));
    }

    @Test
    void connectIsANoOpAndTheProxyHasNoOwnEndpoint() {
        proxy.connect();
        assertEquals("", proxy.getFullEndpointDescription());
    }
}
