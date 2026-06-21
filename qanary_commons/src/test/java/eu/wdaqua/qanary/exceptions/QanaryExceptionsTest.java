package eu.wdaqua.qanary.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for the lightweight exception types in the commons module.
 */
class QanaryExceptionsTest {

    @Test
    void missingRequiredConfigurationMentionsKey() {
        MissingRequiredConfiguration ex = new MissingRequiredConfiguration("qanary.triplestore");
        assertTrue(ex.getMessage().contains("qanary.triplestore"));
    }

    @Test
    void serviceCallNotOkFromHttpStatusExposesDetails() {
        QanaryExceptionServiceCallNotOk ex =
                new QanaryExceptionServiceCallNotOk("my-component", 123L, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals("my-component", ex.getComponentName());
        assertEquals(123L, ex.getDuration());
        assertEquals("INTERNAL_SERVER_ERROR/500", ex.getErrormessageHttpStatus());
        // the message must actually interpolate the values (regression: it used
        // SLF4J-style {} placeholders with String.format, dropping all arguments)
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("my-component"), ex.getMessage());
        assertTrue(ex.getMessage().contains("123"), ex.getMessage());
        assertTrue(ex.getMessage().contains("INTERNAL_SERVER_ERROR"), ex.getMessage());
        assertFalse(ex.getMessage().contains("{}"), ex.getMessage());
    }

    @Test
    void serviceCallNotOkFromMessageAndStackTraceExposesDetails() {
        QanaryExceptionServiceCallNotOk ex =
                new QanaryExceptionServiceCallNotOk("comp", 7L, "boom", "at X.y(Z.java:1)");
        assertEquals("comp", ex.getComponentName());
        assertEquals(7L, ex.getDuration());
        assertEquals("boom\nat X.y(Z.java:1)", ex.getErrormessageHttpStatus());
    }

    @Test
    void sparqlQueryFailedKeepsQueryTriplestoreAndCause() {
        Exception cause = new IllegalStateException("syntax error");
        SparqlQueryFailed ex = new SparqlQueryFailed("SELECT * WHERE {}", "http://localhost/sparql", cause);
        assertSame(cause, ex.getBaseException());
        assertEquals("SELECT * WHERE {}", ex.getSparqlQuery());
        assertEquals("http://localhost/sparql", ex.getTriplestore());
        assertTrue(ex.getMessage().contains("http://localhost/sparql"));
        assertTrue(ex.getMessage().contains("syntax error"));
    }

    @Test
    void tripleStoreNotProvidedFromUri() {
        TripleStoreNotProvided ex = new TripleStoreNotProvided(URI.create("http://localhost:8890/sparql"));
        assertTrue(ex.getMessage().contains("http://localhost:8890/sparql"));
    }

    @Test
    void tripleStoreNotProvidedFromNullUri() {
        TripleStoreNotProvided ex = new TripleStoreNotProvided((URI) null);
        assertTrue(ex.getMessage().contains("null"));
    }

    @Test
    void tripleStoreNotProvidedFromString() {
        TripleStoreNotProvided ex = new TripleStoreNotProvided("some-endpoint");
        assertTrue(ex.getMessage().contains("some-endpoint"));
        assertTrue(new TripleStoreNotProvided((String) null).getMessage().contains("null"));
    }

    @Test
    void tripleStoreNotWorkingKeepsMessage() {
        TripleStoreNotWorking ex = new TripleStoreNotWorking("unreachable");
        assertEquals("unreachable", ex.getMessage());
    }

    @Test
    void notEquivalentSparqlQueriesKeepsBothQueries() {
        NotEquivalentSparqlQueriesException ex =
                new NotEquivalentSparqlQueriesException("not equal", "ASK {}", "SELECT * {}");
        assertEquals("not equal", ex.getMessage());
        assertEquals("ASK {}", ex.getExpectedQuery());
        assertEquals("SELECT * {}", ex.getActualQuery());
    }
}
