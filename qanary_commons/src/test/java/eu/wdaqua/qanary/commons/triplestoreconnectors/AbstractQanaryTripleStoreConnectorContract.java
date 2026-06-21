package eu.wdaqua.qanary.commons.triplestoreconnectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.UUID;

import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

/**
 * Conformance contract that every {@link QanaryTripleStoreConnector} must satisfy.
 * Subclasses supply a concrete connector via {@link #connector()} and the same
 * tests run against each implementation, pinning a single, consistent behaviour.
 * <p>
 * The contract deliberately covers only the operations that behave identically
 * across all connectors: {@code update(String)} (insert) and {@code select(String)}
 * (including a count query). It does NOT include {@code ask(...)} absence,
 * named-graph isolation, {@code construct(...)} or delete, because those diverge
 * between implementations (e.g. the in-memory connector does not implement
 * {@code construct}; the Virtuoso/VirtGraph driver's ASK and named-graph semantics
 * are non-standard — see this package's README.adoc). Each test uses a unique
 * subject IRI so the assertions are independent of residual store state.
 */
abstract class AbstractQanaryTripleStoreConnectorContract {

    protected static final URI GRAPH = URI.create("urn:qanary:contract:graph");

    /** the connector under test (subclasses provide an initialised instance) */
    protected abstract QanaryTripleStoreConnector connector();

    private String subject;

    @BeforeEach
    void freshSubject() {
        subject = "urn:qanary:contract:s:" + UUID.randomUUID();
    }

    private void insert(String s) throws SparqlQueryFailed {
        connector().update("INSERT DATA { GRAPH <" + GRAPH + "> { <" + s + "> <urn:p> <urn:o> . } }");
    }

    @Test
    void insertedTripleIsSelectable() throws SparqlQueryFailed {
        insert(subject);
        ResultSet rs = connector().select(
                "SELECT ?o WHERE { GRAPH <" + GRAPH + "> { <" + subject + "> <urn:p> ?o } }");
        assertTrue(rs.hasNext(), "the inserted triple must be selectable");
        assertEquals("urn:o", rs.next().getResource("o").getURI());
    }

    @Test
    void absentSubjectSelectsNothing() throws SparqlQueryFailed {
        ResultSet rs = connector().select(
                "SELECT ?o WHERE { GRAPH <" + GRAPH + "> { <urn:qanary:contract:absent:"
                        + UUID.randomUUID() + "> <urn:p> ?o } }");
        assertFalse(rs.hasNext(), "a never-inserted subject must select nothing");
    }

    @Test
    void multipleInsertsAreAllSelectable() throws SparqlQueryFailed {
        String s1 = subject + ":a";
        String s2 = subject + ":b";
        insert(s1);
        insert(s2);
        for (String s : new String[] {s1, s2}) {
            ResultSet rs = connector().select(
                    "SELECT ?o WHERE { GRAPH <" + GRAPH + "> { <" + s + "> <urn:p> ?o } }");
            assertTrue(rs.hasNext(), "each inserted triple must be selectable: " + s);
        }
    }

    @Test
    void countIsAtLeastOneAfterInsert() throws Exception {
        insert(subject);
        ResultSet rs = connector().select(QanaryTripleStoreConnector.getCountAllTriplesInGraph(GRAPH));
        assertTrue(rs.hasNext());
        long count = rs.next().get("count").asLiteral().getLong();
        assertTrue(count >= 1, "after an insert the graph holds at least one triple, was " + count);
    }
}
