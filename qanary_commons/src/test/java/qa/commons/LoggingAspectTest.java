package qa.commons;

import com.complexible.stardog.plan.filter.functions.rdfterm.Object;
import eu.wdaqua.qanary.explainability.annotations.LoggingAspectComponent;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoggingAspectTest {

    /*
     * private URI TEST_GRAPH = new URI("test-graph");
     * private URI TEST_ENDPOINT = new URI("test-endpoint");
     */

    private LoggingAspectComponent loggingAspectComponent;
    private JoinPoint joinPoint;
    private Signature signature;

    @BeforeEach
    public void setup() {
        this.loggingAspectComponent = new LoggingAspectComponent();
        // this.loggingAspectComponent.setQanaryTripleStoreConnector(mock(QanaryTripleStoreConnectorVirtuoso.class));
        this.loggingAspectComponent.setCallStack(new Stack<>());
        joinPoint = mock(JoinPoint.class);
        signature = mock(Signature.class);
        // Mock the Signature object to return sample values
        when(signature.getName()).thenReturn("sampleMethodName");
        when(signature.toShortString()).thenReturn("sampleMethodSignature");
        // Set up the JoinPoint to return the mocked Signature and CodeSignature
        when(joinPoint.getSignature()).thenReturn(signature);
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
        Assertions.assertEquals(this.loggingAspectComponent.EMPTY_STACK_ITEM,
                this.loggingAspectComponent.checkAndGetFromStack());
    }

    @Test
    public void nonEmptyStackTest() {
        this.loggingAspectComponent.getCallStack().push("test");
        Assertions.assertEquals("test", this.loggingAspectComponent.checkAndGetFromStack());
    }

    // implementationStoreMethodExecutionInComponentBefore TESTS

    @Test
    public void implementationStoreMethodExecutionInComponentBeforeTest() {
        this.loggingAspectComponent.implementationStoreMethodExecutionInComponentBefore(this.joinPoint);

        assertEquals(1, this.loggingAspectComponent.getMethodList().size());
        assertFalse(this.loggingAspectComponent.getCallStack().empty());
    }

    // implementationStoreMethodExecutionInComponentAfter TESTS

    @Test
    public void implementationStoreMethodExecutionInComponentAfterTest() {
        this.loggingAspectComponent.implementationStoreMethodExecutionInComponentBefore(this.joinPoint);
        assertFalse(this.loggingAspectComponent.getCallStack().empty());
        assertEquals(1, this.loggingAspectComponent.getMethodList().size());
        this.loggingAspectComponent.implementationStoreMethodExecutionInComponentAfter(this.joinPoint, mock(Object.class));

        assertEquals(0, this.loggingAspectComponent.getMethodList().size());
    }

}
