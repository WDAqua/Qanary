package eu.wdaqua.qanary.commons;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import eu.wdaqua.qanary.commons.ontology.TextPositionSelector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorInMemory;

/**
 * Tests the representation-related parts of {@link QanaryQuestion}: raw data,
 * textual and audio representations, translations and language selection,
 * text position annotations, and storing a new textual representation.
 * <p>
 * Annotations are read from and written to an in-memory Jena triplestore; the
 * question data that QanaryQuestion fetches over HTTP is served by a
 * {@link LoopbackHttpServer} on 127.0.0.1, so no external service is needed.
 */
class QanaryQuestionRepresentationsTest {

    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String QA = "http://www.wdaqua.eu/qa#";
    private static final String OA = "http://www.w3.org/ns/openannotation/core/";
    private static final String XSD = "http://www.w3.org/2001/XMLSchema#";

    private static final URI ENDPOINT = URI.create("urn:test:endpoint");
    private static final URI GRAPH = URI.create("urn:graph:representations");
    private static final String QUESTION_TEXT = "What is the capital of Saxony?";

    private LoopbackHttpServer server;
    private QanaryTripleStoreConnectorInMemory connector;
    private QanaryQuestion<String> question;
    private String questionUri;
    private URI previousServiceUri;
    private URI previousHostUri;

    @BeforeEach
    void setUp() throws Exception {
        server = new LoopbackHttpServer();
        questionUri = server.url("/question/q1");
        connector = new QanaryTripleStoreConnectorInMemory();
        connector.update("INSERT DATA { GRAPH <" + GRAPH + "> { <" + questionUri + "> <" + RDF_TYPE + "> <" + QA
                + "Question> . } }");
        question = new QanaryQuestion<>(new QanaryMessage(ENDPOINT, GRAPH), connector);
        previousServiceUri = QanaryConfiguration.getServiceUri();
        previousHostUri = QanaryConfiguration.getHostUri();
    }

    @AfterEach
    void tearDown() {
        server.close();
        QanaryConfiguration.setServiceUri(previousServiceUri);
        QanaryConfiguration.setHostUri(previousHostUri);
    }

    // ---- raw data -------------------------------------------------------------

    @Test
    void getRawDataFollowsTheRawLinkOfTheQuestionAndCachesTheResult() throws Exception {
        server.respond("/question/q1", "application/json", "{\"raw\": \"" + server.url("/question/q1/raw") + "\"}")
                .respond("/question/q1/raw", "text/plain", QUESTION_TEXT);

        assertEquals(QUESTION_TEXT, question.getRawData());
        assertEquals(QUESTION_TEXT, question.getRawData());
        assertEquals(1, server.requestCount("/question/q1/raw"), "the raw data is fetched only once");
    }

    @Test
    void getRawDataIsNullWhenTheRawResourceHasNoContent() throws Exception {
        server.respond("/question/q1", "application/json", "{\"raw\": \"" + server.url("/question/q1/raw") + "\"}")
                .respond("/question/q1/raw", "text/plain", new byte[0]);

        assertNull(question.getRawData());
    }

    // ---- textual representation -------------------------------------------------

    @Test
    void getTextualRepresentationFetchesTheRawTextOfTheAnnotatedRepresentation() throws Exception {
        server.respond("/question/q1/raw", "text/plain", QUESTION_TEXT);
        question.putAnnotationOfTextRepresentation();

        assertEquals(URI.create(questionUri), question.getUriTextualRepresentation());
        assertEquals(QUESTION_TEXT, question.getTextualRepresentation());
    }

    @Test
    void getUriTextualRepresentationFailsWithoutAnnotation() {
        Exception e = assertThrows(Exception.class, question::getUriTextualRepresentation);
        assertTrue(e.getMessage().startsWith("No uriTextRepresentation available"), e.getMessage());
    }

    @Test
    void getUriTextualRepresentationFailsWithTwoRepresentations() throws Exception {
        insertTextRepresentation("urn:anno:text:1", questionUri);
        insertTextRepresentation("urn:anno:text:2", "urn:other:representation");

        Exception e = assertThrows(Exception.class, question::getUriTextualRepresentation);
        assertTrue(e.getMessage().startsWith("More than 1 text representation (count: 2)"), e.getMessage());
    }

    @Test
    void getTextualRepresentationInTheQuestionLanguageReturnsTheOriginalText() throws Exception {
        server.respond("/question/q1/raw", "text/plain", QUESTION_TEXT);
        question.putAnnotationOfTextRepresentation();
        question.setLanguageText("en");

        assertEquals(QUESTION_TEXT, question.getTextualRepresentation("en"));
    }

    @Test
    void getTextualRepresentationInAnotherLanguageReturnsTheTranslation() throws Exception {
        question.setLanguageText("en");
        insertTranslation("Was ist die Hauptstadt von Sachsen?", "de");
        insertTranslation("Quelle est la capitale de la Saxe ?", "fr");

        assertEquals("Was ist die Hauptstadt von Sachsen?", question.getTextualRepresentation("de"));
        assertEquals("Quelle est la capitale de la Saxe ?", question.getTranslatedTextualRepresentation("fr"));
    }

    @Test
    void getTextualRepresentationFallsBackToTranslationsWhenTheLanguageIsUnknown() throws Exception {
        // no AnnotationOfQuestionLanguage: getLanguage() fails, the translation is used
        insertTranslation("Was ist die Hauptstadt von Sachsen?", "de");

        assertEquals("Was ist die Hauptstadt von Sachsen?", question.getTextualRepresentation("de"));
    }

    @Test
    void getTextualRepresentationRejectsLanguagesThatAreNotTwoLetterCodes() throws Exception {
        question.setLanguageText("en");

        Exception e = assertThrows(Exception.class, () -> question.getTextualRepresentation("deu"));
        assertEquals("parameter `language` is invalid: deu", e.getMessage());
    }

    @Test
    void getTranslatedTextualRepresentationFailsWithoutTranslation() throws Exception {
        insertTranslation("Was ist die Hauptstadt von Sachsen?", "de");

        Exception e = assertThrows(Exception.class, () -> question.getTranslatedTextualRepresentation("es"));
        assertTrue(e.getMessage().contains("for language es"), e.getMessage());
    }

    // ---- audio representation -------------------------------------------------

    @Test
    void getAudioRepresentationFetchesTheBytesOfTheAnnotatedRepresentation() throws Exception {
        byte[] audio = { 82, 73, 70, 70, 0, 1, 2, 3 };
        server.respond("/question/q1/raw", "audio/wav", audio);
        question.putAnnotationOfAudioRepresentation();

        assertEquals(URI.create(questionUri), question.getUriAudioRepresentation());
        assertArrayEquals(audio, question.getAudioRepresentation());
    }

    @Test
    void getUriAudioRepresentationFailsWithoutAnnotation() {
        Exception e = assertThrows(Exception.class, question::getUriAudioRepresentation);
        assertTrue(e.getMessage().startsWith("No uriAudioRepresentation available"), e.getMessage());
    }

    @Test
    void getUriAudioRepresentationFailsWithTwoRepresentations() throws Exception {
        question.putAnnotationOfAudioRepresentation();
        question.putAnnotationOfAudioRepresentation();

        Exception e = assertThrows(Exception.class, question::getUriAudioRepresentation);
        // the production message wrongly says "text" representation; only its substance is checked
        assertTrue(e.getMessage().startsWith("More than 1 ") && e.getMessage().contains("(count: 2)"), e.getMessage());
    }

    // ---- annotations of text positions ----------------------------------------------

    @Test
    void addAnnotationsWritesOneInstanceAnnotationPerSelector() throws Exception {
        QanaryConfiguration.setServiceUri(URI.create("urn:qanary:component:NED-test"));
        question.putAnnotationOfTextRepresentation();

        question.addAnnotations(List.of( //
                new TextPositionSelector(24, 30, URI.create("http://dbpedia.org/resource/Saxony"), 0.9f), //
                new TextPositionSelector(8, 15)));

        ResultSet rows = connector.select("SELECT ?start ?end ?source ?body ?score ?by FROM <" + GRAPH + "> WHERE { "
                + "?a a <" + QA + "AnnotationOfInstance> ; <" + OA + "hasTarget> ?t ; <" + OA + "annotatedBy> ?by ; "
                + "   <" + OA + "annotatedAt> ?time . "
                + "?t <" + OA + "hasSource> ?source ; <" + OA + "hasSelector> ?s . "
                + "?s <" + OA + "start> ?start ; <" + OA + "end> ?end . "
                + "OPTIONAL { ?a <" + OA + "hasBody> ?body } OPTIONAL { ?a <" + OA + "score> ?score } "
                + "} ORDER BY ?start");

        QuerySolution withoutResource = rows.next();
        assertEquals(8, withoutResource.getLiteral("start").getInt());
        assertEquals(15, withoutResource.getLiteral("end").getInt());
        assertEquals(questionUri, withoutResource.getResource("source").getURI());
        assertEquals("urn:qanary:component:NED-test", withoutResource.getResource("by").getURI());
        assertNull(withoutResource.get("body"));
        assertNull(withoutResource.get("score"));

        QuerySolution withResource = rows.next();
        assertEquals(24, withResource.getLiteral("start").getInt());
        assertEquals(30, withResource.getLiteral("end").getInt());
        assertEquals("http://dbpedia.org/resource/Saxony", withResource.getResource("body").getURI());
        assertEquals(0.9, withResource.getLiteral("score").getDouble(), 1e-6);
        assertEquals(XSD + "double", withResource.getLiteral("score").getDatatypeURI());
        assertTrue(!rows.hasNext(), "exactly one annotation per selector");
    }

    // ---- storing a new textual representation ---------------------------------------

    @Test
    void putTextRepresentationRegistersTheTextAndAnnotatesTheReturnedUri() throws Exception {
        String createdQuestion = server.url("/question/q2");
        server.respond("/question", "application/json", "{\"questionURI\": \"" + createdQuestion + "\"}");
        QanaryConfiguration.setHostUri(URI.create(server.baseUrl()));
        QanaryConfiguration.setServiceUri(URI.create("urn:qanary:component:translator"));

        question.putTextRepresentation("Welche Stadt ist die Hauptstadt von Sachsen?");

        assertEquals(1, server.requestCount("/question"));
        assertTrue(server.lastRequestBody("/question").startsWith("question=Welche"),
                server.lastRequestBody("/question"));
        assertTrue(connector.ask("ASK { GRAPH <" + GRAPH + "> { ?a a <" + QA + "AnnotationOfTextualRepresentation> ; "
                + "<" + OA + "hasTarget> <" + questionUri + "> ; <" + OA + "hasBody> <" + createdQuestion + "> ; "
                + "<" + OA + "annotatedBy> <urn:qanary:component:translator> . } }"));
    }

    // ---- language, target data and answers ---------------------------------------------

    @Test
    void getLanguageFailsWithoutLanguageAnnotation() {
        Exception e = assertThrows(Exception.class, question::getLanguage);
        assertTrue(e.getMessage().startsWith("No language available"), e.getMessage());
    }

    @Test
    void getLanguageFailsWithTwoEarliestLanguageAnnotations() throws Exception {
        String sameTime = "\"2026-01-01T00:00:00Z\"^^<" + XSD + "dateTime>";
        connector.update("INSERT DATA { GRAPH <" + GRAPH + "> { "
                + "<urn:anno:lang:1> a <" + QA + "AnnotationOfQuestionLanguage> ; <" + OA + "hasBody> \"en\" ; <" + OA
                + "annotatedAt> " + sameTime + " . "
                + "<urn:anno:lang:2> a <" + QA + "AnnotationOfQuestionLanguage> ; <" + OA + "hasBody> \"de\" ; <" + OA
                + "annotatedAt> " + sameTime + " . } }");

        Exception e = assertThrows(Exception.class, question::getLanguage);
        assertTrue(e.getMessage().startsWith("More than 1 language (count: 2)"), e.getMessage());
    }

    @Test
    void getTargetDataFailsWithoutDatasetAnnotation() {
        Exception e = assertThrows(Exception.class, question::getTargetData);
        assertTrue(e.getMessage().matches("(?s)No \\w+ available.*"), e.getMessage()); // tolerant of the "knwoledgebase" typo
    }

    @Test
    void getSparqlResultsReportsTheConfidenceOfAnAnswerQuery() throws Exception {
        connector.update("INSERT DATA { GRAPH <" + GRAPH + "> { "
                + "<urn:anno:sparql> a <" + QA + "AnnotationOfAnswerSPARQL> ; <" + OA + "hasBody> <urn:body:sparql> ; "
                + "  <" + QA + "score> \"0.75\"^^<" + XSD + "double> ; "
                + "  <" + OA + "annotatedAt> \"2026-01-01T00:00:00Z\"^^<" + XSD + "dateTime> . "
                + "<urn:body:sparql> a <" + QA + "SparqlQuery> ; "
                + "  <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> \"ASK {}\" . } }");

        List<QanaryQuestion<String>.SparqlAnnotation> results = question.getSparqlResults();

        assertEquals(1, results.size());
        assertEquals("ASK {}", results.get(0).query);
        // the confidence is the literal's toString(), which keeps the datatype
        assertTrue(results.get(0).confidence.contains("0.75"), results.get(0).confidence);
    }

    // ---- helpers -----------------------------------------------------------------

    private void insertTextRepresentation(String annotation, String body) throws Exception {
        connector.update("INSERT DATA { GRAPH <" + GRAPH + "> { <" + annotation + "> a <" + QA
                + "AnnotationOfTextRepresentation> ; <" + OA + "hasTarget> <" + questionUri + "> ; <" + OA
                + "hasBody> <" + body + "> . } }");
    }

    private void insertTranslation(String text, String language) throws Exception {
        connector.update("INSERT DATA { GRAPH <" + GRAPH + "> { <urn:anno:translation:" + language + "> a <" + QA
                + "AnnotationOfQuestionTranslation> ; <" + OA + "hasTarget> <" + questionUri + "> ; <" + OA
                + "hasBody> \"" + text + "\"@" + language + " ; <" + OA + "annotatedBy> <urn:qanary:component:MT> ; <"
                + OA + "annotatedAt> \"2026-01-01T00:00:00Z\"^^<" + XSD + "dateTime> . } }");
    }
}
