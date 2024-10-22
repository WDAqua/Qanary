package eu.wdaqua.qanary.explainability.logger;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(name = "activeLogging", havingValue = "false", matchIfMissing = false)
public class ExplainabilityLoggerNotExisting implements ExplainabilityLogger {

    private Logger logger = LoggerFactory.getLogger(ExplainabilityLoggerNotExisting.class);

    @Override
    public void logSparqlQuery(String sparqlQuery, String applicationName, QanaryTripleStoreConnector qanaryTripleStoreConnector) {
        logger.error("Logging is not activated. Logging for SPARQL query skipped");
    }
}
