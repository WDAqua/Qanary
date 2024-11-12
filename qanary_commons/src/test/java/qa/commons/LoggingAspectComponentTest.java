package qa.commons;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorVirtuoso;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.explainability.annotations.LoggingAspect;
import eu.wdaqua.qanary.explainability.annotations.MethodObject;
import org.aspectj.lang.JoinPoint;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-testing Aspect; not testing the Pointcuts
 */
@ExtendWith(MockitoExtension.class)
public class LoggingAspectTest {

    private URI TEST_GRAPH = new URI("test-graph");
    private URI TEST_ENDPOINT = new URI("test-endpoint");

    @Mock
    private QanaryTripleStoreConnectorVirtuoso qanaryTripleStoreConnector;

    @InjectMocks
    private LoggingAspect loggingAspectWired;

    public LoggingAspectTest() throws URISyntaxException {
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this); // Initializes the mocks
    }

    /**
     * Testing query equality
     */
    @Test
    public void logMethodDataTest() {

    }

    /**
     * Tests, if null map don't throw exception as it should be handled
     */
    @Test
    public void logMethodsWithEmptyMap() {
        // Setup
        Logger mockLogger = mock(Logger.class);
        LoggingAspect loggingAspect = new LoggingAspect();
        ReflectionTestUtils.setField(loggingAspect, "logger", mockLogger);

        // Act
        loggingAspect.logMethods(null);

        // Verify
        Mockito.verify(mockLogger).error(loggingAspect.MAP_IS_NULL_ERROR);
    }

    @Test
    public void logMethodsWithNonNullMap() throws IOException, SparqlQueryFailed {
        Mockito.doNothing().when(qanaryTripleStoreConnector).update(any());
        Map<String, MethodObject> testMap = new HashMap<>() {{
            put("1", new MethodObject(null,null,null,null));
            put("2", new MethodObject(null,null,null,null));
            put("3", new MethodObject(null,null,null,null));
        }};
        loggingAspectWired.logMethods(testMap);
        Mockito.verify(loggingAspectWired, Mockito.times(1)).logMethodData(any(),any());
        Mockito.verify(loggingAspectWired, Mockito.times(3)).logMethods(any());
    }

    @Test
    public void setGraphFromProcessExecutionTest() throws URISyntaxException {

        // Setup
        LoggingAspect loggingAspect = new LoggingAspect();
        QanaryMessage qanaryMessage = new QanaryMessage();
        qanaryMessage.setValues(TEST_ENDPOINT,TEST_GRAPH,TEST_GRAPH);
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{qanaryMessage});

        // Act
        loggingAspect.setGraphFromProcessExecution(joinPoint);

        // Verify
        assertNotNull(loggingAspect.getCurrentProcessGraph());
        assertNotNull(loggingAspect.getQanaryTripleStoreConnector());
    }




}
