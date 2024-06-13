package eu.wdaqua.qanary.commons.triplestoreconnectors;


import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;

@ConditionalOnProperty(name = "pipeline.component", matchIfMissing = false, havingValue = "false")
@Component
public class QanaryTripleStoreConnectorPipelineComponent {

    @Autowired
    private QanaryTripleStoreConnector externalConnector;

    private QanaryTripleStoreConnectorQanaryInternal internalConnector;

    public void setInternalConnector(QanaryTripleStoreConnectorQanaryInternal internalConnector) {
        this.internalConnector = internalConnector;
    }

    public void connect() {

    }


    public ResultSet select(String sparql) throws SparqlQueryFailed {
        return internalConnector.select(sparql);
    }


    public boolean ask(String sparql) throws SparqlQueryFailed {
        return false;
    }


    public void update(String sparql, URI graph) throws SparqlQueryFailed {

    }


    public void update(String sparql) throws SparqlQueryFailed {

    }

    public void update(Model model) {
        this.externalConnector.update(model);
    }


    public Model construct(String sparql, URI graph) {
        return externalConnector.construct(sparql, graph);
    }


    public Model construct(String sparql) {
        return null;
    }


    public String getFullEndpointDescription() {
        return "";
    }
}


/**
 * General setup QanaryTriplestoreConnector
 * select(query), select(query,graph), select(Apache Jena POJO)
 */