package eu.wdaqua.qanary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.values.StatusInfo;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.explainability.QanaryExplanationData;

/**
 * Explanations of a pipeline are assembled from the explanations of the
 * components that took part in a question answering process. The process data
 * lives in an in-memory triple store, the components and the explanation
 * service are served by a local HTTP stub.
 */
class PipelineExplanationCoverageTest {

    static final String PAC_GRAPH = "urn:graph:pipeline-as-component";
    static final String OTHER_GRAPH = "urn:graph:other-process";
    static final String QUESTION = "http://127.0.0.1/question/stored-question__text_1";

    private CoverageHttpStub stub;
    private QanaryComponentRegistrationChangeNotifier notifier;
    private PipelineExplanationHelper helper;

    @BeforeEach
    void setUp() throws Exception {
        stub = new CoverageHttpStub() //
                .respond("/explanation-service", 200, "text/plain", "the pipeline answered via NED and QB") //
                .respond("/ned/explain", 200, "text/plain", "NED found dbr:Leipzig") //
                .respond("/qb/explain", 200, "text/plain", "QB built a SELECT query");

        // the in-memory store behind the proxy holds two processes; only the first
        // one was annotated by both components of the pipeline
        QanaryTripleStoreProxy store = new QanaryTripleStoreProxy();
        store.update("PREFIX owl: <http://www.w3.org/2002/07/owl#> " //
                + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
                + "INSERT DATA { " //
                + "  GRAPH <" + PAC_GRAPH + "> { " //
                + "    <" + QUESTION + "> owl:sameAs <urn:qanary:currentQuestion> . " //
                + "    <urn:annotation:1> oa:annotatedBy <urn:qanary:NED-Comp> . " //
                + "    <urn:annotation:2> oa:annotatedBy <urn:qanary:QB-Comp> . } " //
                + "  GRAPH <" + OTHER_GRAPH + "> { " //
                + "    <" + QUESTION + "> owl:sameAs <urn:qanary:currentQuestion> . " //
                + "    <urn:annotation:3> oa:annotatedBy <urn:qanary:NED-Comp> . } " //
                + "}");

        notifier = new QanaryComponentRegistrationChangeNotifier(mock(InstanceRepository.class));
        notifier.getAvailableComponents().put("NED-Comp", QanaryComponentRegistrationChangeNotifierCoverageTest
                .instance("NED-Comp", stub.url() + "/ned", StatusInfo.ofUp()));
        notifier.getAvailableComponents().put("QB-Comp", QanaryComponentRegistrationChangeNotifierCoverageTest
                .instance("QB-Comp", stub.url() + "/qb", StatusInfo.ofUp()));

        helper = new PipelineExplanationHelper();
        helper.setupWebclient(stub.url() + "/explanation-service");
        ReflectionTestUtils.setField(helper, "qanaryTripleStoreConnector", store);
        ReflectionTestUtils.setField(helper, "qanaryComponentRegistrationChangeNotifier", notifier);
        ReflectionTestUtils.setField(helper, "QANARY_COMPONENTS", List.of("NED-Comp", "QB-Comp"));
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    @Test
    void componentTriplesRequireAnAnnotationOfEveryPredefinedComponent() {
        assertEquals("?NEDComp oa:annotatedBy <urn:qanary:NED-Comp> ." //
                + "?QBComp oa:annotatedBy <urn:qanary:QB-Comp> .", helper.createComponentTriples());
    }

    @Test
    void graphOfAQuestionIsTheOneAnnotatedByAllPredefinedComponents() throws Exception {
        assertEquals(PAC_GRAPH, helper.getGraphFromQuestionId(QUESTION));
    }

    @Test
    void usedComponentsAreReadFromTheAnnotationsOfTheGraph() throws Exception {
        assertEquals(new HashSet<>(List.of("NED-Comp", "QB-Comp")), new HashSet<>(helper.getUsedComponents(PAC_GRAPH)));
        assertEquals(List.of("NED-Comp"), helper.getUsedComponents(OTHER_GRAPH));
    }

    @Test
    void subComponentExplanationsAreFetchedInTheOrderOfTheRequests() throws Exception {
        List<QanaryExplanationData> requests = List.of( //
                new QanaryExplanationData(PAC_GRAPH, QUESTION, stub.url() + "/qb"), //
                new QanaryExplanationData(PAC_GRAPH, QUESTION, stub.url() + "/ned"));

        List<String> explanations = helper.fetchSubComponentExplanations(requests).collectList().block();

        assertEquals(List.of("QB built a SELECT query", "NED found dbr:Leipzig"), explanations);
        JsonNode sent = new ObjectMapper().readTree(stub.requests().get(0).body());
        assertEquals(PAC_GRAPH, sent.get("graph").asText());
        assertEquals(QUESTION, sent.get("questionId").asText());
        assertSame(notifier, helper.getQanaryComponentRegistrationChangeNotifier());
    }

    @Test
    void pipelineExplainsAProcessFromTheExplanationsOfItsComponents() throws Exception {
        QanaryPipeline pipeline = new QanaryPipeline();
        ReflectionTestUtils.setField(pipeline, "pipelineExplanationHelper", helper);
        ReflectionTestUtils.setField(pipeline, "applicationName", "MyQanaryPipeline");
        QanaryExplanationData data = new QanaryExplanationData(PAC_GRAPH, QUESTION, null);

        String explanation = pipeline.explain(data);

        assertEquals("the pipeline answered via NED and QB", explanation);
        assertEquals("MyQanaryPipeline", data.getComponent());
        // each component's explanation is attached to that component
        assertEquals(Map.of("NED-Comp", "NED found dbr:Leipzig", "QB-Comp", "QB built a SELECT query"),
                data.getExplanations());
        CoverageHttpStub.RecordedRequest toService = stub.requests().get(stub.requests().size() - 1);
        assertEquals("/explanation-service", toService.path());
        JsonNode sent = new ObjectMapper().readTree(toService.body());
        assertEquals("NED found dbr:Leipzig", sent.get("explanations").get("NED-Comp").asText());
    }

    @Test
    void pipelineAsComponentExplainsTheProcessGraphOfItsQuestion() throws Exception {
        QanaryPipelineComponent component = new QanaryPipelineComponent();
        ReflectionTestUtils.setField(component, "QANARY_COMPONENTS", List.of("NED-Comp", "QB-Comp"));
        ReflectionTestUtils.setField(component, "qanaryComponentRegistrationChangeNotifier", notifier);
        ReflectionTestUtils.setField(component, "pipelineExplanationHelper", helper);
        ReflectionTestUtils.setField(component, "env",
                new MockEnvironment().withProperty("spring.application.name", "PipelineAsComponent"));
        QanaryExplanationData data = new QanaryExplanationData(null, QUESTION, null);

        String explanation = component.explain(data);

        assertEquals("the pipeline answered via NED and QB", explanation);
        assertEquals(PAC_GRAPH, data.getGraph());
        assertEquals("PipelineAsComponent", data.getComponent());
        assertEquals(Map.of("NED-Comp", "NED found dbr:Leipzig", "QB-Comp", "QB built a SELECT query"),
                data.getExplanations());
        assertTrue(stub.requests().stream().anyMatch(r -> r.path().equals("/ned/explain")));
    }
}
