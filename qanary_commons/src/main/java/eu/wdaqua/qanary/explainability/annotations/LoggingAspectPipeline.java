package eu.wdaqua.qanary.explainability.annotations;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.net.URISyntaxException;

@Aspect
public class LoggingAspectPipeline extends LoggingAspect{

    ////////////////// CONFIG FOR PIPELINE
    /*
     * START: /questionanswering from controller
     * END: RETURN /questionanswering from controller
     * GRAPH: Fetched from
     * QanaryTripleStoreEndpoint: Fetched from
     */

    @Pointcut(
            "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestionThroughJson(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.createQuestionAnswering(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.createQuestionAnsweringFull(..))"
    ) public void startProcessForPipeline() {};

    @Pointcut("execution(* eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.executeComponentList(..))")
    public void setTriplestoreAndGraphForPipeline() {};

    @Before(value = "setTriplestoreAndGraphForPipeline()") // TODO: Can a parent class be established providing those methods for setting QTSC and graph?
    public void setTriplestoreAndGraphForPipeline(JoinPoint joinPoint) throws URISyntaxException {

    }

}
