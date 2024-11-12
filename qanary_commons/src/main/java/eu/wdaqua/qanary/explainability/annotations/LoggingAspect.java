package eu.wdaqua.qanary.explainability.annotations;


import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;
import org.aspectj.lang.JoinPoint;

import java.net.URI;
import java.net.URISyntaxException;

public class LoggingAspect {

    private URI processGraph;
    private QanaryTripleStoreConnector qanaryTripleStoreConnector;

    public QanaryTripleStoreConnector getQanaryTripleStoreConnector() {
        return qanaryTripleStoreConnector;
    }

    public void setQanaryTripleStoreConnector(QanaryTripleStoreConnector qanaryTripleStoreConnector) {
        this.qanaryTripleStoreConnector = qanaryTripleStoreConnector;
    }

    public URI getProcessGraph() {
        return processGraph;
    }

    public void setProcessGraph(URI processGraph) {
        this.processGraph = processGraph;
    }

    public QanaryTripleStoreConnectorQanaryInternal createTriplestoreConnectorFromJoinPoint(JoinPoint joinPoint, String applicationName) throws URISyntaxException {
        QanaryMessage qanaryMessage = (QanaryMessage) joinPoint.getArgs()[2];
        return new QanaryTripleStoreConnectorQanaryInternal(qanaryMessage.getEndpoint(), applicationName);
    }

    public URI createGraphFromJoinPoint(JoinPoint joinPoint) {
        QanaryMessage qanaryMessage = (QanaryMessage) joinPoint.getArgs()[2];
        return qanaryMessage.getInGraph();
    }

}
