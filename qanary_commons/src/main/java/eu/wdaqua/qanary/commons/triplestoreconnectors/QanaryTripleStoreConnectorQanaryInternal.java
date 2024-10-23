package eu.wdaqua.qanary.commons.triplestoreconnectors;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.explainability.logger.ExplainabilityLogger;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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
    
    @Inject
    private ExplainabilityLogger explainabilityLogger;

    public QanaryTripleStoreConnectorQanaryInternal(URI endpoint, String applicationName) throws URISyntaxException {
        this.setApplicationName(applicationName);
        this.endpoint = endpoint;
        this.connect();
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
        explainabilityLogger.logSparqlQuery(sparql, this.applicationName, this);
        ResultSet myResultSet = queryExecution.execSelect();
        ResultSetRewindable resultSet = ResultSetFactory.makeRewindable(myResultSet);
        return resultSet;
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
    public Model construct(String sparql) {
        return null;
    }

    @Override
    public Model construct(String sparql, URI graph) {
        return null;
    }

    @Override
    public boolean ask(String sparql) throws SparqlQueryFailed {
        explainabilityLogger.logSparqlQuery(sparql, this.applicationName, this);
        return this.connection.queryAsk(sparql);
    }

    @Override
    public String getFullEndpointDescription() {
        return "simple open endpoint without authorization: " + this.getEndpoint().toASCIIString();
    }

}
