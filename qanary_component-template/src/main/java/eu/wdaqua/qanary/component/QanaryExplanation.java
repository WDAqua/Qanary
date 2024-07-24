package eu.wdaqua.qanary.component;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class QanaryExplanation {

    @Autowired
    private QanaryTripleStoreConnector qanaryTripleStoreConnector;
    @Value("${application.name}")
    private String applicationName;
    private final String GRAPH_QUERY = "/queries/select_all_graphs_with_questionId.rq";
    private final String SELECT_ALL_USED_COMPONENTS_QUERY = "/queries/select_all_used_components.rq";

    // Good caching candidate
    // Explanations aren't stored in the triplestore, instead, the /explain endpoint is used
    public String explain(String questionId, String graph) throws URISyntaxException, SparqlQueryFailed, IOException {
        // Get the components of the passed graph, that annotated the graph with at least one annotation
        List<String> usedComponents = getAllUsedComponents(graph); // These are the direct child's


        List<String> possibleGraphs = getAllGraphs(questionId);
        if(possibleGraphs.size() == 0)
            return null;
        else if (possibleGraphs.size() == 1) {

        }
        else { // We've to map the graphs to the components
            Map<String,String> graphAndComponent = new HashMap<>();
        }

        return null;
    }


    /**
     * What are the paths?
     * 1. From first pipeline:
     *  1.1 returns it direct childs -> Okay
     * 2. From a PaC:
     *  2.1 returns its child's -> Needs the correct graph -> We're calling the /explain endpoint with the correct graph
     *  2.2 We don't need to pass the endpoint as we
     */
    public List<String> getAllUsedComponents(String graph) throws IOException, URISyntaxException, SparqlQueryFailed {
        List<String> components = new ArrayList<>();
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("graph", ResourceFactory.createResource(graph));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_ALL_USED_COMPONENTS_QUERY, qsm);
        ResultSet results = qanaryTripleStoreConnector.select(query);
        while(results.hasNext()) {
            QuerySolution solution = results.nextSolution();
            components.add(solution.get("component").toString());
        }
        return components;
    }

    public List<String> getAllGraphs(String questionId) throws URISyntaxException, SparqlQueryFailed {
        List<String> graphs = new ArrayList<>();
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("questionId", ResourceFactory.createResource(questionId));
        String query = GRAPH_QUERY.replace("?questionId", "<" + questionId + ">");
        ResultSet results = qanaryTripleStoreConnector.select(query);
        while(results.hasNext()) {
            QuerySolution solution = results.nextSolution();
            graphs.add(solution.get("graph").toString());
        }
        return graphs;
    }

}
