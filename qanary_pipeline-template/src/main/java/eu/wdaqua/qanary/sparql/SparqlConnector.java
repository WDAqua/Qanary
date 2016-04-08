package eu.wdaqua.qanary.sparql;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * @author Didier Cherix
 *         at 08.04.16.
 */
public interface SparqlConnector {
    public ResultSet select(String query);

    public Model construct(String query);

    public void update(String query);
}
