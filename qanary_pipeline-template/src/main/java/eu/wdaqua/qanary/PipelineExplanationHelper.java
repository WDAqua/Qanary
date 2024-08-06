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

    public void setExplanationService(String service) {
        this.explanationService = service;
    }

    public Flux<String> fetchSubComponentExplanations(List<QanaryExplanationData> data) {
        return Flux.fromIterable(data).flatMapSequential(this::getComponentExplanation);
    }

    public Mono<String> getComponentExplanation(QanaryExplanationData data) {
        return webClient.post().uri(data.getServerHost() + "/explain").bodyValue(data).retrieve().bodyToMono(String.class);
    }

    public String getGraphFromQuestionId(String questionId) throws IOException, URISyntaxException, SparqlQueryFailed {
        String componentTriples = createComponentTriples();
        String query = QanaryTripleStoreConnector.readFileFromResources(GRAPH_QUERY)
                .replace("?componentAnnotations", componentTriples)
                .replace("?questionId", "<" + questionId + ">");
        // Ignore problem of multiple graphs at a time; annotatedAt could partly solve this
        return qanaryTripleStoreConnector.select(query).next().get("g").toString(); // return first
    }

    public String createComponentTriples() {
        String triples = "";
        for (String component : QANARY_COMPONENTS) {
            triples += ANNOTATED_BY_COMPONENT_TRIPLE
                    .replace("?s", "?" + component.replace("-",""))
                    .replace("?component", "<urn:qanary:" + component + ">");
        }
        return triples;
    }

    public String requestPipelineExplanation(QanaryExplanationData data) {
        return webClient.post().uri(this.explanationService).bodyValue(data).retrieve().bodyToMono(String.class).block();
    }

    public List<String> getUsedComponents(String graph) throws IOException, URISyntaxException, SparqlQueryFailed {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("graph", ResourceFactory.createResource(graph));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(COMPONENT_QUERY, qsm);
        ResultSet results = this.qanaryTripleStoreConnector.select(query);
        List<String> list = new ArrayList<>();
        while(results.hasNext()) {
            list.add(results.next().get("component").toString());
        }
        return list;
    }



}
