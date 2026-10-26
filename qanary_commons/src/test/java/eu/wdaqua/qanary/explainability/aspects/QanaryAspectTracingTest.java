package eu.wdaqua.qanary.explainability.aspects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorInMemory;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;

/**
 * Tests the tracing of {@link QanaryAspect}: which method executions are
 * recorded, how calls are nested via the call stack, how a cross-component
 * process id starts tracing, and that a finished trace is written to the
 * triplestore as {@code qa:AnnotationOfLogMethod} annotations.
 * <p>
 * Each test uses its own aspect instance (not the woven singleton) and an
 * in-memory triplestore; the static call stack is reset around every test.
 */
class QanaryAspectTracingTest {

    private static final String QA = "http://www.wdaqua.eu/qa#";
    private static final String PROV = "http://www.w3.org/ns/prov#";
    private static final String X = "http://example.org#";
    private static final URI GRAPH = URI.create("urn:graph:trace");
    private static final URI ENDPOINT = URI.create("http://127.0.0.1:9/sparql"); // never contacted

    private QanaryAspect aspect;
    private QanaryTripleStoreConnectorInMemory store;
    private Stack<String> previousCallStack;
    private String previousProfiles;

    @BeforeEach
    void setUp() {
        aspect = new QanaryAspect();
        previousCallStack = QanaryAspect.getCallStack();
        QanaryAspect.setCallStack(new Stack<>());
        // QanaryAspect treats an active profile containing "test" like a test on the stack
        previousProfiles = System.clearProperty("spring.profiles.active");
        store = new QanaryTripleStoreConnectorInMemory();
    }

    @AfterEach
    void tearDown() {
        QanaryAspect.setCallStack(previousCallStack);
        if (previousProfiles != null) {
            System.setProperty("spring.profiles.active", previousProfiles);
        }
    }

    // ---- configuration accessors -------------------------------------------------

    @Test
    void applicationNameDefaultsToTheQanaryPipelineInQueries() {
        assertNull(aspect.getApplicationName());
        assertEquals("QanaryPipeline", aspect.getApplicationNameForQuery());

        aspect.setApplicationName("NED-DBpediaSpotlight");

        assertEquals("NED-DBpediaSpotlight", aspect.getApplicationName());
        assertEquals("NED-DBpediaSpotlight", aspect.getApplicationNameForQuery());
    }

    @Test
    void methodListAndTriplestoreConnectorCanBeReplaced() {
        Map<String, MethodObject> methods = new HashMap<>();
        aspect.setMethodList(methods);
        aspect.setQanaryTripleStoreConnector(store);

        assertSame(methods, aspect.getMethodList());
        assertSame(store, aspect.getQanaryTripleStoreConnector());
    }

    @Test
    void afterReturningGetApplicationNameAdoptsTheReturnedName() {
        aspect.getApplicationNameAfterReturning(joinPoint("x", "getApplicationName"), "QB-SimpleRealNameOfSuperHero");

        assertEquals("QB-SimpleRealNameOfSuperHero", aspect.getApplicationName());
    }

    // ---- recording method executions -------------------------------------------------

    @Test
    void pipelineMethodsAreRecordedOnlyWhileTracingIsActive() {
        JoinPoint call = joinPoint("eu.wdaqua.qanary.QanaryPipeline", "execute", "question");

        aspect.implementationStoreMethodExecutionInComponentBeforeForPipeline(call);
        assertTrue(aspect.getMethodList().isEmpty());

        aspect.setActiveTracing(true);
        aspect.implementationStoreMethodExecutionInComponentBeforeForPipeline(call);

        assertEquals(1, aspect.getMethodList().size());
        MethodObject recorded = aspect.getMethodList().values().iterator().next();
        assertEquals("eu.wdaqua.qanary.QanaryPipeline.execute", recorded.getMethod());
        assertEquals("init", recorded.getCaller());
        assertEquals("question", recorded.getInput()[0]);
        assertEquals(1, QanaryAspect.getCallStack().size());
    }

    @Test
    void startingAPipelineProcessResetsEarlierStateAndStartsTracing() {
        aspect.getMethodList().put("stale", new MethodObject("init", "stale.method", new Object[0]));
        QanaryAspect.getCallStack().push("stale");

        aspect.startProcessForPipelineBefore(joinPoint("eu.wdaqua.qanary.web.Controller", "startquestionanswering"));

        assertTrue(aspect.isActiveTracing());
        assertEquals(1, aspect.getMethodList().size());
        assertFalse(aspect.getMethodList().containsKey("stale"));
        assertEquals("eu.wdaqua.qanary.web.Controller.startquestionanswering",
                aspect.getMethodList().values().iterator().next().getMethod());
        assertEquals(1, QanaryAspect.getCallStack().size());
    }

    @Test
    void aComponentCallWithAProcessIdHeaderStartsTracingForThatProcess() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("processId", "urn:qanary:process:42");
        JoinPoint call = joinPoint("eu.wdaqua.qanary.component.QanaryServiceController", "annotatequestion", request);
        aspect.getMethodList().put("leftover", new MethodObject("init", "leftover", new Object[0]));

        aspect.implementationStoreMethodExecutionInComponentBeforeForComponent(call);

        assertTrue(aspect.isActiveTracing());
        assertEquals("urn:qanary:process:42", aspect.getCrossComponentProcessId());
        assertEquals(1, aspect.getMethodList().size(), "earlier records are discarded");
        assertEquals("urn:qanary:process:42", aspect.getMethodList().values().iterator().next().getCaller());
    }

    @Test
    void requestsWithoutProcessIdDoNotStartTracing() {
        MockHttpServletRequest withoutHeader = new MockHttpServletRequest();
        MockHttpServletRequest emptyHeader = new MockHttpServletRequest();
        emptyHeader.addHeader("processId", "");

        assertFalse(aspect.checkForProcessIdAndSetup(new Object[] { "no request", 42 }));
        assertFalse(aspect.checkForProcessIdAndSetup(new Object[] { withoutHeader }));
        assertFalse(aspect.checkForProcessIdAndSetup(new Object[] { emptyHeader }));

        aspect.implementationStoreMethodExecutionInComponentBeforeForComponent(
                joinPoint("eu.wdaqua.qanary.component.X", "y", withoutHeader));
        assertFalse(aspect.isActiveTracing());
        assertTrue(aspect.getMethodList().isEmpty());
    }

    @Test
    void theDocstringOfTheExecutedMethodIsRecorded() {
        aspect.setActiveTracing(true);
        JoinPoint call = joinPoint(DocumentedService.class.getName(), "greet", new Class<?>[] { String.class },
                new DocumentedService(), "Ada");

        aspect.implementationStoreMethodExecutionInComponentBeforeForPipeline(call);

        String docstring = aspect.getMethodList().values().iterator().next().getDocstring();
        assertTrue(docstring.contains("Greets the given person"), docstring);
    }

    // ---- writing a finished trace to the triplestore --------------------------------------

    @Test
    void aFinishedTraceIsWrittenToTheProcessGraph() throws Exception {
        aspect.setProcessGraph(GRAPH);
        aspect.setQanaryTripleStoreConnector(store);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("processId", "urn:qanary:process:7");

        JoinPoint outer = joinPoint("eu.wdaqua.qanary.component.Component", "annotate", request);
        JoinPoint inner = joinPoint("eu.wdaqua.qanary.component.Component", "lookup", "Saxony");
        aspect.implementationStoreMethodExecutionInComponentBeforeForComponent(outer);
        aspect.implementationStoreMethodExecutionInComponentBeforeForComponent(inner);
        String outerUuid = QanaryAspect.getCallStack().firstElement();

        aspect.implementationStoreMethodExecutionInComponentAfter(inner, "dbr:Saxony");
        assertEquals(2, aspect.getMethodList().size(), "the trace is written only when the outermost call returns");
        aspect.implementationStoreMethodExecutionInComponentAfter(outer, 42);

        assertFalse(aspect.isActiveTracing());
        assertNull(aspect.getProcessGraph());
        assertTrue(aspect.getMethodList().isEmpty());

        Map<String, String> callerOf = new HashMap<>();
        Map<String, String> outputOf = new HashMap<>();
        ResultSet rows = store.select("SELECT ?name ?caller ?by ?value FROM <" + GRAPH + "> WHERE { "
                + "?a a <" + QA + "AnnotationOfLogMethod> ; <" + QA + "methodName> ?name ; <" + PROV
                + "actedOnBehalfOf> ?caller ; <http://www.w3.org/ns/openannotation/core/annotatedBy> ?by ; <" + X + "output> ?o . "
                + "?o <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> ?value }");
        while (rows.hasNext()) {
            QuerySolution row = rows.next();
            callerOf.put(row.getLiteral("name").getString(), row.getResource("caller").getURI());
            outputOf.put(row.getLiteral("name").getString(), row.getLiteral("value").getString());
            assertEquals("urn:qanary:QanaryPipeline", row.getResource("by").getURI());
        }
        assertEquals(2, callerOf.size());
        assertEquals("urn:qanary:process:7", callerOf.get("eu.wdaqua.qanary.component.Component.annotate"));
        assertTrue(callerOf.get("eu.wdaqua.qanary.component.Component.lookup").endsWith(outerUuid),
                "the inner call was made on behalf of the outer one");
        assertEquals("42", outputOf.get("eu.wdaqua.qanary.component.Component.annotate"));
        assertEquals("dbr:Saxony", outputOf.get("eu.wdaqua.qanary.component.Component.lookup"));
    }

    @Test
    void aThrowingMethodIsMarkedAndTheTraceIsStillWritten() throws Exception {
        aspect.setProcessGraph(GRAPH);
        aspect.setQanaryTripleStoreConnector(store);
        aspect.setActiveTracing(true);
        JoinPoint outer = joinPoint("eu.wdaqua.qanary.component.Component", "annotate");
        JoinPoint failing = joinPoint("eu.wdaqua.qanary.component.Component", "callService");
        aspect.implementationStoreMethodExecutionInComponentBeforeForComponent(outer);
        aspect.implementationStoreMethodExecutionInComponentBeforeForComponent(failing);
        String failingUuid = QanaryAspect.getCallStack().peek();

        aspect.implementationStoreMethodExecutionInComponentThrowing(failing);

        MethodObject failed = aspect.getMethodList().get(failingUuid);
        assertTrue(failed.isErrorOccurred());
        assertNull(failed.getOutput());

        aspect.implementationStoreMethodExecutionInComponentThrowing(outer);
        assertFalse(aspect.isActiveTracing());
        assertEquals(2, countLoggedMethods());
    }

    @Test
    void endOfAPipelineProcessWritesTheTrace() throws Throwable {
        aspect.setProcessGraph(GRAPH);
        aspect.setQanaryTripleStoreConnector(store);
        JoinPoint start = joinPoint("eu.wdaqua.qanary.web.QanaryQuestionAnsweringController", "start");
        aspect.startProcessForPipelineBefore(start);

        aspect.endProcessForPipeline(start, "finished");

        assertEquals(1, countLoggedMethods());
        assertFalse(aspect.isActiveTracing());
    }

    @Test
    void withoutTracingNothingIsUpdatedOrWritten() throws Exception {
        aspect.setProcessGraph(GRAPH);
        aspect.setQanaryTripleStoreConnector(store);
        QanaryAspect.getCallStack().push("foreign");

        aspect.implementationStoreMethodExecutionInComponentAfter(joinPoint("a.B", "c"), "x");
        aspect.implementationStoreMethodExecutionInComponentThrowing(joinPoint("a.B", "c"));

        assertEquals("foreign", QanaryAspect.getCallStack().peek());
        assertEquals(0, countLoggedMethods());
    }

    @Test
    void anOutputWithoutStringFormIsLoggedByItsClass() throws Exception {
        aspect.setProcessGraph(GRAPH);
        aspect.setQanaryTripleStoreConnector(store);
        MethodObject method = new MethodObject("urn:qanary:caller", "a.B.c", new Object[] { "in" });
        method.setDocstring("");
        method.setOutput(new WithoutStringForm());

        aspect.logMethodData("urn:qanary:method:1", method);

        ResultSet rows = store.select("SELECT ?value FROM <" + GRAPH + "> WHERE { <urn:qanary:method:1> <" + X
                + "output> ?o . ?o <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> ?value }");
        assertEquals("class " + WithoutStringForm.class.getName(), rows.next().getLiteral("value").getString());
    }

    @Test
    void loggingProblemsNeverEscape() {
        aspect.setQanaryTripleStoreConnector(store); // but no process graph: every log write fails
        Map<String, MethodObject> methods = new HashMap<>();
        methods.put("urn:qanary:method:1", new MethodObject("init", "a.B.c", new Object[0]));

        assertDoesNotThrow(() -> aspect.logMethods(methods));
        assertDoesNotThrow(() -> aspect.logMethods(null));
    }

    // ---- data representations ----------------------------------------------------------

    @Test
    void inputRepresentationsCoverEmptyNullAndEmptyStringArguments() {
        assertEquals("()", aspect.generateInputDataRepresentation(new Object[0]));
        assertEquals("([ rdf:type \"null\";rdf:value \"null\" ]"
                + "[ rdf:type \"class java.lang.String\";rdf:value \"null\" ])",
                aspect.generateInputDataRepresentation(new Object[] { null, "" }));
        assertEquals("[]", aspect.generateOutputDataRepresentation(null));
    }

    // ---- setting process graph and triplestore -------------------------------------------

    @Test
    void processExecutionSetsTheGraphAndAnInternalConnectorForTheComponent() throws Exception {
        aspect.setApplicationName("NED-test");
        QanaryMessage message = new QanaryMessage(ENDPOINT, GRAPH);

        aspect.setGraphFromProcessExecution(joinPoint("eu.wdaqua.qanary.component.C", "process", message));

        assertEquals(GRAPH, aspect.getProcessGraph());
        QanaryTripleStoreConnectorQanaryInternal connector = assertInstanceOf(
                QanaryTripleStoreConnectorQanaryInternal.class, aspect.getQanaryTripleStoreConnector());
        assertEquals("urn:qanary:NED-test", connector.getApplicationName());
        assertFalse(aspect.isActiveTracing(), "a QanaryMessage is no HTTP request with a process id");
    }

    @Test
    void processExecutionKeepsAnAlreadyConfiguredConnector() throws Exception {
        aspect.setQanaryTripleStoreConnector(store);

        aspect.setGraphFromProcessExecution(
                joinPoint("eu.wdaqua.qanary.component.C", "process", new QanaryMessage(ENDPOINT, GRAPH)));

        assertSame(store, aspect.getQanaryTripleStoreConnector());
        assertEquals(GRAPH, aspect.getProcessGraph());
    }

    /**
     * Characterises a production guard, not a requirement: QanaryAspect#isTestEnvironment
     * (QanaryAspect.java:452-457) skips the pipeline advice whenever a class whose name
     * contains "Test" is on the stack. Adapt this test when that guard is replaced.
     */
    @Test
    void thePipelineAdviceDoesNothingInsideTests() throws Exception {
        aspect.setTriplestoreAndGraphForPipeline(
                joinPoint("eu.wdaqua.qanary.web.C", "executeComponentList", "a", "b", new QanaryMessage(ENDPOINT, GRAPH)));

        assertNull(aspect.getProcessGraph());
        assertNull(aspect.getQanaryTripleStoreConnector());
    }

    @Test
    void thePipelineAdviceSetsTheOutGraphAndAnInternalConnector() throws Throwable {
        URI outGraph = URI.create("urn:graph:out");
        JoinPoint call = joinPoint("eu.wdaqua.qanary.web.C", "executeComponentList", "a", "b",
                new QanaryMessage(ENDPOINT, GRAPH, outGraph));

        PipelineAdviceRunner.runOutsideOfTests(aspect, call);

        assertEquals(outGraph, aspect.getProcessGraph());
        QanaryTripleStoreConnectorQanaryInternal connector = assertInstanceOf(
                QanaryTripleStoreConnectorQanaryInternal.class, aspect.getQanaryTripleStoreConnector());
        assertEquals("urn:qanary:QanaryPipeline", connector.getApplicationName());
    }

    @Test
    void thePipelineAdviceKeepsAnAlreadyConfiguredConnector() throws Throwable {
        aspect.setQanaryTripleStoreConnector(store);
        JoinPoint call = joinPoint("eu.wdaqua.qanary.web.C", "executeComponentList", "a", "b",
                new QanaryMessage(ENDPOINT, GRAPH));

        PipelineAdviceRunner.runOutsideOfTests(aspect, call);

        assertSame(store, aspect.getQanaryTripleStoreConnector());
        assertEquals(GRAPH, aspect.getProcessGraph());
    }

    // ---- helpers -------------------------------------------------------------------------

    private int countLoggedMethods() throws Exception {
        ResultSet rows = store.select("SELECT (COUNT(?a) AS ?n) FROM <" + GRAPH + "> WHERE { ?a a <" + QA
                + "AnnotationOfLogMethod> }");
        return rows.next().getLiteral("n").getInt();
    }

    private static JoinPoint joinPoint(String declaringType, String name, Object... args) {
        return joinPoint(declaringType, name, new Class<?>[0], new Object(), args);
    }

    private static JoinPoint joinPoint(String declaringType, String name, Class<?>[] parameterTypes, Object target,
            Object... args) {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getName()).thenReturn(name);
        when(signature.getDeclaringTypeName()).thenReturn(declaringType);
        when(signature.getParameterTypes()).thenReturn(parameterTypes);
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.getTarget()).thenReturn(target);
        return joinPoint;
    }

    /** a traced service whose method documentation the aspect records */
    public static class DocumentedService {
        /**
         * Greets the given person.
         */
        public String greet(String name) {
            return "Hello " + name;
        }
    }

    /** an output whose {@code toString()} yields no text */
    static class WithoutStringForm {
        @Override
        public String toString() {
            return null;
        }
    }
}
