package eu.wdaqua.qanary.commons.triplestoreconnectors;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * simple connection to open endpoint without authorization using the Apache Jena library
 *
 * @author AnBo
 */
public class QanaryTripleStoreConnectorQanaryInternal extends QanaryTripleStoreConnector {
    private static final Logger logger = LoggerFactory.getLogger(QanaryTripleStoreConnectorQanaryInternal.class);
    private URI endpoint;
    private RDFConnection connection;
    private String applicationName;

    public QanaryTripleStoreConnectorQanaryInternal(URI endpoint, String applicationName) throws URISyntaxException {
        this.setApplicationName(applicationName);
        this.endpoint = endpoint;
        this.connect();
    }

    public String getApplicationName() {
        return applicationName;
    }

    private void setApplicationName(String applicationName) {
        this.applicationName = "urn:qanary:" + applicationName;
    }

    private URI getEndpoint() {
        return this.endpoint;
    }

    @Override
    public void connect() {
        org.apache.jena.query.ARQ.init();  // maybe it helps to prevent problems?
        this.connection = RDFConnection.connect(this.getEndpoint().toASCIIString());
    }

    @Override
    public ResultSet select(String sparql) throws SparqlQueryFailed {
        QueryExecution queryExecution = this.connection.query(sparql);
        logData(sparql);
        ResultSet myResultSet = queryExecution.execSelect();
        ResultSetRewindable resultSet = ResultSetFactory.makeRewindable(myResultSet);
        return resultSet;
    }

    private void logData(String sparql) {
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        Query query = QueryFactory.create(sparql);
        querySolutionMap.add("graph", ResourceFactory.createResource(query.getGraphURIs().get(0)));
        querySolutionMap.add("component", ResourceFactory.createResource(this.applicationName));
        querySolutionMap.add("questionID", ResourceFactory.createResource("urn:qanary:currentQuestion"));
        querySolutionMap.add("body", ResourceFactory.createPlainLiteral(sparql));
        try {
            this.update(QanaryTripleStoreConnector.readFileFromResourcesWithMap("/queries/insert_explanation_data_sparql_query.rq", querySolutionMap));
        } catch (Exception e) {
            getLogger().error("Logging failed, {}", e);
        }

    }

    @Override
    public void update(String sparql, URI graph) throws SparqlQueryFailed {
        this.update(sparql);
    }

    @Override
    public void update(String sparql) throws SparqlQueryFailed {
        logger.debug("UPDATE on {}: {}", this.getEndpoint().toASCIIString(), sparql);
        this.connection.update(sparql);
    }

    @Override
    public boolean ask(String sparql) throws SparqlQueryFailed {
        logData(sparql);
        return this.connection.queryAsk(sparql);
    }

    @Override
    public String getFullEndpointDescription() {
        return "simple open endpoint without authorization: " + this.getEndpoint().toASCIIString();
    }
}
