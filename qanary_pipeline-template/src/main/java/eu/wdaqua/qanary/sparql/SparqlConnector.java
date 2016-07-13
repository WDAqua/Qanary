package eu.wdaqua.qanary.sparql;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author Didier Cherix at 08.04.16.
 */
public interface SparqlConnector {
    ResultSet select(String query);

    Model construct(String query);

    void update(String query);
}
