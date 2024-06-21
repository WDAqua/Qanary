package eu.wdaqua.qanary;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryQuestionTextual;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.web.QanaryQuestionAnsweringController;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@ConditionalOnProperty(name = "pipeline.as.component", havingValue = "true", matchIfMissing = false)
public class QanaryPipelineComponent extends QanaryComponent {

    private final String QUESTION_QUERY = "/select_questionId.rq";
    private final String CONSTRUCT_QUERY = "/pipeline_component_construct.rq";
    private final String ANNOTATIONS_CONSTRUCT_QUERY = "/annotations_construct.rq";
    private final String INSERT_QUERY = "/insert_constructed_triples.rq";
    private final String DELETE_QUERY = "/delete_previous_annotations.rq";
    private final Logger logger = LoggerFactory.getLogger(QanaryPipelineComponent.class);
    private final WebClient webClient;
    @Autowired
    private QanaryQuestionAnsweringController qanaryQuestionAnsweringController;
    @Autowired
    private QanaryConfigurator qanaryConfigurator;
    @Value("${qanary.components}")
    private List<String> QANARY_COMPONENTS;
    @Autowired
    private QanaryTripleStoreConnector externalConnector;
    private QanaryTripleStoreConnectorQanaryInternal internalConnector;

    public QanaryPipelineComponent(
    ) {
        webClient = WebClient.builder().build();
    }

    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

        this.getUtils(); // Init QanaryTripleStoreConnectorInternal
        internalConnector = (QanaryTripleStoreConnectorQanaryInternal) this.getUtils().getQanaryTripleStoreConnector();

        // Execute internal pipeline with same question(ID)(URI)(...)
        URI questionId = new URI(getQuestionIdWithQuery(myQanaryMessage.getInGraph().toASCIIString()));
        QanaryQuestion qanaryQuestion = new QanaryQuestionTextual(questionId.toURL(), this.qanaryConfigurator, null);
        URI subGraph = qanaryQuestion.getInGraph();
        QanaryMessage qanaryMessage = new QanaryMessage(myQanaryMessage.getEndpoint(), subGraph);

        // Get annotations and store as model
        Model parentGraphAnnotationsAsModel = externalConnector.construct(
                QanaryTripleStoreConnector.readFileFromResources(ANNOTATIONS_CONSTRUCT_QUERY),
                myQanaryMessage.getInGraph() // get from parent graph
        );

        // STEP 2: Compute new data / execute sub-components
        updateTriplesFromModel(subGraph, parentGraphAnnotationsAsModel, INSERT_QUERY); // Insert annotations to new graph
        this.qanaryQuestionAnsweringController.executeComponentList(qanaryQuestion.getUri(), this.QANARY_COMPONENTS, qanaryMessage); // execute internal pipeline

        // STEP 3: Insert data with CONSTRUCT Query to the parent graph
        updateTriplesFromModel(subGraph, parentGraphAnnotationsAsModel, DELETE_QUERY); // Delete previously inserted triples as not needed anymore
        Model constructedTriples = constructTriplesFromSubGraph(subGraph);
        updateTriplesFromModel(myQanaryMessage.getInGraph(), constructedTriples, INSERT_QUERY); // Insert constructed triples to parent graph

        return myQanaryMessage;
    }

    private void updateTriplesFromModel(URI graph, Model constructedTriples, String queryPath) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            constructedTriples.write(byteArrayOutputStream, "Turtle");
            String triplesAsString = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
            String query = QanaryTripleStoreConnector.readFileFromResources(queryPath)
                    .replace("?graph", "<" + graph.toASCIIString() + ">")
                    .replace("?triples", triplesAsString);
            externalConnector.update(query);
        } catch (Exception e) {
            logger.warn("Error: {}", e.getMessage());
        }
    }

    public Model constructTriplesFromSubGraph(URI graph) throws IOException {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("newComponent", ResourceFactory.createResource("urn:qanary:" + this.getApplicationName()));
        String sparqlQuery = QanaryTripleStoreConnector.readFileFromResourcesWithMap(CONSTRUCT_QUERY, qsm);
        return externalConnector.construct(sparqlQuery, graph);
    }

    private String getQuestionIdWithQuery(String graphUri) throws IOException, SparqlQueryFailed {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("graph", ResourceFactory.createResource(graphUri));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(QUESTION_QUERY, qsm);
        ResultSet result = internalConnector.select(query);
        try {
            return result.next().get("questionId").toString();
        } catch (Exception e) {
            logger.error("QuestionID couldn't be fetched from ResultSet with error: {}", e.getMessage());
            return null;
        }
    }

}
