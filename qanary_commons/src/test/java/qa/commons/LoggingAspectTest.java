package qa.commons;

import com.complexible.stardog.plan.filter.functions.rdfterm.Object;
import eu.wdaqua.qanary.explainability.aspects.QanaryAspect;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Stack;

import static org.junit.Assert.assertFalse;
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
        // this.loggingAspectComponent.setQanaryTripleStoreConnector(mock(QanaryTripleStoreConnectorVirtuoso.class));
        QanaryAspect.setCallStack(new Stack<>());
        joinPoint = mock(JoinPoint.class);
        signature = mock(Signature.class);
        // Mock the Signature object to return sample values
        when(signature.getName()).thenReturn("sampleMethodName");
        when(signature.toShortString()).thenReturn("sampleMethodSignature");
        // Set up the JoinPoint to return the mocked Signature and CodeSignature
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[2]);
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
    public void implementationStoreMethodExecutionInComponentAfterTest() {
        this.qanaryAspect.setActiveTracing(true);
        this.qanaryAspect.implementationStoreMethodExecutionInComponentBeforeForComponent(this.joinPoint);
        assertFalse(QanaryAspect.getCallStack().empty());
        assertEquals(1, this.qanaryAspect.getMethodList().size());
        this.qanaryAspect.implementationStoreMethodExecutionInComponentAfter(this.joinPoint, null);
        assertEquals(0, this.qanaryAspect.getMethodList().size());
    }

}
