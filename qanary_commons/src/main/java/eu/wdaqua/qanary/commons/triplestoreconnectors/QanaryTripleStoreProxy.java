package eu.wdaqua.qanary.commons.triplestoreconnectors;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.URISyntaxException;

public class QanaryTripleStoreProxy extends QanaryTripleStoreConnector {

    @Autowired
    private QanaryTripleStoreConnector externalConnector; // Injects external connector

    private QanaryTripleStoreConnectorQanaryInternal internalConnector; // set if necessary
    @Value("#{new Boolean('${pipeline.as.component}')}")
    private boolean isPipelineComponent;
    private URI internEndpointGraph;
    private URI externalEndpointGraph;

    public void setInternalConnector(URI endpoint, String applicationName) throws URISyntaxException {
        internalConnector = new QanaryTripleStoreConnectorQanaryInternal(endpoint, applicationName);
    }

    public void setExternalEndpointGraph(URI externalEndpointGraph) {
        this.externalEndpointGraph = externalEndpointGraph;
    }

    public void setInternEndpointGraph(URI internEndpointGraph) {
        this.internEndpointGraph = internEndpointGraph;
    }

    @Override
    public void connect() {

    }

    @Override
    public ResultSet select(String sparql) throws SparqlQueryFailed, URISyntaxException {
        ResultSet results = externalConnector.select(sparql);
        if (isPipelineComponent && !results.hasNext()) {
            getLogger().info("Requesting parent endpoint and graph");
            sparql = externalEndpointGraph == null ? sparql : sparql.replace(externalEndpointGraph.toASCIIString(), internEndpointGraph.toASCIIString());
            results = internalConnector.select(sparql);
        }
        return results;
    }

    @Override
    public boolean ask(String sparql) throws SparqlQueryFailed {
        return false;
    }

    @Override
    public void update(String sparql, URI graph) throws SparqlQueryFailed {
        this.update(sparql, null);
    }

    @Override
    public void update(String sparql) throws SparqlQueryFailed {
        externalConnector.update(sparql);
    }

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
