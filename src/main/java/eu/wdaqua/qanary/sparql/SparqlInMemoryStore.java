package eu.wdaqua.qanary.sparql;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.hp.hpl.jena.sparql.util.NodeUtils;
import com.hp.hpl.jena.update.*;

/**
 * @author Didier Cherix at 08.04.16.
 */
public class SparqlInMemoryStore implements SparqlConnector {

    private final GraphStore store;

    public SparqlInMemoryStore(final GraphStore store) {
        this.store = store;
    }

    @Override
    public ResultSet select(final String query) {
        final Query parsedQuery = QueryFactory.create(query);
        final QueryExecution qexec = QueryExecutionFactory.create(query, store.toDataset());
        return qexec.execSelect();
    }

    @Override
    public Model construct(final String query) {
        final Query constructQuery = QueryFactory.create(query);
        final QueryExecution qexec = QueryExecutionFactory.create(query, store.toDataset());
        return qexec.execConstruct();
    }

    @Override
    public void update(final String query) {
        try {
            final UpdateRequest update = UpdateFactory.create(query);
            final UpdateProcessor processor = UpdateExecutionFactory.create(update, store);
            processor.execute();
        } catch (final JenaException e) {
            if (e.getMessage().startsWith("No such graph")) {
                final String graph = e.getMessage().replace("No such graph: ", "");
                store.addGraph(NodeUtils.asNode(graph), GraphFactory.createGraphMem());
                update(query);
            }
        }
    }
}
