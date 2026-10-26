package eu.wdaqua.qanary.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorInMemory;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

/**
 * Tests how {@link QanaryQuestion} and {@link QanaryQuestionTextual} are created:
 * from a question URL (a new graph is initialised in the triplestore), from a
 * {@link QanaryMessage} plus {@link QanaryConfigurator}, and the error cases.
 * <p>
 * The configurator's {@link QanaryTripleStoreProxy} is backed by an in-memory
 * store that records the {@code LOAD} requests for the ontologies instead of
 * fetching them from the Web, and executes every other update.
 */
class QanaryQuestionConstructionTest {

    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String QA = "http://www.wdaqua.eu/qa#";
    private static final String OA = "http://www.w3.org/ns/openannotation/core/";
    private static final URI ONTOLOGY = URI.create("urn:qanary:ontology");
    private static final URI ENDPOINT = URI.create("urn:test:endpoint");

    private OfflineInMemoryConnector store;
    private QanaryConfigurator configurator;

    @BeforeEach
    void setUp() {
        store = new OfflineInMemoryConnector();
        QanaryTripleStoreProxy proxy = new QanaryTripleStoreProxy();
        proxy.setExternalConnector(store);
        configurator = new QanaryConfigurator(null, List.of(), "localhost", 8080, ONTOLOGY, ENDPOINT, proxy);
    }

    @Test
    void newQuestionFromUrlInitialisesItsGraph() throws Exception {
        URL questionUrl = URI.create("http://localhost:8080/question/42").toURL();

        QanaryQuestion<String> question = new QanaryQuestion<>(questionUrl, configurator, null);

        URI graph = question.getNamedGraph();
        assertTrue(graph.toString().startsWith("urn:graph:"), graph.toString());
        assertEquals(graph, question.getInGraph());
        assertEquals(ENDPOINT, question.getEndpoint());
        assertEquals(URI.create("http://localhost:8080/question/42"), question.getUri());
        assertEquals(List.of("LOAD <http://www.w3.org/ns/oa.rdf> INTO GRAPH <" + graph + ">",
                "LOAD <" + ONTOLOGY + "> INTO GRAPH <" + graph + ">"), store.loadRequests);
        assertTrue(store.ask("ASK { GRAPH <" + graph + "> { <" + questionUrl + "> a <" + QA + "Question> ; "
                + "<http://www.w3.org/2002/07/owl#sameAs> <urn:qanary:currentQuestion> . "
                + "<" + questionUrl + "#Answer> a <" + QA + "Answer> . "
                + "<" + questionUrl + "#Dataset> a <" + QA + "Dataset> . "
                + "?a <" + OA + "hasTarget> <" + questionUrl + "> ; <" + OA + "hasBody> <" + questionUrl
                + "#Dataset> . } }"));
        assertFalse(store.ask("ASK { GRAPH <" + graph + "> { ?q <" + QA + "priorConversation> ?prior } }"),
                "no prior conversation was given");
        assertSame(configurator.getQanaryTripleStoreConnector(), question.getQanaryTripleStoreConnector());
    }

    @Test
    void eachNewQuestionGetsItsOwnGraph() throws Exception {
        URL questionUrl = URI.create("http://localhost:8080/question/42").toURL();

        URI first = new QanaryQuestion<String>(questionUrl, configurator, null).getNamedGraph();
        URI second = new QanaryQuestion<String>(questionUrl, configurator, null).getNamedGraph();

        assertFalse(first.equals(second));
    }

    @Test
    void newQuestionWithoutConfiguratorIsRejected() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> new QanaryQuestion<String>(URI.create("http://localhost/q").toURL(), null, null));
        assertEquals("qanaryConfigurator was null", e.getMessage());
    }

    @Test
    void questionFromMessageAndConfiguratorUsesTheConfiguratorsTriplestore() throws Exception {
        URI graph = URI.create("urn:graph:existing");
        store.update("INSERT DATA { GRAPH <" + graph + "> { <urn:qanary:question:7> <" + RDF_TYPE + "> <" + QA
                + "Question> . } }");

        QanaryQuestion<String> question = new QanaryQuestion<>(new QanaryMessage(ENDPOINT, graph), configurator);

        assertSame(configurator.getQanaryTripleStoreConnector(), question.getQanaryTripleStoreConnector());
        assertEquals(URI.create("urn:qanary:question:7"), question.getUri());
        assertTrue(store.loadRequests.isEmpty(), "an existing question does not load ontologies");
    }

    @Test
    void textualQuestionFromMessageIsMarkedAsTextual() throws Exception {
        URI graph = URI.create("urn:graph:textual");
        store.update("INSERT DATA { GRAPH <" + graph + "> { <urn:qanary:question:8> <" + RDF_TYPE + "> <" + QA
                + "Question> . } }");

        new QanaryQuestionTextual(new QanaryMessage(ENDPOINT, graph), configurator);

        assertTrue(store.ask("ASK { GRAPH <" + graph + "> { ?a a <" + QA + "AnnotationOfTextRepresentation> ; "
                + "<" + OA + "hasTarget> <urn:qanary:question:8> ; <" + OA + "hasBody> <urn:qanary:question:8> . } }"));
    }

    @Test
    void textualQuestionFromUrlIsMarkedAsTextual() throws Exception {
        URL questionUrl = URI.create("http://localhost:8080/question/43").toURL();

        QanaryQuestionTextual question = new QanaryQuestionTextual(questionUrl, configurator);

        assertTrue(store.ask("ASK { GRAPH <" + question.getNamedGraph() + "> { ?a a <" + QA
                + "AnnotationOfTextRepresentation> ; <" + OA + "hasBody> <" + questionUrl + "> . } }"));
    }

    @Test
    void textualQuestionFromUrlKeepsThePriorConversation() throws Exception {
        URL questionUrl = URI.create("http://localhost:8080/question/44").toURL();
        URI prior = URI.create("urn:graph:previous-turn");

        QanaryQuestionTextual question = new QanaryQuestionTextual(questionUrl, configurator, prior);

        assertTrue(store.ask("ASK { GRAPH <" + question.getNamedGraph() + "> { <" + questionUrl + "> <" + QA
                + "priorConversation> <" + prior + "> . } }"));
    }

    @Test
    void aQuestionWithoutAnyTriplestoreConnectorFailsWhenItNeedsOne() throws Exception {
        QanaryQuestion<String> question = new QanaryQuestion<>(new QanaryMessage(ENDPOINT, URI.create("urn:g")),
                (QanaryTripleStoreConnector) null);

        RuntimeException e = assertThrows(RuntimeException.class, question::getQanaryTripleStoreConnector);
        assertEquals("in QanaryQuestion a QanaryTripleStoreConnector instance is required", e.getMessage());
    }

    @Test
    void getUriFailsWhenTheMessageHasNoInGraph() throws Exception {
        QanaryQuestion<String> question = new QanaryQuestion<>(new QanaryMessage(ENDPOINT, null), store);

        QanaryExceptionNoOrMultipleQuestions e = assertThrows(QanaryExceptionNoOrMultipleQuestions.class,
                question::getUri);
        assertTrue(e.getMessage().contains("inGraph is null"), e.getMessage());
    }

    /**
     * in-memory triplestore that records {@code LOAD} requests instead of fetching
     * the documents from the Web (tests run offline) and executes all other updates
     */
    static class OfflineInMemoryConnector extends QanaryTripleStoreConnectorInMemory {
        final List<String> loadRequests = new ArrayList<>();

        @Override
        public void update(String sparql) throws SparqlQueryFailed {
            if (sparql.trim().startsWith("LOAD ")) {
                loadRequests.add(sparql.trim());
            } else {
                super.update(sparql);
            }
        }
    }
}
