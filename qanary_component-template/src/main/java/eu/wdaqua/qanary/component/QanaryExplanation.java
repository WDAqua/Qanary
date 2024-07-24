package eu.wdaqua.qanary.component;

import eu.wdaqua.qanary.business.QanaryConfigurator;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class QanaryExplanation {

    @Autowired
    private QanaryTripleStoreConnector qanaryTripleStoreConnector;
    private final String GRAPH_QUERY = "/queries/select_all_graphs_with_questionId.rq";
    private final String SELECT_ALL_USED_COMPONENTS_QUERY = "/queries/select_all_used_components.rq";

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${pipeline.as.component}")
    private String isPipeline;

    private QanaryConfigurator qanaryConfigurator;
    private URI endpoint;

    @Autowired
    public void setQanaryConfigurator(QanaryConfigurator qanaryConfigurator) {
        this.endpoint = qanaryConfigurator.getEndpoint();
    }

    @ConditionalOnProperty

    // Good caching candidate
    // Explanations aren't stored in the triplestore, instead, the /explain endpoint is used
    public String explain(QanaryExplanationDTO qanaryExplanationDTO) throws URISyntaxException, SparqlQueryFailed, IOException {
        if(qanaryExplanationDTO.getRootGraph() != null && qanaryExplanationDTO.getPriorGraph() == null) {
            List<String> components = getAllUsedComponents(qanaryExplanationDTO.getRootGraph());
            components.forEach(component -> {

            });
        }
        else {
            if(isPipeline == "true") { // Component is PaC oder Root pipeline
                boolean doesEndpointExist = qanaryExplanationDTO.doesEndpointExist(endpoint);
                URI graph = doesEndpointExist ?
                        qanaryExplanationDTO.popAndReturnGraph(endpoint)
                        :
                        qanaryExplanationDTO.setNewEndpointWithGraph(endpoint, this.getAllGraphsFromQuestionId(qanaryExplanationDTO.getQuestionId()));

            }
            else {
                // Is a concrete component, request explanation
            }


        }
    }

    public List<String> getAllUsedComponents(URI graph) throws IOException, URISyntaxException, SparqlQueryFailed {
        List<String> components = new ArrayList<>();
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("graph", ResourceFactory.createResource(graph.toASCIIString()));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_ALL_USED_COMPONENTS_QUERY, qsm);
        ResultSet results = qanaryTripleStoreConnector.select(query);
        while(results.hasNext()) {
            QuerySolution solution = results.nextSolution();
            components.add(solution.get("component").toString());
        }
        return components;
    }

    // TODO: Sort graphs
    public ArrayList<URI> getAllGraphsFromQuestionId(URI questionId) throws URISyntaxException, SparqlQueryFailed {
        ArrayList<URI> graphs = new ArrayList<>();
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("questionId", ResourceFactory.createResource(questionId.toASCIIString()));
        String query = GRAPH_QUERY.replace("?questionId", "<" + questionId + ">");
        ResultSet results = qanaryTripleStoreConnector.select(query);
        while(results.hasNext()) {
            QuerySolution solution = results.nextSolution();
            graphs.add(new URI(solution.get("graph").toString()));
        }
        return graphs;
    }

}


/*
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

 --------



 */