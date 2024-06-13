package eu.wdaqua.qanary.commons.triplestoreconnectors;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import virtuoso.jena.driver.*;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * @author AnBo-de
 * <p>
 * the component connects to a Virtuoso triplestore for connecting and
 * executing queries
 * <p>
 * the component is initialized if and only if the required information
 * is available
 * <p>
 * required parameters
 *
 * <pre>
 * <code>
 * virtuoso.url=
 * virtuoso.username=
 * virtuoso.password=
 * </code>
 *         </pre>
 */
@ConditionalOnProperty(name = {"virtuoso.url", "virtuoso.username", "virtuoso.password"}, matchIfMissing = false)
@Component
public class QanaryTripleStoreConnectorVirtuoso extends QanaryTripleStoreConnector {

    private static final CharSequence VIRTUOSO_PROBLEM_STRING = "Read timed out";
    private final String virtuosoUrl;
    private final String username;
    private final String password;
    private final int queryTimeout;
    private final short maxTriesConnectionTimeout = 3;
    private final Logger logger = LoggerFactory.getLogger(QanaryTripleStoreConnectorVirtuoso.class);
    private VirtGraph connection;

    public QanaryTripleStoreConnectorVirtuoso( //
                                               @Value("${virtuoso.url}") String virtuosoUrl, //
                                               @Value("${virtuoso.username}") String username, //
                                               @Value("${virtuoso.password}") String password, //
                                               @Value("${virtuoso.query.timeout:10}") int queryTimeout //
    ) {
        getLogger().info("initialize Virtuoso triplestore connector as {} to {} with timeout {} secs", username, virtuosoUrl, queryTimeout);
        this.virtuosoUrl = virtuosoUrl;
        this.username = username;
        this.password = password;
        this.queryTimeout = queryTimeout;
        this.connect();
    }

    public String getVirtuosoUrl() {
        return this.virtuosoUrl;
    }

    private String getUsername() {
        return this.username;
    }

    private String getPassword() {
        return this.password;
    }

    private int getTimeout() {
        return this.queryTimeout;
    }

    @Override
    public void connect() {
        getLogger().debug("Virtuoso server connecting to {}", this.getVirtuosoUrl());
        assert this.virtuosoUrl != null && !"".equals(this.virtuosoUrl);
        assert this.username != null && !"".equals(this.username);
        assert this.password != null && !"".equals(this.password);
        assert this.getTimeout() > 0;
        this.initConnection();
        assert connection != null;
    }

    private void initConnection() {
        if (this.connection != null) {
            getLogger().warn("Virtuoso server trying to re-connected at {}", this.getVirtuosoUrl());
        } else {
            getLogger().debug("Virtuoso server trying to connected at {}", this.getVirtuosoUrl());
        }

        int numberOfReconnectingTries = 0;
        while (this.maxTriesConnectionTimeout > numberOfReconnectingTries) {
            try {
                connection = new VirtGraph(this.getVirtuosoUrl(), this.getUsername(), this.getPassword());
                connection.setQueryTimeout(getTimeout());
                getLogger().info("Virtuoso server connected at {}", this.getVirtuosoUrl());
                return;
            } catch (Exception e) {
                getLogger().warn("Tried to establish connection ({}), but failed: {}", numberOfReconnectingTries, e.getMessage());
                e.printStackTrace();
                numberOfReconnectingTries++;

                if (this.maxTriesConnectionTimeout <= numberOfReconnectingTries) {
                    getLogger().error("Failed to establish connection. Max tries exceeded!");
                    throw new RuntimeException(e);
                }

                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (Exception e2) {
                    getLogger().warn("Failed to wait for 5 seconds: {}", e2.getMessage());
                    e2.printStackTrace();
                }
            }
        }
    }

    @Override
    public ResultSet select(String sparql) throws SparqlQueryFailed {
        short numberOfTries = 0;
        // try N times if there was a timeout
        while (numberOfTries < this.maxTriesConnectionTimeout || numberOfTries == 0) {
            try {
                return this.select(sparql, numberOfTries);
            } catch (Exception e) {
                getLogger().error("Error while executing a SELECT query: {}", e.getMessage());
                e.printStackTrace();

                if (e.getMessage().contains(VIRTUOSO_PROBLEM_STRING)) { // not nice
                    getLogger().error("Connection was a timeout. Possible retry ({} tries already).", numberOfTries);
                    this.initConnection();
                    numberOfTries++;
                } else {
                    getLogger().error("Error happened. Returns SparqlQueryFailed exception.");
                    throw new SparqlQueryFailed(sparql, this.virtuosoUrl, e);
                }
            }
        }
        return null; // should never happen
    }

    private ResultSet select(String sparql, short numberOfTries) {
        long start = getTime();
        getLogger().info("execute SELECT query (try: {}): {}", numberOfTries, sparql);
        Query query = QueryFactory.create(sparql);
        VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(query, this.connection);
        ResultSetRewindable resultsRewindable = ResultSetFactory.makeRewindable(vqe.execSelect());
        this.logTime(getTime() - start, "SELECT on " + this.getVirtuosoUrl() + " resulted in " + resultsRewindable.size() + " rows: " + sparql);
        return resultsRewindable;
    }

    @Override
    public boolean ask(String sparql) throws SparqlQueryFailed {
        short numberOfTries = 0;
        // try N times if there was a timeout
        while (numberOfTries < this.maxTriesConnectionTimeout || numberOfTries == 0) {
            try {
                return this.ask(sparql, numberOfTries);
            } catch (Exception e) {
                getLogger().error("Error while executing a ASK query: {}", e.getMessage());
                e.printStackTrace();

                if (e.getMessage().contains(VIRTUOSO_PROBLEM_STRING)) { // not nice
                    getLogger().error("Connection was a timeout. Possible retry ({} tries already).", numberOfTries);
                    this.initConnection();
                    numberOfTries++;
                } else {
                    getLogger().error("Error happened. Returns SparqlQueryFailed exception.");
                    throw new SparqlQueryFailed(sparql, this.virtuosoUrl, e);
                }
            }
        }
        return false; // should never happen
    }

    private boolean ask(String sparql, short numberOfTries) throws SparqlQueryFailed {
        long start = getTime();
        getLogger().info("execute ASK query (try: {}): {}", numberOfTries, sparql);
        VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(QueryFactory.create(sparql), this.connection);
        boolean result = vqe.execAsk();
        this.logTime(getTime() - start, "ASK on " + this.getVirtuosoUrl() + ": " + sparql);
        return result;
    }

    @Override
    public void update(String sparql, URI graph) throws SparqlQueryFailed {
        short numberOfTries = 0;
        // try N times if there was a timeout
        while (numberOfTries < this.maxTriesConnectionTimeout || numberOfTries == 0) {
            try {
                this.update(sparql, graph, numberOfTries);
                return;
            } catch (Exception e) {
                getLogger().error("Error while executing a UPDATE query: {}", e.getMessage());
                e.printStackTrace();

                if (e.getMessage().contains(VIRTUOSO_PROBLEM_STRING)) { // not nice
                    getLogger().error("Connection was a timeout. Possible retry ({} tries already).", numberOfTries);
                    this.initConnection();
                    numberOfTries++;
                } else {
                    getLogger().error("Error happened. Returns SparqlQueryFailed exception.");
                    throw new SparqlQueryFailed(sparql, this.virtuosoUrl, e);
                }
            }
        }
    }

    private void update(String sparql, URI graph, short numberOfTries) throws SparqlQueryFailed {
        long start = getTime();
        getLogger().info("execute UPDATE query on graph {} (try: {}): {}", graph, numberOfTries, sparql);
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(sparql, this.connection);
        vur.exec();
        this.logTime(getTime() - start, "UPDATE on " + this.getVirtuosoUrl() + ": " + sparql);
    }

    @Override
    public void update(String sparql) throws SparqlQueryFailed {
        this.update(sparql, null);
    }

    @Override
    public void update(Model model) {
        StmtIterator iterator = model.listStatements();
        iterator.forEach(statement -> {
            Triple triple = statement.asTriple();
            this.connection.performAdd(triple);
        });
    }

    public void update(Triple triple) {
        this.connection.performAdd(triple);
    }

    @Override
    public Model construct(String sparql) {
        return this.construct(sparql, null);
    }

    @Override
    public Model construct(String sparql, URI graph) {
        /*
        VirtGraph constructConnection = new VirtGraph(graph.toASCIIString(), this.virtuosoUrl, this.username, this.password);
        logger.info("Construct query: {} on endpoint: {}", sparql, getFullEndpointDescription());
        try {
            VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, constructConnection);
            return vqe.execConstruct();
        } catch (Exception e) {
            logger.warn("Error: {}", e.getMessage());
            return null;
        }
         */
        // Mapp graph to Jena model
        Model virtModel = VirtModel.openDatabaseModel(graph.toASCIIString(), this.virtuosoUrl, this.username, this.password);
        logger.info("Graph to model size: {}", virtModel.size());
        QueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtModel);
        return vqe.execConstruct();
    }

    @Override
    public String getFullEndpointDescription() {
        return "Virtuoso tiplestore connected on the endpoint " + this.getVirtuosoUrl();
    }

}
