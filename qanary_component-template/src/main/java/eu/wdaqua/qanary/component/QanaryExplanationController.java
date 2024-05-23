package eu.wdaqua.qanary.component;

import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class QanaryExplanationController {

    private static final Logger logger = LoggerFactory.getLogger(QanaryExplanationController.class);

    @GetMapping(value = {"/explain/{graphUri}"})
    @Operation(
            summary = "Explanation endpoint for Qanary component.", //
            operationId = "explainComponent", //
            description = "Returns an explanation for this component within the specified graph." //
    )
    public ResponseEntity<?> explainComponent(@RequestParam String graphUri) { // To be done
        return new ResponseEntity<>("Not yet implemented", HttpStatus.BAD_REQUEST);
    }

}
