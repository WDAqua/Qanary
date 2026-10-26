package eu.wdaqua.qanary.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.servlet.ServletException;

import org.apache.jena.rdf.model.ModelFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import eu.wdaqua.qanary.QanaryComponentRegistrationChangeNotifier;
import eu.wdaqua.qanary.business.QanaryComponent;
import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.business.TriplestoreEndpointIdentifier;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.exceptions.AdditionalTriplesCouldNotBeAdded;
import eu.wdaqua.qanary.exceptions.QanaryExceptionServiceCallNotOk;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.exceptions.TripleStoreNotProvided;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringFinished;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;
import eu.wdaqua.qanary.message.QanaryQuestionCreated;
import eu.wdaqua.qanary.web.messages.AdditionalTriples;
import eu.wdaqua.qanary.web.messages.NumberOfAnnotationsResponse;
import eu.wdaqua.qanary.web.messages.QanaryQuestionAnsweringError;
import eu.wdaqua.qanary.web.messages.QanaryQuestionAnsweringError.ErrorType;

/**
 * Tests the question answering process controller without a Spring context:
 * the triplestore is a real in-memory store (so the written annotations can be
 * checked), the question texts are served by a local HTTP stub, and the
 * component calls are simulated by a mocked {@link QanaryConfigurator}.
 * <p>
 * The controller keeps its process log and error log in static maps, so every
 * test works with fresh, UUID-based question URIs.
 */
class QanaryQuestionAnsweringControllerUnitTest {

    private static final URI ENDPOINT = URI.create("urn:test:qanary-triplestore");
    private static final String QA = "http://www.wdaqua.eu/qa#";
    private static final String OA = "http://www.w3.org/ns/openannotation/core/";
    private static final String RDF_TYPE = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    private static final String RDF_VALUE = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#value>";

    @TempDir
    Path additionalTriplesDirectory;

    private InMemoryQanaryTripleStore triplestore;
    private LocalQanaryHttpStub questionServer;
    private QanaryConfigurator configurator;
    private QanaryQuestionController questionController;
    private QanaryComponentRegistrationChangeNotifier notifier;
    private QanaryPipelineConfiguration pipelineConfiguration;
    private TriplestoreEndpointIdentifier endpointIdentifier;
    private MockEnvironment environment;
    private QanaryQuestionAnsweringController controller;
    private MockMvc mvc;

    private final Map<String, QanaryComponent> registeredComponents = new LinkedHashMap<>();
    // what the (simulated) components do with the message, e.g. write annotations
    private Consumer<QanaryMessage> componentBehaviour = message -> {
    };

    @BeforeEach
    void setUp() throws Exception {
        triplestore = new InMemoryQanaryTripleStore();
        questionServer = new LocalQanaryHttpStub();

        configurator = mock(QanaryConfigurator.class);
        when(configurator.getEndpoint()).thenReturn(ENDPOINT);
        when(configurator.getQanaryTripleStoreConnector()).thenReturn(triplestore.proxy());
        when(configurator.getQanaryOntology()).thenReturn(URI.create("urn:test:qanary-ontology"));
        when(configurator.getHost()).thenReturn("http://127.0.0.1");
        when(configurator.getPort()).thenReturn(8080);
        when(configurator.callServices(anyList(), any())).thenAnswer(invocation -> {
            QanaryMessage message = invocation.getArgument(1);
            componentBehaviour.accept(message);
            QanaryQuestionAnsweringFinished finished = new QanaryQuestionAnsweringFinished(message);
            finished.startQuestionAnswering();
            finished.endQuestionAnswering();
            return finished;
        });

        questionController = mock(QanaryQuestionController.class);
        when(questionController.storeQuestion(anyString()))
                .thenAnswer(invocation -> storedQuestion(invocation.getArgument(0)));
        when(questionController.storeAudioQuestion(any())).thenAnswer(invocation -> storedQuestion("(audio)"));

        registeredComponents.put("NED-DBpediaSpotlight",
                new QanaryComponent("NED-DBpediaSpotlight", "http://127.0.0.1:1/ned", true));
        registeredComponents.put("QB-Simple", new QanaryComponent("QB-Simple", "http://127.0.0.1:1/qb", true));
        notifier = mock(QanaryComponentRegistrationChangeNotifier.class);
        when(notifier.getAvailableComponentsFromNames(anyList())).thenAnswer(invocation -> {
            List<String> names = invocation.getArgument(0);
            return names.stream().filter(registeredComponents::containsKey).map(registeredComponents::get).toList();
        });

        environment = new MockEnvironment()
                .withProperty("qanary.process.additional-triples-directory", additionalTriplesDirectory.toString())
                .withProperty("spring.application.name", "test pipeline");
        pipelineConfiguration = mock(QanaryPipelineConfiguration.class);
        when(pipelineConfiguration.getAdditionalTriplesAllowed()).thenReturn(true);
        when(pipelineConfiguration.getEnv()).thenReturn(environment);
        when(pipelineConfiguration.getTriplestoreAsURI()).thenReturn(URI.create("http://127.0.0.1:8890/sparql"));
        when(pipelineConfiguration.getTriplestore()).thenReturn("http://127.0.0.1:8890/sparql");

        endpointIdentifier = mock(TriplestoreEndpointIdentifier.class);
        when(endpointIdentifier.getSelectEndpoint(any())).thenReturn(URI.create("http://127.0.0.1:8890/sparql/select"));

        controller = new QanaryQuestionAnsweringController(configurator, questionController, notifier,
                pipelineConfiguration, endpointIdentifier, triplestore.proxy(), environment);

        StringToTtlConverter ttlConverter = new StringToTtlConverter();
        ReflectionTestUtils.setField(ttlConverter, "environment", environment);
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(ttlConverter);
        mvc = MockMvcBuilders.standaloneSetup(controller).setConversionService(conversionService).build();
    }

    @AfterEach
    void tearDown() {
        questionServer.close();
    }

    private QanaryQuestionCreated storedQuestion(String text) {
        String id = UUID.randomUUID().toString();
        return new QanaryQuestionCreated(id, questionServer.addQuestion(id, text));
    }

    private static JSONObject json(MvcResult result) throws Exception {
        return new JSONObject(result.getResponse().getContentAsString());
    }

    private boolean graphContains(String graph, String triplePattern) throws SparqlQueryFailed {
        return triplestore.contains(graph, triplePattern);
    }

    private List<QanaryQuestionAnsweringError> errorsFor(URI question) {
        ResponseEntity<?> response = controller.getQuestionAnsweringErrors(question);
        @SuppressWarnings("unchecked")
        List<QanaryQuestionAnsweringError> errors = (List<QanaryQuestionAnsweringError>) response.getBody();
        return errors;
    }

    // ---- starting a process with a textual question --------------------------------

    @Test
    void jsonRequestStoresTheQuestionWithItsMetadata() throws Exception {
        JSONObject request = new JSONObject()
                .put("question", "What is the capital of Saxony?")
                .put("language", "en")
                .put("targetdata", new JSONArray(List.of("http://dbpedia.org")))
                .put("priorConversation", "urn:graph:earlier");

        MvcResult result = mvc.perform(post("/startquestionansweringwithtextquestion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        JSONObject run = json(result);
        String graph = run.getString("inGraph");
        String question = run.getString("question");
        assertEquals(graph, run.getString("outGraph"));
        assertEquals(ENDPOINT.toString(), run.getString("endpoint"));
        assertTrue(question.startsWith(questionServer.baseUrl() + "/question/"), question);

        // the question and its metadata are in the new graph
        assertTrue(graphContains(graph, "<" + question + "> " + RDF_TYPE + " <" + QA + "Question>"));
        assertTrue(graphContains(graph, "<" + question + "> <" + QA + "priorConversation> <urn:graph:earlier>"));
        assertTrue(graphContains(graph, "?a " + RDF_TYPE + " <" + QA + "AnnotationOfTextRepresentation> ; <" + OA
                + "hasBody> <" + question + ">"));
        assertTrue(graphContains(graph, "?a " + RDF_TYPE + " <" + QA + "AnnotationOfQuestionLanguage>"));
        assertTrue(graphContains(graph, "?a " + RDF_TYPE + " <" + QA + "AnnotationDataset>"));
        // the ontologies are requested via LOAD (not executed in this offline test)
        assertTrue(triplestore.skippedLoads().stream().anyMatch(load -> load.contains("urn:test:qanary-ontology")));
    }

    @Test
    void jsonEndpointForSwaggerAddsAdditionalTriplesToTheGraph() throws Exception {
        JSONObject request = new JSONObject()
                .put("question", "Who wrote Faust?")
                .put("additionalTriples", "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
                        + "PREFIX ex: <urn:qanary:test:> "
                        + "ex:goethe ex:wrote ex:faust . "
                        + "ex:faust ex:year \"1808\"^^xsd:integer ; ex:title \"Faust\" .");

        MvcResult result = mvc.perform(post("/startquestionansweringwithtextquestionThroughJson")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        String graph = json(result).getString("inGraph");
        assertTrue(graphContains(graph, "<urn:qanary:test:goethe> <urn:qanary:test:wrote> <urn:qanary:test:faust>"));
        assertTrue(graphContains(graph,
                "<urn:qanary:test:faust> <urn:qanary:test:year> \"1808\"^^<http://www.w3.org/2001/XMLSchema#integer>"));
        assertTrue(graphContains(graph, "<urn:qanary:test:faust> <urn:qanary:test:title> \"Faust\""));
        // the triples were also kept as a Turtle file
        try (Stream<Path> files = Files.list(additionalTriplesDirectory)) {
            assertEquals(1, files.count());
        }
        // no components were requested, so none were called
        verify(configurator, never()).callServices(anyList(), any());
    }

    @Test
    void additionalTriplesAreIgnoredWhenNotAllowed() throws Exception {
        when(pipelineConfiguration.getAdditionalTriplesAllowed()).thenReturn(false);
        JSONObject request = new JSONObject()
                .put("question", "Who wrote Faust?")
                .put("additionalTriples", "<urn:qanary:test:goethe> <urn:qanary:test:wrote> <urn:qanary:test:faust> .");

        MvcResult result = mvc.perform(post("/startquestionansweringwithtextquestion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        assertFalse(graphContains(json(result).getString("inGraph"), "<urn:qanary:test:goethe> ?p ?o"));
    }

    @Test
    void malformedAdditionalTriplesAreRejected() {
        JSONObject request = new JSONObject()
                .put("question", "Who wrote Faust?")
                .put("additionalTriples", "<urn:qanary:test:goethe <urn:qanary:test:wrote> .");

        ServletException e = assertThrows(ServletException.class,
                () -> mvc.perform(post("/startquestionansweringwithtextquestion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString())));
        assertInstanceOf(AdditionalTriplesCouldNotBeAdded.class, e.getCause());
    }

    @Test
    void failingTriplestoreWhileAddingTriplesIsReportedAsSparqlFailure() {
        triplestore.failUpdatesMatching(query -> query.contains("urn:qanary:test:goethe"));
        JSONObject request = new JSONObject()
                .put("question", "Who wrote Faust?")
                .put("additionalTriples", "<urn:qanary:test:goethe> <urn:qanary:test:wrote> <urn:qanary:test:faust> .");

        ServletException e = assertThrows(ServletException.class,
                () -> mvc.perform(post("/startquestionansweringwithtextquestion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString())));
        assertInstanceOf(SparqlQueryFailed.class, e.getCause());
    }

    @Test
    void formRequestStartsAProcess() throws Exception {
        MvcResult result = mvc.perform(post("/startquestionansweringwithtextquestion")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param(QanaryStandardWebParameters.QUESTION, "How many people live in Madrid?")
                        .param(QanaryStandardWebParameters.COMPONENTLIST, "QB-Simple")
                        .param(QanaryStandardWebParameters.LANGUAGE, "en")
                        .param(QanaryStandardWebParameters.ADDITIONALTRIPLES, "<urn:qanary:test:madrid> <urn:qanary:test:a> <urn:qanary:test:City> ."))
                .andExpect(status().isCreated())
                .andReturn();

        JSONObject run = json(result);
        String graph = run.getString("inGraph");
        assertTrue(graphContains(graph, "<urn:qanary:test:madrid> <urn:qanary:test:a> <urn:qanary:test:City>"));
        assertTrue(graphContains(graph, "<" + run.getString("question") + "> " + RDF_TYPE + " <" + QA + "Question>"));
        // the requested component was called with the message of the new graph ...
        verify(configurator).callServices(
                org.mockito.ArgumentMatchers.argThat(components -> components.size() == 1
                        && components.get(0).getName().equals("QB-Simple")),
                org.mockito.ArgumentMatchers.argThat(message -> message.getInGraph().toString().equals(graph)));
        // ... and the finished process is listed
        assertNotNull(controller.getLastQanaryQuestionAnsweringProcesses().getBody().get(URI.create(graph)));
        assertNotNull(controller.getQuestionAnsweringGraphInformation(URI.create(graph)).getBody());
    }

    // ---- starting a process with an audio question -----------------------------------

    @Test
    void audioQuestionIsStoredAndMarkedAsAudio() throws Exception {
        MockMultipartFile audio = new MockMultipartFile(QanaryStandardWebParameters.QUESTION, "question.mp3",
                "audio/mpeg", new byte[]{1, 2, 3});

        MvcResult result = mvc.perform(multipart("/startquestionansweringwithaudioquestion").file(audio)
                        .param(QanaryStandardWebParameters.LANGUAGE, "de")
                        .param(QanaryStandardWebParameters.TARGETDATA, "http://dbpedia.org"))
                .andExpect(status().isCreated())
                .andReturn();

        JSONObject run = json(result);
        String graph = run.getString("inGraph");
        assertTrue(graphContains(graph, "?a " + RDF_TYPE + " <" + QA + "AnnotationOfAudioRepresentation> ; <" + OA
                + "hasBody> <" + run.getString("question") + ">"));
        assertFalse(graphContains(graph, "?a " + RDF_TYPE + " <" + QA + "AnnotationOfTextRepresentation>"));
        verify(questionController).storeAudioQuestion(any());
        verify(questionController, never()).storeQuestion(anyString());
    }

    // ---- POST /questionanswering ---------------------------------------------------------

    @Test
    void questionAnsweringEndpointStartsAProcessForATextQuestion() throws Exception {
        MvcResult result = mvc.perform(post(QanaryQuestionAnsweringController.QUESTIONANSWERING)
                        .param(QanaryStandardWebParameters.TEXTQUESTION, "Is Berlin the capital of Germany?")
                        .param(QanaryStandardWebParameters.COMPONENTLIST, "NED-DBpediaSpotlight"))
                .andExpect(status().isCreated())
                .andReturn();

        JSONObject run = json(result);
        assertTrue(graphContains(run.getString("inGraph"),
                "<" + run.getString("question") + "> " + RDF_TYPE + " <" + QA + "Question>"));
    }

    @Test
    void withoutQuestionAndGraphNoProcessCanBeStarted() {
        Exception e = assertThrows(Exception.class,
                () -> controller.createOrUpdateAndRunQuestionAnsweringSystemHelper(null, "  ", null, List.of(),
                        null, null, null, null));
        assertTrue(e.getMessage().startsWith("graph URI was not provided"), e.getMessage());
    }

    @Test
    void legacyEntryPointRunsComponentsOnAnExistingGraph() throws Exception {
        URI graph = URI.create("urn:graph:" + UUID.randomUUID());
        URI question = URI.create("urn:qanary:question:" + UUID.randomUUID());
        triplestore.update("INSERT DATA { GRAPH <" + graph + "> { <" + question + "> " + RDF_TYPE + " <" + QA
                + "Question> } }");
        String message = new JSONObject().put("values", new JSONObject()
                .put("urn:qanary#endpoint", ENDPOINT.toString())
                .put("urn:qanary#inGraph", graph.toString())
                .put("urn:qanary#outGraph", graph.toString())).toString();

        ResponseEntity<?> response = controller.questionansweringLegacy(List.of("QB-Simple"), message);

        assertEquals(200, response.getStatusCode().value());
        QanaryQuestionAnsweringRun run = (QanaryQuestionAnsweringRun) response.getBody();
        assertEquals(question, run.getQuestion());
        assertEquals(graph, run.getOutGraph());
    }

    // ---- component execution and the error log -----------------------------------------------

    @Test
    void unavailableComponentsAreRecordedAsErrors() throws Exception {
        URI question = URI.create("urn:qanary:question:" + UUID.randomUUID());
        URI graph = URI.create("urn:graph:" + UUID.randomUUID());

        controller.executeComponentList(question, List.of("QB-Simple", "NoSuchComponent"),
                new QanaryMessage(ENDPOINT, graph));

        List<QanaryQuestionAnsweringError> errors = errorsFor(question);
        assertEquals(1, errors.size());
        assertEquals(ErrorType.COMPONENT_NOT_AVAILABLE, errors.get(0).getType());
        assertEquals("NoSuchComponent", errors.get(0).getComponent());

        // also visible through the web endpoint, with and without question filter
        mvc.perform(get(QanaryQuestionAnsweringController.QUESTIONANSWERING + "/errors")
                        .param("questionuri", question.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("COMPONENT_NOT_AVAILABLE"))
                .andExpect(jsonPath("$[0].component").value("NoSuchComponent"));
        @SuppressWarnings("unchecked")
        Map<URI, List<QanaryQuestionAnsweringError>> all =
                (Map<URI, List<QanaryQuestionAnsweringError>>) controller.getQuestionAnsweringErrors(null).getBody();
        assertEquals(1, all.get(question).size());
    }

    @Test
    void failingComponentIsRecordedAndReported() throws Exception {
        URI question = URI.create("urn:qanary:question:" + UUID.randomUUID());
        when(configurator.callServices(anyList(), any())).thenThrow(new QanaryExceptionServiceCallNotOk("QB-Simple",
                12, "HTTP 500 from component", "stacktrace"));

        QanaryExceptionServiceCallNotOk e = assertThrows(QanaryExceptionServiceCallNotOk.class,
                () -> controller.executeComponentList(question, List.of("QB-Simple"),
                        new QanaryMessage(ENDPOINT, URI.create("urn:graph:" + UUID.randomUUID()))));

        assertEquals("QB-Simple", e.getComponentName());
        List<QanaryQuestionAnsweringError> errors = errorsFor(question);
        assertEquals(ErrorType.COMPONENT_EXECUTION_FAILED, errors.get(0).getType());
        assertEquals("QB-Simple", errors.get(0).getComponent());
    }

    @Test
    void runtimeErrorsAreClassifiedAsTriplestoreOrPipelineFailures() throws Exception {
        assertEquals(ErrorType.TRIPLESTORE_NOT_ACCESSIBLE,
                errorAfterRuntimeFailure(new IllegalStateException("SPARQL endpoint: Connection refused")));
        assertEquals(ErrorType.TRIPLESTORE_NOT_ACCESSIBLE,
                errorAfterRuntimeFailure(new IllegalStateException("Virtuoso JDBC driver error")));
        assertEquals(ErrorType.PIPELINE_FAILURE, errorAfterRuntimeFailure(new IllegalStateException("boom")));
        assertEquals(ErrorType.PIPELINE_FAILURE, errorAfterRuntimeFailure(new NullPointerException()));
    }

    private ErrorType errorAfterRuntimeFailure(RuntimeException failure) throws Exception {
        URI question = URI.create("urn:qanary:question:" + UUID.randomUUID());
        doThrow(failure).when(configurator).callServices(anyList(), any());
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> controller.executeComponentList(question, List.of("QB-Simple"),
                        new QanaryMessage(ENDPOINT, URI.create("urn:graph:" + UUID.randomUUID()))));
        assertSame(failure, thrown);
        List<QanaryQuestionAnsweringError> errors = errorsFor(question);
        assertNull(errors.get(0).getComponent());
        return errors.get(0).getType();
    }

    @Test
    void errorsWithoutQuestionAreNotRecorded() throws Exception {
        controller.executeComponentList(null, List.of("NoSuchComponent"),
                new QanaryMessage(ENDPOINT, URI.create("urn:graph:" + UUID.randomUUID())));
        @SuppressWarnings("unchecked")
        Map<URI, List<QanaryQuestionAnsweringError>> all =
                (Map<URI, List<QanaryQuestionAnsweringError>>) controller.getQuestionAnsweringErrors(null).getBody();
        assertFalse(all.containsKey(null));
    }

    @Test
    void emptyComponentListRunsNothing() throws Exception {
        URI question = URI.create("urn:qanary:question:" + UUID.randomUUID());
        URI graph = URI.create("urn:graph:" + UUID.randomUUID());

        QanaryQuestionAnsweringRun run = controller.executeComponentList(question, List.of(),
                new QanaryMessage(ENDPOINT, graph));

        assertEquals(question, run.getQuestion());
        assertEquals(graph, run.getInGraph());
        verify(configurator, never()).callServices(anyList(), any());
    }

    @Test
    void processAndErrorLogsKeepOnlyTheLatestThousandEntries() throws Exception {
        List<URI> questions = new ArrayList<>();
        List<URI> graphs = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            URI question = URI.create("urn:qanary:question:bounded:" + UUID.randomUUID());
            URI graph = URI.create("urn:graph:bounded:" + UUID.randomUUID());
            controller.executeComponentList(question, List.of("NoSuchComponent"), new QanaryMessage(ENDPOINT, graph));
            questions.add(question);
            graphs.add(graph);
        }

        Map<URI, QanaryQuestionAnsweringFinished> processes = controller.getLastQanaryQuestionAnsweringProcesses()
                .getBody();
        assertEquals(1000, processes.size());
        assertFalse(processes.containsKey(graphs.get(0)));
        assertTrue(processes.containsKey(graphs.get(1000)));
        assertTrue(errorsFor(questions.get(0)).isEmpty());
        assertEquals(1, errorsFor(questions.get(1000)).size());
    }

    // ---- other endpoints --------------------------------------------------------------------

    @Test
    void numberOfAnnotationsCountsTheAnnotationsOfAComponent() throws Exception {
        String graph = "urn:graph:" + UUID.randomUUID();
        triplestore.update("INSERT DATA { GRAPH <" + graph + "> { "
                + "<urn:a1> <" + OA + "annotatedBy> <urn:qanary:NED-DBpediaSpotlight> . "
                + "<urn:a2> <" + OA + "annotatedBy> <urn:qanary:NED-DBpediaSpotlight> . "
                + "<urn:a3> <" + OA + "annotatedBy> <urn:qanary:QB-Simple> . } }");

        mvc.perform(get("/numberOfAnnotations/").param("component", "NED-DBpediaSpotlight").param("graph", graph))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annotationCount").value(2))
                .andExpect(jsonPath("$.componentUrl").value("urn:qanary:NED-DBpediaSpotlight"))
                .andExpect(jsonPath("$.graph").value(graph));

        NumberOfAnnotationsResponse none = controller.getNumberOfAnnotationsForComponent("Unknown", graph).getBody();
        assertEquals(0, none.getAnnotationCount());
        assertNull(none.getComponentUrl());
    }

    @Test
    void ontologyFilesAreServed() throws Exception {
        mvc.perform(get("/qanaryOntology.ttl")).andExpect(status().isOk());
        mvc.perform(get("/oa.owl")).andExpect(status().isOk());
        ClassPathResource ontology = controller.getFile2();
        assertTrue(ontology.exists());
        assertTrue(controller.getFile1().exists());
    }

    @Test
    void processInformationIsAvailableAsJson() throws Exception {
        mvc.perform(get(QanaryQuestionAnsweringController.QUESTIONANSWERING))
                .andExpect(status().isOk());
        URI unknownGraph = URI.create("urn:graph:" + UUID.randomUUID());
        assertNull(controller.getQuestionAnsweringGraphInformation(unknownGraph).getBody());
    }

    @Test
    void updatingAndDeletingRunsIsNotImplemented() {
        String runPath = QanaryQuestionAnsweringController.QUESTIONANSWERING + "/" + UUID.randomUUID();
        ServletException deleted = assertThrows(ServletException.class, () -> mvc.perform(delete(runPath)));
        assertEquals("not yet implemented", deleted.getCause().getMessage());
        ServletException updated = assertThrows(ServletException.class, () -> mvc.perform(put(runPath)));
        assertEquals("not yet implemented", updated.getCause().getMessage());
    }

    // ---- HTML forms and model attributes --------------------------------------------------------

    @Test
    void htmlFormsExposePipelinePropertiesInTheSession() {
        MockHttpSession session = new MockHttpSession();
        assertEquals("startquestionansweringwithtextquestion", controller.startquestionansweringwithtextquestion(
                new MockHttpServletResponse(), ModelFactory.createDefaultModel(), session));
        assertEquals("test pipeline", session.getAttribute("spring_application_name"));
        assertTrue(((String) session.getAttribute("spring_boot_admin_url")).contains("is not defined"));
        assertEquals("/description", session.getAttribute("component_description_url"));
        assertEquals("/component-description", session.getAttribute("rdfcomponentdescription"));

        MockHttpSession audioSession = new MockHttpSession();
        assertEquals("startquestionansweringwithaudioquestion", controller.startquestionansweringwithaudioquestion(
                new MockHttpServletResponse(), ModelFactory.createDefaultModel(), audioSession));
        assertEquals("test pipeline", audioSession.getAttribute("spring_application_name"));
    }

    @Test
    void modelAttributesDescribeComponentsAndEndpoints() throws Exception {
        when(notifier.getComponentsAndAvailability(any())).thenReturn(Map.of("QB-Simple", "UP"));
        assertEquals(Map.of("QB-Simple", "UP"), controller.componentList());
        assertEquals("http://127.0.0.1:8890/sparql/select", controller.triplestoreEndpointOfCurrentQanaryPipeline());
        assertEquals("http://127.0.0.1:8080/sparql", controller.sparqlEndpointOfCurrentQanaryPipeline());

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.setVaryResponseHeader(response);
        assertEquals("*", response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void missingTriplestoreConfigurationIsReported() throws Exception {
        when(pipelineConfiguration.getTriplestoreAsURI()).thenThrow(new TripleStoreNotProvided("none"));
        assertThrows(TripleStoreNotProvided.class, () -> controller.triplestoreEndpointOfCurrentQanaryPipeline());
    }

    @Test
    void additionalTriplesWithoutContentDoNotTouchTheGraph() throws Exception {
        QanaryQuestionAnsweringRun run = controller.createOrUpdateAndRunQuestionAnsweringSystemHelper(null,
                "Who wrote Faust?", null, List.of(), null, null, null, new AdditionalTriples("", environment));

        // only the question set-up wrote to the graph: no INSERT DATA of additional triples
        assertTrue(triplestore.executedUpdates().stream().noneMatch(update -> update.startsWith("INSERT DATA { GRAPH <"
                + run.getInGraph() + "> {\n")));
        assertTrue(graphContains(run.getInGraph().toString(),
                "<" + run.getQuestion() + "> " + RDF_TYPE + " <" + QA + "Question>"));
        try (Stream<Path> files = Files.list(additionalTriplesDirectory)) {
            assertEquals(0, files.count());
        }
    }
}
