package eu.wdaqua.qanary.explainability.annotations;


import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Aspect
@Service
public class LoggingAspectPipeline {

    ////////////////// CONFIG FOR PIPELINE
    /*
     * START: /questionanswering from controller
     * END: RETURN /questionanswering from controller
     * GRAPH: Fetched from
     * QanaryTripleStoreEndpoint: Fetched from
     */

    private URI processGraph;
    private QanaryTripleStoreConnector qanaryTripleStoreConnector;

    @Pointcut(
            "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(...) || " +
                    "execution(public org.springframework.http.ResponseEntity<eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestionThroughJson(...) || " +
                    "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.createQuestionAnswering(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.createQuestionAnsweringFull(..))"
    ) public void startProcessForPipeline() {};

    @Pointcut("execution(* eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.executeComponentList(..))")
    public void setTriplestoreAndGraphForPipeline() {};

    @Before(value = "setTriplestoreAndGraphForPipeline()") // TODO: Can a parent class be established providing those methods for setting QTSC and graph?
    public void setTriplestoreAndGraphForPipeline(JoinPoint joinPoint) throws URISyntaxException {
        QanaryMessage qanaryMessage = (QanaryMessage) joinPoint.getArgs()[2]; // 3rd parameter is of type QanaryMessage
        this.processGraph = qanaryMessage.getInGraph();
        if(this.qanaryTripleStoreConnector == null) {
            this.qanaryTripleStoreConnector = new QanaryTripleStoreConnectorQanaryInternal(qanaryMessage.getEndpoint(), "null"); // TODO: Application name
        }
    }

}
