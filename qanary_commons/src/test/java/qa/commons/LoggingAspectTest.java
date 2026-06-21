package qa.commons;

import eu.wdaqua.qanary.explainability.aspects.QanaryAspect;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.jena.update.UpdateFactory;

import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoggingAspectTest {

    /*
     * private URI TEST_GRAPH = new URI("test-graph");
     * private URI TEST_ENDPOINT = new URI("test-endpoint");
     */

    private final String CROSS_COMPONENT_PROCESS_ID_EXAMPLE = "crossComponentProcessId";
    private QanaryAspect qanaryAspect;
    private JoinPoint joinPoint;
    private Signature signature;

    @BeforeEach
    public void setup() {
        this.qanaryAspect = new QanaryAspect();
        QanaryAspect.setCallStack(new Stack<>());

        // Use MethodSignature instead of basic Signature
        joinPoint = mock(JoinPoint.class);
        signature = mock(org.aspectj.lang.reflect.MethodSignature.class);

        when(signature.getName()).thenReturn("sampleMethodName");
        when(signature.toShortString()).thenReturn("sampleMethodSignature");

        when(((org.aspectj.lang.reflect.MethodSignature) signature).getParameterTypes()).thenReturn(new Class[0]);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[2]);

        java.lang.Object mockTarget = mock(java.lang.Object.class);
        when(joinPoint.getTarget()).thenReturn(mockTarget);
    }

    /*
     * @Test
     * public void logMethodDataTest() {
     *
     * }
     *
     * @Test
     * public void logMethodsWithEmptyMap() {
     * // Setup
     * Logger mockLogger = mock(Logger.class);
     * LoggingAspect loggingAspect = new LoggingAspect();
     * ReflectionTestUtils.setField(loggingAspect, "logger", mockLogger);
     *
     * // Act
     * loggingAspect.logMethods(null);
     *
     * // Verify
     * Mockito.verify(mockLogger).error(loggingAspect.MAP_IS_NULL_ERROR);
     * }
     *
     * @Test
     * public void logMethodsWithNonNullMap() throws IOException, SparqlQueryFailed
     * {
     * Mockito.doNothing().when(qanaryTripleStoreConnector).update(any());
     * Map<String, MethodObject> testMap = new HashMap<>() {
     * {
     * put("1", new MethodObject(null, null, null, null));
     * put("2", new MethodObject(null, null, null, null));
     * put("3", new MethodObject(null, null, null, null));
     * }
     * };
     * loggingAspectWired.logMethods(testMap);
     * Mockito.verify(loggingAspectWired, Mockito.times(1)).logMethodData(any(),
     * any());
     * Mockito.verify(loggingAspectWired, Mockito.times(3)).logMethods(any());
     * }
     *
     * @Test
     * public void setGraphFromProcessExecutionTest() throws URISyntaxException {
     *
     * // Setup
     * LoggingAspect loggingAspect = new LoggingAspect();
     * QanaryMessage qanaryMessage = new QanaryMessage();
     * qanaryMessage.setValues(TEST_ENDPOINT, TEST_GRAPH, TEST_GRAPH);
     * JoinPoint joinPoint = mock(JoinPoint.class);
     * when(joinPoint.getArgs()).thenReturn(new Object[] { qanaryMessage });
     *
     * // Act
     * loggingAspect.setGraphFromProcessExecution(joinPoint);
     *
     * // Verify
     * assertNotNull(loggingAspect.getCurrentProcessGraph());
     * assertNotNull(loggingAspect.getQanaryTripleStoreConnector());
     * }
     */

    // STACK TESTS

    @Test
    public void emptyStackTest() {
        assertEquals("init",
                this.qanaryAspect.checkAndGetFromStack());
    }

    @Test
    void crossComponentProcessIdTest() {
        this.qanaryAspect.setCrossComponentProcessId(CROSS_COMPONENT_PROCESS_ID_EXAMPLE);
        assertEquals(CROSS_COMPONENT_PROCESS_ID_EXAMPLE,
                this.qanaryAspect.checkAndGetFromStack());
    }

    @Test
    public void nonEmptyStackTest() {
        String TEST_STACK_ITEM = "testStackItem";
        QanaryAspect.getCallStack().push(TEST_STACK_ITEM);
        assertEquals(TEST_STACK_ITEM, this.qanaryAspect.checkAndGetFromStack());
    }

    // implementationStoreMethodExecutionInComponentBefore TESTS

    @Test
    public void implementationStoreMethodExecutionInComponentBeforeTest() {
        this.qanaryAspect.setActiveTracing(true);
        this.qanaryAspect.implementationStoreMethodExecutionInComponentBeforeForComponent(this.joinPoint);
        assertEquals(1, this.qanaryAspect.getMethodList().size());
        assertFalse(QanaryAspect.getCallStack().empty());
    }

    // implementationStoreMethodExecutionInComponentAfter TESTS

    @Test
    public void implementationStoreMethodExecutionInComponentAfterTest() throws Throwable {
        this.qanaryAspect.setActiveTracing(true);
        this.qanaryAspect.implementationStoreMethodExecutionInComponentBeforeForComponent(this.joinPoint);
        assertFalse(QanaryAspect.getCallStack().empty());
        assertEquals(1, this.qanaryAspect.getMethodList().size());
        this.qanaryAspect.implementationStoreMethodExecutionInComponentAfter(this.joinPoint, null);
        assertEquals(0, this.qanaryAspect.getMethodList().size());
    }

    // METHOD-DATA REPRESENTATION TESTS (Jena 5 regression guard)

    /**
     * Method output whose toString contains a double quote (and a SPARQL keyword)
     * must still produce a parseable SPARQL UPDATE. Jena 5 parses updates
     * client-side, so an unescaped quote in the generated rdf:value literal
     * previously raised QueryParseException -> HTTP 500 during question answering.
     */
    @Test
    public void generateOutputRepresentationProducesParseableSparql() {
        Object output = "some result with a \" quote and the VALUES keyword";
        String representation = this.qanaryAspect.generateOutputDataRepresentation(output);
        String update = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
                + "INSERT DATA { GRAPH <urn:g> { <urn:s> <urn:p> " + representation + " . } }";
        // throws QueryParseException (a RuntimeException) if the literal is not escaped
        UpdateFactory.create(update);
    }

    /**
     * Same guard for the input representation (array of arguments).
     */
    @Test
    public void generateInputRepresentationProducesParseableSparql() {
        Object[] input = new Object[] { "arg with a \" quote", "second VALUES arg" };
        String representation = this.qanaryAspect.generateInputDataRepresentation(input);
        String update = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
                + "INSERT DATA { GRAPH <urn:g> { <urn:s> <urn:p> " + representation + " . } }";
        UpdateFactory.create(update);
    }

}
