package eu.wdaqua.qanary.explainability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tests the {@code /explain} endpoint of {@link QanaryExplanationController}:
 * it returns the explanation of the configured {@link QanaryExplanation}
 * service, and a 400 response when the service fails.
 */
class QanaryExplanationControllerTest {

    @Test
    void returnsTheExplanationOfTheService() {
        QanaryExplanationController controller = new QanaryExplanationController();
        controller.setQanaryExplanationService(
                data -> "The component annotated " + data.getQuestionId() + " in " + data.getGraph());

        ResponseEntity<String> response = controller.explain(
                new QanaryExplanationData("urn:graph:1", "urn:qanary:question:1", "http://localhost:8080"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("The component annotated urn:qanary:question:1 in urn:graph:1", response.getBody());
    }

    @Test
    void answersBadRequestWhenTheServiceFails() {
        QanaryExplanationController controller = new QanaryExplanationController();
        controller.setQanaryExplanationService(data -> {
            throw new IOException("explanation template missing");
        });

        ResponseEntity<String> response = controller.explain(new QanaryExplanationData());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Couldn't explain requested component.", response.getBody());
    }
}
