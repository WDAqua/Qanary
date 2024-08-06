package eu.wdaqua.qanary.explainability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.awt.image.RescaleOp;

@Controller
public class QanaryExplanationController {

    private QanaryExplanation qanaryExplanation;
    private Logger logger = LoggerFactory.getLogger(QanaryExplanationController.class);

    @Autowired
    public void setQanaryExplanationService(QanaryExplanation qanaryExplanation) {
        this.qanaryExplanation = qanaryExplanation;
    }

    @GetMapping("/explain")
    public ResponseEntity<String> explain(@RequestBody QanaryExplanationData qanaryExplanationData) {
        try {
            return new ResponseEntity<>(this.qanaryExplanation.explain(qanaryExplanationData), HttpStatus.OK);
        } catch(NullPointerException e) {
            String error = "No component exists that implements QanaryExplanation.";
            logger.error("{}", error);
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch(Exception e) {
            return null; // TODO!
        }
    }

}
