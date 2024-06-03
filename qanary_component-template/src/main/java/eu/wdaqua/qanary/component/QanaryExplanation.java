package eu.wdaqua.qanary.component;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;

import java.io.IOException;
import java.util.List;

public class QanaryExplanation {

    private static final String SELECT_ALL_COMPONENTS_QUERY = "/queries/select_all_used_components.rq";

    // Good caching candidate
    // Explanations aren't stored in the triplestore, instead, the /explain endpoint is used
    public void explain(String graphURI) {
        ///// What to take into account?

        // explanation of each child
        List<String> usedComponents;

        // Question - only pipeline or *real* components too?
            // ...

        // Java <-> RDF
            // ...
    }

    public void selectAllComponents(String graphURI) throws IOException {
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.add("graph", ResourceFactory.createResource(graphURI));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_ALL_COMPONENTS_QUERY,querySolutionMap);
        // return components, select query
    }

}
