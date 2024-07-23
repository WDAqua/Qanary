package eu.wdaqua.qanary.component;

import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class QanaryExplanationController {

    private static final Logger logger = LoggerFactory.getLogger(QanaryExplanationController.class);

    @Autowired
    QanaryExplanation qanaryExplanationService;

    @GetMapping(value = {"/explain/{graphUri}"})
    @Operation(
            summary = "Explanation endpoint for Qanary component.", //
            operationId = "explainComponent", //
            description = "Returns an explanation for this component within the specified graph." //
    )
    public ResponseEntity<?> explainComponent(@RequestBody QanaryQuestionAnsweringRun qanaryQuestionAnsweringRun) { // To be done
        return new ResponseEntity<>("Not yet implemented", HttpStatus.BAD_REQUEST);
    }

}
