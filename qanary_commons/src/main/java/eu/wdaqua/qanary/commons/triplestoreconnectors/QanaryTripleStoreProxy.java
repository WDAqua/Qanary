package eu.wdaqua.qanary.commons.triplestoreconnectors;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * This QanaryTripleStoreConnector acts as a proxy for the QanaryPipeline, mainly for the use-case "pipeline as component".
 * It can handle requests for two endpoints and therefore, it can recursively send requests to higher-level pipelines.
 * In the case of "pipeline as pipeline" the internalConnector will remain unassigned and null.
 */
public class QanaryTripleStoreProxy extends QanaryTripleStoreConnector {

    @Autowired // Injects external connector
    private QanaryTripleStoreConnector externalConnector;

    private QanaryTripleStoreConnectorQanaryInternal internalConnector;
    private URI internEndpointGraph;
    private URI externalEndpointGraph;

    /**
     * Sets the internalConnector if the pipeline acts as component
     *
     * @param endpoint        Endpoint to higher-level (=parent) pipeline
     * @param applicationName This components' application name
     * @throws URISyntaxException
     */
    public void setInternalConnector(URI endpoint, String applicationName) throws URISyntaxException {
        internalConnector = new QanaryTripleStoreConnectorQanaryInternal(endpoint, applicationName);
    }

    /**
     * Used only for "pipeline as component", to store the context
     *
     * @param externalEndpointGraph graph of this pipelines' QA process
     */
    public void setExternalEndpointGraph(URI externalEndpointGraph) {
        this.externalEndpointGraph = externalEndpointGraph;
    }

    /**
     * Used only for "pipeline as component", to store the context
     *
     * @param internEndpointGraph graph of the higher-level pipeline QA process
     */
    public void setInternEndpointGraph(URI internEndpointGraph) {
        this.internEndpointGraph = internEndpointGraph;
    }

    @Override
    public void connect() {

    }

    @Override
    public ResultSet select(String sparql) throws SparqlQueryFailed {
        ResultSet results = externalConnector.select(sparql);
        if (internalConnector != null && !results.hasNext()) {
            getLogger().info("Requesting parent endpoint and graph");
            sparql = externalEndpointGraph == null ? sparql : sparql.replace(externalEndpointGraph.toASCIIString(), internEndpointGraph.toASCIIString());
            results = internalConnector.select(sparql);
        }
        return results;
    }

    @Override
    public boolean ask(String sparql) throws SparqlQueryFailed {
        boolean result = externalConnector.ask(sparql);
        if (internalConnector != null && !result) {
            getLogger().info("Requesting parent endpoint and graph");
            sparql = externalEndpointGraph == null ? sparql : sparql.replace(externalEndpointGraph.toASCIIString(), internEndpointGraph.toASCIIString());
            result = internalConnector.ask(sparql);
        }
        return result;
    }

    @Override
    public void update(String sparql, URI graph) throws SparqlQueryFailed {
        this.update(sparql, null);
    }

    @Override
    public void update(String sparql) throws SparqlQueryFailed {
        externalConnector.update(sparql);
    }

    /**
     * Executes a UPDATE on the internalConnector
     *
     * @param sparql SPARQL query
     * @throws SparqlQueryFailed
     */
    public void updateInternal(String sparql) throws SparqlQueryFailed {
        internalConnector.update(sparql);
    }

    @Override
    public Model construct(String sparql) {
        return externalConnector.construct(sparql);
    }

    @Override
    public Model construct(String sparql, URI graph) {
        return externalConnector.construct(sparql, graph);
    }

    @Override
    public String getFullEndpointDescription() {
        return "";
    }

    public void clearEndpointGraphs() {
        this.externalEndpointGraph = null;
        this.internEndpointGraph = null;
    }
}
