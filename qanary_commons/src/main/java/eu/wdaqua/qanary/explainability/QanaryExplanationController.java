package eu.wdaqua.qanary.explainability;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * This controller class handles HTTP POST requests for providing explanations for Qanary components.
 * By default, it uses a {@link QanaryExplanation} service to generate explanations based on the provided data.
 * In case of a specific explanation that should be provided by a component, it should overwrite this class by a specific implementation.
 */
@Controller
public class QanaryExplanationController {

    private QanaryExplanation qanaryExplanation;
    private Logger logger = LoggerFactory.getLogger(QanaryExplanationController.class);

    @Autowired
    public void setQanaryExplanationService(QanaryExplanation qanaryExplanation) {
        this.qanaryExplanation = qanaryExplanation;
    }

    @PostMapping("/explain")
    @Operation(
            summary = "Endpoint to request the explanation for this component.",
            description = "Pass the QA processes' graph and questionId as JSON body like described. Other properties can be ignored."
    )
    public ResponseEntity<String> explain(@RequestBody QanaryExplanationData qanaryExplanationData) {
        try {
            return new ResponseEntity<>(this.qanaryExplanation.explain(qanaryExplanationData), HttpStatus.OK);
        } catch(Exception e) {
            logger.error("Error with message: {}", e.getMessage());
            return new ResponseEntity<>("Couldn't explain requested component.", HttpStatus.BAD_REQUEST);
        }
    }

}
