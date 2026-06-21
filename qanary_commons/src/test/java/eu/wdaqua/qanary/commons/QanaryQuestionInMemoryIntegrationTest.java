package eu.wdaqua.qanary.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorInMemory;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

/**
 * Integration test for {@link QanaryQuestion} (and the {@link QanaryUtils} it
 * wraps) running against a real, in-memory Jena triplestore
 * ({@link QanaryTripleStoreConnectorInMemory}). Unlike the pure-unit tests this
 * actually parses and executes SPARQL queries and round-trips RDF, exercising
 * the question/annotation read+write code paths end to end without any external
 * triplestore.
 */
class QanaryQuestionInMemoryIntegrationTest {

    private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String RDF_TYPE = RDF + "type";
    private static final String RDF_VALUE = RDF + "value";
    private static final String QA = "http://www.wdaqua.eu/qa#";
    private static final String OA = "http://www.w3.org/ns/openannotation/core/";

    private static final URI ENDPOINT = URI.create("urn:test:endpoint");
    private static final URI GRAPH = URI.create("urn:graph:test");
    private static final String QUESTION_URI = "urn:qanary:question:1";

    private QanaryTripleStoreConnectorInMemory connector;
    private QanaryQuestion<String> question;

    @BeforeEach
    void setUp() throws Exception {
        connector = new QanaryTripleStoreConnectorInMemory();
        // a single qa:Question resource in the working graph
        connector.update("INSERT DATA { GRAPH <" + GRAPH + "> { "
                + "<" + QUESTION_URI + "> <" + RDF_TYPE + "> <" + QA + "Question> . } }");
        QanaryMessage message = new QanaryMessage(ENDPOINT, GRAPH); // outGraph == inGraph
        question = new QanaryQuestion<>(message, connector);
    }

    @Test
    void exposesGraphsEndpointMessageAndConnector() {
        assertEquals(GRAPH, question.getInGraph());
        assertEquals(GRAPH, question.getOutGraph());
        assertEquals(GRAPH, question.getNamedGraph());
        assertEquals(ENDPOINT, question.getEndpoint());
        assertNotNull(question.getQanaryMessage());
        assertNotNull(question.getQanaryUtils());
        assertSame(connector, question.getQanaryTripleStoreConnector());
    }

    @Test
    void getUriReadsTheStoredQuestionResource() throws Exception {
        assertEquals(URI.create(QUESTION_URI), question.getUri());
    }

    @Test
    void getUriThrowsWhenNoQuestionInGraph() throws Exception {
        QanaryMessage emptyMessage = new QanaryMessage(ENDPOINT, URI.create("urn:graph:empty"));
        QanaryQuestion<String> emptyQuestion = new QanaryQuestion<>(emptyMessage, connector);
        assertThrows(QanaryExceptionNoOrMultipleQuestions.class, emptyQuestion::getUri);
    }

    @Test
    void getUriThrowsWhenMultipleQuestionsInGraph() throws Exception {
        connector.update("INSERT DATA { GRAPH <" + GRAPH + "> { "
                + "<urn:qanary:question:2> <" + RDF_TYPE + "> <" + QA + "Question> . } }");
        assertThrows(QanaryExceptionNoOrMultipleQuestions.class, question::getUri);
    }

    @Test
    void putAnnotationOfTextRepresentationInsertsAnnotation() throws Exception {
        // initially absent
        assertTrue(annotationAbsent("AnnotationOfTextRepresentation"));
        question.putAnnotationOfTextRepresentation();
        assertTrue(annotationPresent("AnnotationOfTextRepresentation"));
    }

    @Test
    void putAnnotationOfAudioRepresentationInsertsAnnotation() throws Exception {
        question.putAnnotationOfAudioRepresentation();
        assertTrue(annotationPresent("AnnotationOfAudioRepresentation"));
    }

    @Test
    void getSparqlResultsReturnsAnnotatedAnswerQuery() throws Exception {
        // no answer-SPARQL annotations yet
        assertTrue(question.getSparqlResults().isEmpty());
        assertEquals("", question.getSparqlResult());

        connector.update("INSERT DATA { GRAPH <" + GRAPH + "> { "
                + "<urn:anno:sparql> <" + RDF_TYPE + "> <" + QA + "AnnotationOfAnswerSPARQL> ; "
                + "  <" + OA + "hasBody> <urn:body:sparql> ; "
                + "  <" + OA + "annotatedAt> \"2024-01-01T00:00:00Z\"^^<http://www.w3.org/2001/XMLSchema#dateTime> . "
                + "<urn:body:sparql> <" + RDF_TYPE + "> <" + QA + "SparqlQuery> ; "
                + "  <" + RDF_VALUE + "> \"SELECT * WHERE {}\" . } }");

        var results = question.getSparqlResults();
        assertEquals(1, results.size());
        assertTrue(question.getSparqlResult().contains("SELECT"));
    }

    @Test
    void getJsonResultReturnsEmptyThenAnnotatedJson() throws Exception {
        assertEquals("", question.getJsonResult());

        connector.update("INSERT DATA { GRAPH <" + GRAPH + "> { "
                + "<urn:anno:json> <" + RDF_TYPE + "> <" + QA + "AnnotationOfAnswerJson> ; "
                + "  <" + OA + "hasBody> <urn:body:json> . "
                + "<urn:body:json> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> \"[1,2,3]\" . } }");

        assertTrue(question.getJsonResult().contains("1,2,3"));
    }

    @Test
    void languageAnnotationRoundTrip() throws Exception {
        question.setLanguageText("en");
        assertEquals("en", question.getLanguage());
    }

    @Test
    void targetDataAnnotationRoundTrip() throws Exception {
        question.setTargetData(java.util.List.of("http://dbpedia.org"));
        assertTrue(question.getTargetData().contains("http://dbpedia.org"));
    }

    @Test
    void getAnswerFoundDefaultsToUndefinedWhenAbsent() throws Exception {
        assertEquals("undefined", question.getAnswerFound());
    }

    @Test
    void getUriTextualRepresentationReturnsAnnotatedBody() throws Exception {
        question.putAnnotationOfTextRepresentation();
        assertEquals(URI.create(QUESTION_URI), question.getUriTextualRepresentation());
    }

    // ---- helpers ------------------------------------------------------------

    private boolean annotationPresent(String qaType) throws SparqlQueryFailed {
        return connector.ask("ASK { GRAPH <" + GRAPH + "> { ?a <" + RDF_TYPE + "> <" + QA + qaType + "> } }");
    }

    private boolean annotationAbsent(String qaType) throws SparqlQueryFailed {
        return !annotationPresent(qaType);
    }
}
