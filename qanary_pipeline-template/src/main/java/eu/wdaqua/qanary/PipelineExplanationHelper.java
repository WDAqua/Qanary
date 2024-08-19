package eu.wdaqua.qanary;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.explainability.QanaryExplanationData;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for the creation of pipeline-explanations
 * :: In this approach, pipelines are being explained by collecting the sub-components explanations. To realize this step of fetching these explanations this
 * helper class implements several methods to do so.
 */
@Component
@Lazy
public class PipelineExplanationHelper {

    private WebClient webClient;
    @Lazy
    @Autowired
    private QanaryTripleStoreProxy qanaryTripleStoreConnector;
    @Lazy
    @Autowired
    private QanaryComponentRegistrationChangeNotifier qanaryComponentRegistrationChangeNotifier;
    private final String ANNOTATED_BY_COMPONENT_TRIPLE = "?s oa:annotatedBy ?component .";
    @Value("${qanary.components}")
    private List<String> QANARY_COMPONENTS; // The non-prefixed component-names !!!
    private final String GRAPH_QUERY = "/queries/select_all_graphs_with_questionId.rq";
    private final String COMPONENT_QUERY = "/queries/select_all_used_components.rq";
    private Logger logger = LoggerFactory.getLogger(PipelineExplanationHelper.class);
    private String explanationService;

    public QanaryComponentRegistrationChangeNotifier getQanaryComponentRegistrationChangeNotifier() {
        logger.info("{}", this.qanaryComponentRegistrationChangeNotifier.getAvailableComponents());
        return qanaryComponentRegistrationChangeNotifier;
    }

    @Value("${explanation.service}")
    public void setupWebclient(String explanationService){
        this.explanationService = explanationService;
        this.webClient = WebClient.builder().build();
    }

    /**
     * Requests the explanations of the passed components withing the QanaryExplanationData-List
     * @param data List with objects that contain the component, its corresponding URI and the graph
     * @return Stream of Strings (= Explanations)
     */
    public Flux<String> fetchSubComponentExplanations(List<QanaryExplanationData> data) {
        return Flux.fromIterable(data).flatMapSequential(this::getComponentExplanation);
    }

    /**
     * Requests the explanation for a single components embedded in a {@link eu.wdaqua.qanary.explainability.QanaryExplanationData}
     * @param data
     * @return Explanation as Mono
     */
    public Mono<String> getComponentExplanation(QanaryExplanationData data) {
        return webClient.post().uri(data.getServerHost() + "/explain").bodyValue(data).retrieve().bodyToMono(String.class);
    }

    /**
     * Queries for the sub-graph of the PaC by using the questionId and the pre-defined Qanary components (defined in the corresponding application.properties)
     * Note: The questionId is the same for all graphs withing one QA process.
     * Note: The Qanary components are pre-defined for any pipeline-as-component
     * @param questionId QuestionID of the QA process
     * @return graph
     * @throws IOException
     * @throws SparqlQueryFailed
     */
    public String getGraphFromQuestionId(String questionId) throws IOException, SparqlQueryFailed {
        String componentTriples = createComponentTriples();
        String query = QanaryTripleStoreConnector.readFileFromResources(GRAPH_QUERY)
                .replace("?componentAnnotations", componentTriples)
                .replace("?questionId", "<" + questionId + ">");
        // Ignore problem of multiple graphs at a time; annotatedAt could partly solve this
        return qanaryTripleStoreConnector.select(query).next().get("g").toString(); // return first
    }

    /**
     * Creates the triples that represent the used components
     * Note: Each component must have at least one annotation (if no new data was computed, logs were inserted to the graph)
     * @return String representing the used components as triples
     */ // TODO: Different, more robust approach, see Jena Model
    public String createComponentTriples() {
        String triples = "";
        for (String component : QANARY_COMPONENTS) {
            triples += ANNOTATED_BY_COMPONENT_TRIPLE
                    .replace("?s", "?" + component.replace("-",""))
                    .replace("?component", "<urn:qanary:" + component + ">");
        }
        return triples;
    }

    /**
     * Requests the explanation for the pipeline with the prior computed sub-component-explanations
     * @param data Containing the sub-components' explanations
     * @return explanation for pipeline
     */
    public String requestPipelineExplanation(QanaryExplanationData data) {
        return webClient.post().uri(this.explanationService).bodyValue(data).retrieve().bodyToMono(String.class).block();
    }

    /**
     * Requests used components for the case pipeline-as-pipeline
     * Note: In this case, the pipeline don't define its components statically as they are selected by the user
     * @param graph graph
     * @return List of used components
     * @throws IOException
     * @throws URISyntaxException
     * @throws SparqlQueryFailed
     */
    public List<String> getUsedComponents(String graph) throws IOException, URISyntaxException, SparqlQueryFailed {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("graph", ResourceFactory.createResource(graph));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(COMPONENT_QUERY, qsm);
        ResultSet results = this.qanaryTripleStoreConnector.select(query);
        List<String> list = new ArrayList<>();
        while(results.hasNext()) {
            list.add(results.next().get("component").toString().replace("urn:qanary:",""));
        }
        return list;
    }



}
