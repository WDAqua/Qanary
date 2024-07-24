/*
package eu.wdaqua.qanary.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

@Component
public class QanaryExplanation {

    private QanaryTripleStoreConnector qanaryTripleStoreConnector;
    private final String GRAPH_QUERY = "/queries/select_all_graphs_with_questionId.rq";
    private final String ANNOTATED_BY_COMPONENT_TRIPLE = "?s oa:annotatedBy ?component .";

    @Value("${explanation.service.uri}")
    private String EXPLANATION_SERVICE_URI;
    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${qanary.components:#{null}}")
    private ArrayList<String> componentList;
    @Value("${server.host:#{null}}")
    private String serverHost;
    @Value("${server.port:#{null}}")
    private String serverPort;

    private final Logger logger = LoggerFactory.getLogger(QanaryExplanation.class);
    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    public void setQanaryTripleStoreConnector(QanaryTripleStoreProxy qanaryTripleStoreConnector, QanaryTripleStoreConnectorQanaryInternal qanaryTripleStoreConnectorInternal) {
        if(qanaryTripleStoreConnectorInternal == null)
            this.qanaryTripleStoreConnector = qanaryTripleStoreConnector;
        else
            this.qanaryTripleStoreConnector = qanaryTripleStoreConnectorInternal;
    }

    public String explain(QanaryExplanationData qanaryExplanationData) throws URISyntaxException, SparqlQueryFailed, IOException {
        logger.info("Requested explanation for component '{}', graph '{}' and questionURI '{}'", this.applicationName, qanaryExplanationData.getGraph(), qanaryExplanationData.getQuestionId());

        String graph = qanaryExplanationData.getGraph();
        String questionId = qanaryExplanationData.getQuestionId();
        QanaryExplanationData explanationData = qanaryExplanationData; // The questionId remains the same
        if(componentList == null) { // Case if it's a component
            explanationData.setComponent(this.applicationName);
            explanationData.setServerHost(null);
        }
        else { // Case it's not an atomic component
            if(componentList.isEmpty()) { // Case it's the pipeline, the direct child-components can be fetched by the expl. service with the graph
                explanationData.setServerHost(this.serverHost + ":" + this.serverPort);
                explanationData.setComponent(null);
                explanationData.setGraph(graph);
            }
            else { // Case it's a pipeline as component, the graph needs to be selected, then proceed like a pipeline
                String currentGraph = getGraphFromQuestionId(questionId);
                explanationData.setServerHost(this.serverHost + ":" + this.serverPort);
                explanationData.setComponent(null);
                explanationData.setGraph(currentGraph);
                }
        }
        return webClient.post().uri(new URI(EXPLANATION_SERVICE_URI + "/explain")).bodyValue(explanationData).retrieve()
                .onStatus(httpStatus -> httpStatus.is5xxServerError(), error -> Mono.error(new Exception("Server-side error"))) // TODO: Extend
                .bodyToMono(String.class).block();
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

*/