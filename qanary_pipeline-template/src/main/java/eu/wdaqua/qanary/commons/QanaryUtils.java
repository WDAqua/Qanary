package eu.wdaqua.qanary.commons;

import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;

import java.net.URI;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import eu.wdaqua.qanary.component.config.QanaryConfiguration;
//import eu.wdaqua.qanary.commons.config.ontology.TextPositionSelector;

/**
 * the class is a covering standard tasks users of the Qanary methodology might have
 *
 * if you are missing helper methods than please request them via {@see <a
 * href="https://github.com/WDAqua/Qanary/issues/new">Github</a>}
 *
 * @author AnBo
 */
public class QanaryUtils {

    private static final Logger logger = LoggerFactory.getLogger(QanaryUtils.class);

    private final URI endpoint;
    private final URI inGraph;
    private final URI outGraph;

    public QanaryUtils(QanaryMessage qanaryMessage) {
        this.endpoint = qanaryMessage.getEndpoint();
        this.inGraph = qanaryMessage.getInGraph();
        this.outGraph = qanaryMessage.getOutGraph();
    }

    public QanaryUtils(QanaryQuestionAnsweringRun qanaryRun) {
        this.endpoint = qanaryRun.getEndpoint();
        this.inGraph = qanaryRun.getInGraph();
        this.outGraph = qanaryRun.getOutGraph();
    }

    /**
     * returns the endpoint provided by the QanaryMessage object provided via constructor
     */
    public URI getEndpoint() {
        return this.endpoint;
    }

    /**
     * returns the inGraph provided by the QanaryMessage object provided via constructor
     */
    public URI getInGraph() {
        return this.inGraph;
    }

    /**
     * returns the outGraph provided by the QanaryMessage object provided via constructor
     */
    public URI getOutGraph() {
        return this.outGraph;
    }

    /**
     * wrapper for selectTripleStore
     */
    public ResultSet selectFromTripleStore(String sparqlQuery) {
        return this.selectFromTripleStore(sparqlQuery, this.getEndpoint().toString());
    }

    /**
     *  query a SPARQL endpoint with a given SELECT query
     */
    public ResultSet selectFromTripleStore(String sparqlQuery, String endpoint) {
        logger.debug("selectTripleStore on {} execute {}", endpoint, sparqlQuery);
        long start = getTime();
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
        ResultSet resultset = qExe.execSelect();
        this.logTime(getTime() - start, "selectFromTripleStore: " + sparqlQuery);
        return resultset;
    }

    /**
     *  query a SPARQL endpoint with a given ASK query
     */
    public boolean askTripleStore(String sparqlQuery, String endpoint) {
        logger.debug("askTripleStore on {} execute {}", endpoint, sparqlQuery);
        long start = getTime();
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
        boolean b = qExe.execAsk();
        this.logTime(getTime() - start, "askFromTripleStore: " + sparqlQuery);
        return b;
    }

    /**
     * insert data into triplestore, endpoint is taken from QanaryMessage
     */
    public void updateTripleStore(String sparqlQuery) {
        this.updateTripleStore(sparqlQuery, this.getEndpoint().toString());
    }

    /**
     * insert data into triplestore
     */
    public void updateTripleStore(String sparqlQuery, String endpoint) {
        long start = getTime();
        UpdateRequest request = UpdateFactory.create(sparqlQuery);
        UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);
        proc.execute();
        this.logTime(getTime() - start, "updateTripleStore: " + sparqlQuery);
    }

    /**
     * executes a SPARQL INSERT into the triplestore
     *
     * TODO: needs to be extracted
     *
     * @return map
     */
    public static void loadTripleStore(final String sparqlQuery, final URI endpoint) {
        final UpdateRequest request = UpdateFactory.create(sparqlQuery);
        final UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint.toString());
        proc.execute();
    }

    /**
     * wrapper for retrieving the URI where the service is currently running
     */
    public String getComponentUri() {
        return QanaryConfiguration.getServiceUri().toString();
    }

    /**
     * wrapper for retrieving the URI where the service is currently running
     */
    public String getHostUri() {
        return QanaryConfiguration.getHostUri().toString();
    }


    /**
     * get current time in milliseconds
     */
    public static long getTime() {
        return System.currentTimeMillis();
    }

    /**
     *
     * @param description
     * @param duration
     */
    private void logTime(long duration, String description) {
        logger.debug("runtime measurement: {} ms for {}", duration, description);
    }

}
