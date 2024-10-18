package qa.commons;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.explainability.annotations.LoggingAspect;
import eu.wdaqua.qanary.explainability.annotations.MethodObject;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

/**
 * Unit-testing Aspect; not testing the Pointcuts
 */
@ExtendWith(MockitoExtension.class)
public class LoggingAspectTest {

    @InjectMocks
    private LoggingAspect loggingAspect;

    /**
     * Testing query equality
     */
    @Test
    public void logMethodDataTest() {

    }

    @Test
    public void logMethodsWithEmptyMap() {
        String error = Assertions.assertThrows(NullPointerException.class, () -> {
            loggingAspect.logMethods(null);
        }).getMessage();
        Assertions.assertEquals(loggingAspect.MAP_IS_NULL_ERROR, error);
    }

    @Test
    public void logMethodsWithNonNullMap() throws IOException, SparqlQueryFailed {
        Mockito.when(loggingAspect.logMethodData(any(),any())).then(); // TODO: then what
        Map<String, MethodObject> testMap = new HashMap<>();
        loggingAspect.logMethods(testMap);
        // TODO: Verify method call x times
    }




}
