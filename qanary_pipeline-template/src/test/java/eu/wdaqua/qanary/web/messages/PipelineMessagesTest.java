package eu.wdaqua.qanary.web.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.Test;

class PipelineMessagesTest {

    // ---- AdditionalInsertQuery ----------------------------------------------

    @Test
    void additionalInsertQueryAcceptsValidInsert() {
        String query = "INSERT DATA { GRAPH <urn:g> { <urn:s> <urn:p> <urn:o> } }";
        AdditionalInsertQuery aiq = new AdditionalInsertQuery(query);
        assertNotNull(aiq.getInsertQuery());
        assertTrue(aiq.getInsertQuery().toUpperCase().contains("INSERT"));
    }

    @Test
    void additionalInsertQueryAddsBindsForVariables() {
        String query = "INSERT { GRAPH ?g { ?s ?p ?o } }";
        AdditionalInsertQuery aiq = new AdditionalInsertQuery(query);
        assertNotNull(aiq.getInsertQuery());
        assertTrue(aiq.getInsertQuery().contains("BIND"));
    }

    @Test
    void additionalInsertQueryHandlesVariableNotFollowedBySpace() {
        // a variable immediately followed by '}' (no trailing space) must not throw
        // a StringIndexOutOfBoundsException while scanning variables (regression fix)
        String query = "INSERT { GRAPH ?g { ?s ?p ?o} }";
        AdditionalInsertQuery aiq = new AdditionalInsertQuery(query);
        assertNotNull(aiq.getInsertQuery());
        assertTrue(aiq.getInsertQuery().contains("BIND"));
    }

    @Test
    void additionalInsertQueryRejectsForbiddenKeywords() {
        // DELETE/CLEAR/LOAD are forbidden -> internally raises and is swallowed -> null
        assertNull(new AdditionalInsertQuery("DELETE WHERE { ?s ?p ?o }").getInsertQuery());
        assertNull(new AdditionalInsertQuery("CLEAR GRAPH <urn:g>").getInsertQuery());
        assertNull(new AdditionalInsertQuery("LOAD <urn:g>").getInsertQuery());
    }

    @Test
    void additionalInsertQueryRequiresInsertKeyword() {
        assertNull(new AdditionalInsertQuery("SELECT * WHERE { ?s ?p ?o }").getInsertQuery());
    }

    @Test
    void additionalInsertQueryHandlesNullAndEmpty() {
        assertNull(new AdditionalInsertQuery(null).getInsertQuery());
        assertNull(new AdditionalInsertQuery("").getInsertQuery());
    }

    // ---- NumberOfAnnotationsResponse ----------------------------------------

    @Test
    void numberOfAnnotationsResponseRoundTrips() {
        NumberOfAnnotationsResponse r =
                new NumberOfAnnotationsResponse("http://c", 5, "urn:g", "SELECT *");
        assertEquals("http://c", r.getComponentUrl());
        assertEquals(5, r.getAnnotationCount());
        assertEquals("urn:g", r.getGraph());
        assertEquals("SELECT *", r.getSparqlGet());

        r.setComponentUrl("http://c2");
        r.setAnnotationCount(7);
        r.setGraph("urn:g2");
        r.setSparqlGet("ASK {}");
        assertEquals("http://c2", r.getComponentUrl());
        assertEquals(7, r.getAnnotationCount());
        assertEquals("urn:g2", r.getGraph());
        assertEquals("ASK {}", r.getSparqlGet());
    }

    // ---- QanaryQuestionAnsweringError ---------------------------------------

    @Test
    void questionAnsweringErrorExposesAllFields() {
        URI q = URI.create("urn:question:1");
        QanaryQuestionAnsweringError error = new QanaryQuestionAnsweringError(
                q, QanaryQuestionAnsweringError.ErrorType.COMPONENT_EXECUTION_FAILED, "NER", "boom");
        assertEquals(q, error.getQuestionUri());
        assertEquals(QanaryQuestionAnsweringError.ErrorType.COMPONENT_EXECUTION_FAILED, error.getType());
        assertEquals("NER", error.getComponent());
        assertEquals("boom", error.getMessage());
        assertNotNull(error.getTimestamp());
    }

    @Test
    void questionAnsweringErrorTypeEnumIsComplete() {
        // exercise valueOf/values for the enum
        assertEquals(4, QanaryQuestionAnsweringError.ErrorType.values().length);
        assertEquals(QanaryQuestionAnsweringError.ErrorType.PIPELINE_FAILURE,
                QanaryQuestionAnsweringError.ErrorType.valueOf("PIPELINE_FAILURE"));
    }
}
