package eu.wdaqua.qanary.component;

import com.complexible.stardog.plan.filter.functions.numeric.E;
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
import org.springframework.web.reactive.function.client.WebClient;

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
    private final String ANNOTATED_BY_COMPONENT_TRIPLE = "?s oa:annotatedBy ?component .";

    @Value("${explanation.service.uri}")
    private String EXPLANATION_SERVICE_URI;
    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${component.list}") // TODO: What, if component list not available? If it's a component? -> Is it null, empty or does it throw an error?
    private ArrayList<String> componentList;

    @Autowired
    WebClient webClient;

    public QanaryExplanation() {
        webClient = WebClient.builder().build();
    }

    public String explain(String graph, String questionId) throws URISyntaxException, SparqlQueryFailed, IOException {

        if(componentList == null) { // Case it's a component
            return webClient.get().uri(new URI(EXPLANATION_SERVICE_URI + "/explain/" + graph + "/" + this.applicationName)).retrieve().bodyToMono(String.class).block();
        }
        else { // Case it's not an atomic component
            if(componentList.isEmpty()) { // Case it's the pipeline, the direct child-components can be fetched by the expl. service with the graph
                return webClient.get().uri(new URI(EXPLANATION_SERVICE_URI + "/explain/" + graph)).retrieve().bodyToMono(String.class).block();
            }
            else { // Case it's a pipeline as component, the graph needs to be selected, then proceed like a pipeline
                String currentGraph = getGraphFromQuestionId(questionId);
                return webClient.get().uri(new URI(EXPLANATION_SERVICE_URI + "/explain/" + currentGraph)).retrieve().bodyToMono(String.class).block();
            }
        }
    }

    public String getGraphFromQuestionId(String questionId) throws IOException, URISyntaxException, SparqlQueryFailed {
        String componentTriples = createComponentTriples();
        String query = QanaryTripleStoreConnector.readFileFromResources(GRAPH_QUERY)
                .replace("?componentAnnotations", componentTriples)
                .replace("?questionId", "<" + questionId + ">");
        // Ignore problem of multiple graphs at a time; annotatedAt could partly solve this
        return qanaryTripleStoreConnector.select(query).next().get("g").toString();
    }

    public String createComponentTriples() {
        String triples = "";
        for (String s : componentList) {
            triples += ANNOTATED_BY_COMPONENT_TRIPLE
                    .replace("?s", "?" + s)
                    .replace("?component", "urn:qanary:" + s);
        }
        return triples;
    }

}