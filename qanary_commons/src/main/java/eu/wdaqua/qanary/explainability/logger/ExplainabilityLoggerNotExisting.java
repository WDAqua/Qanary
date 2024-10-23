package eu.wdaqua.qanary.explainability.logger;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(name = "activeLogging", havingValue = "false", matchIfMissing = false)
@Service
public class ExplainabilityLoggerNotExisting implements ExplainabilityLogger {

    public ExplainabilityLoggerNotExisting() {
        logger.debug("ExplainabilityLoggerNotExisting created");
    }

    private Logger logger = LoggerFactory.getLogger(ExplainabilityLoggerNotExisting.class);

    @Override
    public void logSparqlQuery(String sparqlQuery, String applicationName, QanaryTripleStoreConnector qanaryTripleStoreConnector) {
        logger.error("Logging is not activated. Logging for SPARQL query skipped");
    }
}
