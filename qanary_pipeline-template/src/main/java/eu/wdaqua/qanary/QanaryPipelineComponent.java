package eu.wdaqua.qanary;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryQuestionTextual;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryExplanationData;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.web.QanaryQuestionAnsweringController;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
// Only created when the pipeline should act as a component, otherwise not
@ConditionalOnProperty(name = "pipeline.as.component", havingValue = "true")
public class QanaryPipelineComponent extends QanaryComponent {

    private final String QUESTION_QUERY = "/select_questionId.rq";
    private final String CONSTRUCT_QUERY = "/pipeline_component_construct.rq";
    private final String INSERT_QUERY = "/insert_constructed_triples.rq";
    private final String GRAPH_QUERY = "/queries/select_all_graphs_with_questionId.rq";
    private final String ANNOTATED_BY_COMPONENT_TRIPLE = "?s oa:annotatedBy ?component .";
    private final Logger logger = LoggerFactory.getLogger(QanaryPipelineComponent.class);
    private final String EXPLAIN_ENDPOINT = "/explain";
    private WebClient webClient = WebClient.builder().build();
    private QanaryTripleStoreProxy qanaryTripleStoreConnector;
    @Autowired
    private QanaryQuestionAnsweringController qanaryQuestionAnsweringController;
    private QanaryConfigurator qanaryConfigurator;
    @Value("${qanary.components}")
    private List<String> QANARY_COMPONENTS; // The non-prefixed component-names !!!
    @Autowired
    private QanaryComponentRegistrationChangeNotifier qanaryComponentRegistrationChangeNotifier;
    @Value("${explanation.service}")
    private String explanationService;

    public QanaryPipelineComponent() {
    }

    @Autowired
    public void setupTripleStoreConnector(QanaryConfigurator qanaryConfigurator) {
        this.qanaryConfigurator = qanaryConfigurator;
        this.qanaryTripleStoreConnector = qanaryConfigurator.getQanaryTripleStoreConnector();
    }

    /**
     * Implementation of the QanaryComponent 'process()' method. Executes its own pipeline with pre-defined components and writes the results
     * to the parent triplestore as annotated of this component.
     *
     * @param myQanaryMessage QanaryMessage passed from the "parent" pipeline
     * @throws Exception
     */
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

        // Get required information
        URI externalEndpoint = this.qanaryConfigurator.getEndpoint();
        URI internalEndpoint = myQanaryMessage.getEndpoint();
        this.qanaryTripleStoreConnector.setInternalConnector(internalEndpoint, this.getApplicationName());
        this.qanaryTripleStoreConnector.setInternEndpointGraph(myQanaryMessage.getInGraph());

        // Get question and execute internal pipeline with it
        URI questionId = new URI(getQuestionIdWithQuery(myQanaryMessage.getInGraph().toASCIIString(), qanaryTripleStoreConnector));

        // Set up QanaryQuestion and QanaryMessage for pipeline execution
        QanaryQuestion<?> qanaryQuestion = new QanaryQuestionTextual(questionId.toURL(), this.qanaryConfigurator, null);
        URI subGraph = qanaryQuestion.getInGraph(); // "local" graph
        QanaryMessage qanaryMessage = new QanaryMessage(externalEndpoint, subGraph);
        qanaryTripleStoreConnector.setExternalEndpointGraph(subGraph);

        // Execute internal pipeline
        this.qanaryQuestionAnsweringController.executeComponentList(qanaryQuestion.getUri(), QANARY_COMPONENTS, qanaryMessage);

        // Write internal annotations to parent endpoint and graph
        Model constructedTriples = constructTriplesFromSubGraph(subGraph, qanaryTripleStoreConnector);
        String insertQuery = constructQueryFromModel(constructedTriples, myQanaryMessage.getInGraph(), INSERT_QUERY);
        qanaryTripleStoreConnector.updateInternal(insertQuery);

        // Clear endpoint graphs specified earlier
        qanaryTripleStoreConnector.clearEndpointGraphs();

        return myQanaryMessage;
    }

    /**
     * Creates a query for the passed query from path. Utilizes a model and converts it to triple-statements
     * Uses String replacement rather than variable replacement (!)
     *
     * @param constructedTriples Model with Statements
     * @param graph              Target graph
     * @param pathToQuery        Path to query, should consist of "?graph" as well as "?triples"
     * @return executable query
     */
    public String constructQueryFromModel(Model constructedTriples, URI graph, String pathToQuery) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            constructedTriples.write(byteArrayOutputStream, "Turtle");
            String triplesAsString = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
            return QanaryTripleStoreConnector.readFileFromResources(pathToQuery)
                    .replace("?graph", "<" + graph.toASCIIString() + ">")
                    .replace("?triples", triplesAsString);
        } catch (Exception e) {
            logger.warn("Error while converting Model to triples and inserting it to the passed query: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Executes a construct query to the graph @param graph with the passed QanaryTripleStoreConnector
     *
     * @param graph                      Graph on which the CONSTRUCT is executed
     * @param qanaryTripleStoreConnector
     * @return Model
     * @throws IOException
     */
    public Model constructTriplesFromSubGraph(URI graph, QanaryTripleStoreConnector qanaryTripleStoreConnector) throws IOException {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("newComponent", ResourceFactory.createResource("urn:qanary:" + this.getApplicationName()));
        String sparqlQuery = QanaryTripleStoreConnector.readFileFromResourcesWithMap(CONSTRUCT_QUERY, qsm);
        return qanaryTripleStoreConnector.construct(sparqlQuery, graph);
    }

    /**
     * Fetches the questionID from the parent.
     *
     * @param graphUri                   The parent's graph
     * @param qanaryTripleStoreConnector
     * @return QuestionID as String
     * @throws IOException
     * @throws URISyntaxException
     * @throws SparqlQueryFailed
     */
    private String getQuestionIdWithQuery(String graphUri, QanaryTripleStoreConnector qanaryTripleStoreConnector) throws IOException, URISyntaxException, SparqlQueryFailed {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("graph", ResourceFactory.createResource(graphUri));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(QUESTION_QUERY, qsm);
        ResultSet result = qanaryTripleStoreConnector.select(query);
        try {
            return result.next().get("questionId").toString();
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            return null;
        }
    }

    @Override
    public String explain(QanaryExplanationData data) throws IOException, URISyntaxException, SparqlQueryFailed {
        String pacGraph = getGraphFromQuestionId(data.getQuestionId());
        List<QanaryExplanationData> dataList = new ArrayList<>();
        Map<String, Instance> componentsAndInstances = qanaryComponentRegistrationChangeNotifier.getAvailableComponents();
        for (String qanaryComponent : QANARY_COMPONENTS) {
            dataList.add(new QanaryExplanationData(
                            pacGraph,
                            data.getQuestionId(),
                            qanaryComponent,
                            qanaryComponentRegistrationChangeNotifier.getAvailableComponents().get(qanaryComponent).getRegistration().getServiceUrl()
            ));
        }
        List<String> subComponentExplanations = fetchSubComponentExplanations(dataList).collectList().block();
        // TODO: Send to explanationService to compose them and return it; TODO: Add status handling
        // return webClient.post().uri(this.explanationService + EXPLAIN_ENDPOINT).bodyValue(null/*TODO:*/).retrieve().bodyToMono(String.class).block();
        return StringUtils.join(subComponentExplanations).toString();
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
        return qanaryTripleStoreConnector.select(query).next().get("g").toString(); // return first,
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


}
