package eu.wdaqua.qanary.web;

import eu.wdaqua.qanary.message.QanaryComponentNotAvailableException;
import eu.wdaqua.qanary.message.QanaryExceptionQuestionNotProvided;
import eu.wdaqua.qanary.message.QanaryExceptionServiceCallNotOk;
import eu.wdaqua.qanary.message.QanaryQuestionCreated;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * controller for processing questions, i.e., related to the question answering process
 *
 * @author AnBo
 */
@Controller
public class QanaryFeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(QanaryFeedbackController.class);

    //Set this to allow browser requests from other websites
    @ModelAttribute
    public void setVaryResponseHeader(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
    }

    /**
     * recives a message and stores it locally
     */
    @RequestMapping(value = "/feedback", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> startquestionansweringwithtextquestion(
        @RequestParam(value = "question", required = true) final String question,
        @RequestParam(value = "sparql") final String sparql,
        @RequestParam(value = "correct") final Boolean correct,
	@RequestParam(value = "language", required = false, defaultValue ="en") final String language,
	@RequestParam(value = "knowledgebase",required = false, defaultValue = "dbpedia") final String knowledgebase)
        throws URISyntaxException, QanaryComponentNotAvailableException, QanaryExceptionServiceCallNotOk,
        IOException, QanaryExceptionQuestionNotProvided {
        JSONObject obj = new JSONObject();
        obj.put("question", question);
        obj.put("sparql", sparql);
        obj.put("correct", correct);
	obj.put("language", language);
	obj.put("knowledgebase", knowledgebase);
        logger.info("feedback: {}", obj.toString());
        return new ResponseEntity<QanaryQuestionCreated>(HttpStatus.OK);
    }
}

