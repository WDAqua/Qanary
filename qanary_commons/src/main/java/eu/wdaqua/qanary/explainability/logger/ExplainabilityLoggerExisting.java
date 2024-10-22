package eu.wdaqua.qanary.explainability.logger;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Conditional class to log different types of data
 * Created if activeLogging is missing as property or set to "true"
 */
@ConditionalOnProperty(name = "activeLogging", havingValue = "true", matchIfMissing = true)
@Service
public class ExplainabilityLoggerExisting implements ExplainabilityLogger {

    private Logger logger = LoggerFactory.getLogger(ExplainabilityLoggerExisting.class);

    public void logSparqlQuery(String sparqlQuery, String applicationName, QanaryTripleStoreConnector qanaryTripleStoreConnector) {
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        Query query = QueryFactory.create(sparqlQuery);
        querySolutionMap.add("graph", ResourceFactory.createResource(query.getGraphURIs().get(0)));
        querySolutionMap.add("component", ResourceFactory.createResource(applicationName));
        querySolutionMap.add("questionID", ResourceFactory.createResource("urn:qanary:currentQuestion"));
        querySolutionMap.add("body", ResourceFactory.createPlainLiteral(sparqlQuery));
        try {
            qanaryTripleStoreConnector.update(QanaryTripleStoreConnector.readFileFromResourcesWithMap("/queries/insert_explanation_data_sparql_query.rq", querySolutionMap));
        } catch (Exception e) {
            logger.error("Logging failed, {}", e.getMessage());
        }
    }

}
