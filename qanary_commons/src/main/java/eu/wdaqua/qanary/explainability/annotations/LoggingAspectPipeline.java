package eu.wdaqua.qanary.explainability.annotations;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;

import java.net.URI;
import java.net.URISyntaxException;

@Aspect
public class LoggingAspectPipeline {

    ////////////////// CONFIG FOR PIPELINE
    /*
     * START: /questionanswering from controller
     * END: RETURN /questionanswering from controller
     * GRAPH: Fetched from
     * QanaryTripleStoreEndpoint: Fetched from
     */

    private String processGraph;
    private QanaryTripleStoreConnector qanaryTripleStoreConnector;
    private final Logger logger = LoggerFactory.getLogger(LoggingAspectPipeline.class);

    public String getProcessGraph() {
        return processGraph;
    }

    public QanaryTripleStoreConnector getQanaryTripleStoreConnector() {
        return qanaryTripleStoreConnector;
    }

    public void setProcessGraph(String processGraph) {
        this.processGraph = processGraph;
    }
    
    public void setQanaryTripleStoreConnector(QanaryTripleStoreConnector qanaryTripleStoreConnector) {
        this.qanaryTripleStoreConnector = qanaryTripleStoreConnector;
    }


    @Pointcut(
            "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestion(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.startquestionansweringwithtextquestionThroughJson(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.createQuestionAnswering(..)) || " +
                    "execution(public org.springframework.http.ResponseEntity<?> eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.createQuestionAnsweringFull(..))"
    ) public void startProcessForPipeline() {};

    @Pointcut("execution(* eu.wdaqua.qanary.web.QanaryQuestionAnsweringController.executeComponentList(..))")
    public void setTriplestoreAndGraphForPipeline() {};

    @Before(value = "setTriplestoreAndGraphForPipeline()")
    public void setTriplestoreAndGraphForPipeline(JoinPoint joinPoint) throws URISyntaxException {
        QanaryMessage qanaryMessage = (QanaryMessage) joinPoint.getArgs()[2];
        this.processGraph = qanaryMessage.getOutGraph().toString();
        if (this.getQanaryTripleStoreConnector() == null) {
            QanaryUtils qanaryUtils = new QanaryUtils(qanaryMessage,
                    new QanaryTripleStoreConnectorQanaryInternal(qanaryMessage.getEndpoint(), null));
            this.setQanaryTripleStoreConnector(qanaryUtils.getQanaryTripleStoreConnector());
        }
        logger.info("Initialized triplestore and graph for pipeline: {}, {}", this.processGraph, this.qanaryTripleStoreConnector.getFullEndpointDescription());
    }


}
