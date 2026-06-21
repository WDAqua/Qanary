package qald.evaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import eu.wdaqua.qanary.qald.evaluator.qaldreader.QaldQuestion;
import eu.wdaqua.qanary.qald.evaluator.qaldreader.QaldQuestionUri;

class QaldQuestionTest {

    private JsonObject parse(String json) {
        return new Gson().fromJson(json, JsonObject.class);
    }

    /**
     * a complete QALD question (English string + SPARQL query) is parsed and the
     * annotated URIs are extracted from the query pattern
     */
    @Test
    void parsesQuestionWithSparqlQueryAndExtractsUris() {
        String json = "{"
                + "\"id\": 42,"
                + "\"question\": [{\"language\": \"en\", \"string\": \"Where is Berlin?\"}],"
                + "\"query\": {\"sparql\": \"PREFIX dbr: <http://dbpedia.org/resource/> "
                + "PREFIX dbo: <http://dbpedia.org/ontology/> "
                + "SELECT ?x WHERE { dbr:Berlin dbo:country ?x . }\"}"
                + "}";

        QaldQuestion q = new QaldQuestion(parse(json));

        assertEquals(42, q.getQaldId());
        assertEquals("Where is Berlin?", q.getQuestion());

        // dbr:Berlin (subject) + dbo:country (predicate) are URIs; ?x is a variable
        assertEquals(2, q.getUris().size());

        List<String> resourceUris = q.getResourceUrisAsString();
        assertEquals(1, resourceUris.size());
        assertEquals("http://dbpedia.org/resource/Berlin", resourceUris.get(0));

        QaldQuestionUri berlin = q.getUri(URI.create("http://dbpedia.org/resource/Berlin"));
        assertNotNull(berlin);
        assertTrue(berlin.isUsedAsSubject());
        assertTrue(berlin.isDBpediaResource());
    }

    /**
     * a non-English language entry is skipped until the English one is found
     */
    @Test
    void skipsNonEnglishLanguageEntries() {
        String json = "{"
                + "\"id\": 7,"
                + "\"question\": ["
                + "  {\"language\": \"de\", \"string\": \"Wo liegt Berlin?\"},"
                + "  {\"language\": \"en\", \"string\": \"Where is Berlin?\"}"
                + "],"
                + "\"query\": {\"sparql\": \"PREFIX dbr: <http://dbpedia.org/resource/> "
                + "SELECT ?x WHERE { dbr:Berlin ?p ?x . }\"}"
                + "}";

        QaldQuestion q = new QaldQuestion(parse(json));

        assertEquals(7, q.getQaldId());
        assertEquals("Where is Berlin?", q.getQuestion());
        assertEquals(1, q.getResourceUrisAsString().size());
    }

    /**
     * a question without a SPARQL query yields no annotated URIs (warn branch)
     */
    @Test
    void questionWithoutSparqlHasNoUris() {
        String json = "{"
                + "\"id\": 99,"
                + "\"question\": [{\"language\": \"en\", \"string\": \"unanswerable\"}],"
                + "\"query\": {}"
                + "}";

        QaldQuestion q = new QaldQuestion(parse(json));

        assertEquals(99, q.getQaldId());
        assertTrue(q.getUris().isEmpty());
        assertTrue(q.getResourceUrisAsString().isEmpty());
    }
}
