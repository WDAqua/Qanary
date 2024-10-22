package eu.wdaqua.qanary.explainability.logger;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;

public interface ExplainabilityLogger {

    public void logSparqlQuery(String sparqlQuery, String applicationName, QanaryTripleStoreConnector qanaryTripleStoreConnector);

}
