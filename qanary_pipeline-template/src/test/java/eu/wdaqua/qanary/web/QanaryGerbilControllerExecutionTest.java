package eu.wdaqua.qanary.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;

import eu.wdaqua.qanary.QanaryComponentRegistrationChangeNotifier;
import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.web.messages.GerbilExecuteResponse;

/**
 * Tests the Gerbil QA integration: generating the Gerbil endpoint URL from a
 * component list, and executing a Gerbil request end to end. The pipeline's own
 * start endpoint and the question text are served by a local HTTP stub; the
 * annotations the components would write live in an in-memory triplestore.
 */
class QanaryGerbilControllerExecutionTest {

    private static final URI ENDPOINT = URI.create("urn:test:qanary-triplestore");
    private static final URI GRAPH = URI.create("urn:graph:gerbil-test");
    private static final String QA = "http://www.wdaqua.eu/qa#";
    private static final String OA = "http://www.w3.org/ns/openannotation/core/";
    private static final String RDF_TYPE = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    private static final String RDF_VALUE = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#value>";

    private InMemoryQanaryTripleStore triplestore;
    private LocalQanaryHttpStub pipeline;
    private QanaryComponentRegistrationChangeNotifier notifier;
    private QanaryGerbilController controller;

    @BeforeEach
    void setUp() throws Exception {
        triplestore = new InMemoryQanaryTripleStore();
        pipeline = new LocalQanaryHttpStub();

        QanaryConfigurator configurator = mock(QanaryConfigurator.class);
        when(configurator.getHost()).thenReturn("http://127.0.0.1");
        when(configurator.getPort()).thenReturn(pipeline.port());
        when(configurator.getEndpoint()).thenReturn(ENDPOINT);
        when(configurator.getQanaryTripleStoreConnector()).thenReturn(triplestore.proxy());

        QanaryPipelineConfiguration pipelineConfiguration = mock(QanaryPipelineConfiguration.class);
        when(pipelineConfiguration.getHost()).thenReturn("http://qanary.example.test");
        when(pipelineConfiguration.getPort()).thenReturn(8080);

        notifier = mock(QanaryComponentRegistrationChangeNotifier.class);
        when(notifier.getAvailableComponentNames()).thenReturn(List.of("NED-DBpediaSpotlight", "QB-Simple"));

        controller = new QanaryGerbilController(configurator, pipelineConfiguration,
                mock(QanaryQuestionController.class), notifier);

        pipeline.respondToStartRequestsWith(new JSONObject()
                .put("endpoint", ENDPOINT.toString())
                .put("inGraph", GRAPH.toString())
                .put("outGraph", GRAPH.toString())
                .toString());
    }

    @AfterEach
    void tearDown() {
        pipeline.close();
    }

    /**
     * stores a question, its text representation and the results a query builder
     * and a query executor would have annotated
     */
    private void annotateAnsweredQuestion(String text, String language) throws Exception {
        URI question = pipeline.addQuestion("gerbil-1", text);
        triplestore.update("INSERT DATA { GRAPH <" + GRAPH + "> { "
                + "<" + question + "> " + RDF_TYPE + " <" + QA + "Question> . "
                + "<urn:anno:sparql> " + RDF_TYPE + " <" + QA + "AnnotationOfAnswerSPARQL> ; "
                + "  <" + OA + "hasBody> <urn:body:sparql> ; "
                + "  <" + OA + "annotatedAt> \"2024-01-01T00:00:00Z\"^^<http://www.w3.org/2001/XMLSchema#dateTime> . "
                + "<urn:body:sparql> " + RDF_TYPE + " <" + QA + "SparqlQuery> ; "
                + "  " + RDF_VALUE + " \"SELECT ?height WHERE { dbr:Zugspitze dbo:elevation ?height }\" . "
                + "<urn:anno:json> " + RDF_TYPE + " <" + QA + "AnnotationOfAnswerJson> ; "
                + "  <" + OA + "hasBody> <urn:body:json> . "
                + "<urn:body:json> " + RDF_VALUE + " \"{\\\"height\\\": \\\"2962\\\"}\" . } }");
        QanaryQuestion<String> qanaryQuestion = new QanaryQuestion<>(new QanaryMessage(ENDPOINT, GRAPH),
                triplestore.proxy());
        qanaryQuestion.putAnnotationOfTextRepresentation();
        if (language != null) {
            qanaryQuestion.setLanguageText(language);
        }
    }

    // ---- endpoint generator ------------------------------------------------------

    @Test
    void exposesTheAvailableComponents() {
        assertEquals(List.of("NED-DBpediaSpotlight", "QB-Simple"), controller.componentList());
    }

    @Test
    void generatorFormAsksForComponents() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("generategerbilendpoint", controller.startquestionansweringwithtextquestion(model));
        assertEquals("Select components!", model.get("url"));
    }

    @Test
    void generatorWithoutComponentsAsksForComponents() throws Exception {
        ExtendedModelMap model = new ExtendedModelMap();
        assertEquals("generategerbilendpoint", controller.gerbilGenerator(List.of(), model));
        assertEquals("Select components!", model.get("url"));
    }

    @Test
    void generatorBuildsTheGerbilUrlFromTheComponentList() throws Exception {
        ExtendedModelMap model = new ExtendedModelMap();
        controller.gerbilGenerator(List.of("NED-DBpediaSpotlight", "QB-Simple"), model);
        assertEquals("http://qanary.example.test:8080/gerbil-execute/NED-DBpediaSpotlight,%20QB-Simple/",
                model.get("url"));
    }

    @Test
    void responsesAllowCrossOriginRequests() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.setVaryResponseHeader(response);
        assertEquals("*", response.getHeader("Access-Control-Allow-Origin"));
    }

    // ---- executing a Gerbil request ------------------------------------------------

    @Test
    void executionReturnsQuestionQueryAndAnswersInGerbilFormat() throws Exception {
        annotateAnsweredQuestion("How high is the Zugspitze?", "de");

        ResponseEntity<GerbilExecuteResponse> response = controller.gerbil("How high is the Zugspitze?", "de",
                URI.create("urn:graph:previous"), "NED-DBpediaSpotlight, QB-Simple");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(GRAPH.toString(), response.getHeaders().getFirst(QanaryStandardWebParameters.QANARYGRAPHHEADERNAME));
        assertEquals(QanaryStandardWebParameters.QANARYGRAPHHEADERNAME,
                response.getHeaders().getFirst("Access-Control-Expose-Headers"));

        JSONObject body = new JSONObject(Jackson2ObjectMapperBuilder.json().build()
                .writeValueAsString(response.getBody()));
        JSONObject question = body.getJSONArray("questions").getJSONObject(0);
        assertEquals("How high is the Zugspitze?", question.getJSONArray("question").getJSONObject(0).getString("string"));
        assertEquals("de", question.getJSONArray("question").getJSONObject(0).getString("language"));
        assertEquals("SELECT ?height WHERE { dbr:Zugspitze dbo:elevation ?height }",
                question.getJSONObject("query").getString("sparql"));
        assertEquals("2962", question.getJSONArray("answers").getJSONObject(0).getString("height"));

        // the pipeline was asked to run exactly these components on this question
        Map<String, String> startRequest = pipeline.receivedStartRequests().get(0);
        assertEquals("How high is the Zugspitze?", startRequest.get("question"));
        assertEquals("de", startRequest.get("language"));
        assertEquals("NED-DBpediaSpotlight, QB-Simple", startRequest.get("componentlist[]"));
        assertEquals("urn:graph:previous", startRequest.get(QanaryStandardWebParameters.PRIORCONVERSATION));
    }

    @Test
    void executionFallsBackToEnglishWithoutLanguageAnnotation() throws Exception {
        annotateAnsweredQuestion("Who wrote Faust?", null);

        ResponseEntity<GerbilExecuteResponse> response = controller.gerbil("Who wrote Faust?", null,
                URI.create(""), "QB-Simple");

        JSONObject body = new JSONObject(Jackson2ObjectMapperBuilder.json().build()
                .writeValueAsString(response.getBody()));
        JSONObject question = body.getJSONArray("questions").getJSONObject(0);
        assertEquals("en", question.getJSONArray("question").getJSONObject(0).getString("language"));
        assertEquals("Who wrote Faust?", question.getJSONArray("question").getJSONObject(0).getString("string"));
        // an empty prior conversation is not passed on to the pipeline
        assertFalse(pipeline.receivedStartRequests().get(0).containsKey(QanaryStandardWebParameters.PRIORCONVERSATION));
        assertNull(pipeline.receivedStartRequests().get(0).get(QanaryStandardWebParameters.PRIORCONVERSATION));
    }

    @Test
    void executionIsReachableThroughTheGerbilUrl() throws Exception {
        annotateAnsweredQuestion("How high is the Zugspitze?", "de");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post(QanaryGerbilController.GERBILEXECUTE + "NED-DBpediaSpotlight, QB-Simple")
                        .param("query", "How high is the Zugspitze?")
                        .param("lang", "de"))
                .andExpect(status().isOk())
                .andExpect(header().string(QanaryStandardWebParameters.QANARYGRAPHHEADERNAME, GRAPH.toString()));

        assertEquals("NED-DBpediaSpotlight, QB-Simple", pipeline.receivedStartRequests().get(0).get("componentlist[]"));
    }
}
