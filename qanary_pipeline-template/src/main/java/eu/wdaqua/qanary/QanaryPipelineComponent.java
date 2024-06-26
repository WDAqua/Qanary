package eu.wdaqua.qanary;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryQuestionTextual;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@ConditionalOnProperty(name = "pipeline.as.component", havingValue = "true", matchIfMissing = false)
public class QanaryPipelineComponent extends QanaryComponent {

    private final String QUESTION_QUERY = "/select_questionId.rq";
    private final String CONSTRUCT_QUERY = "/pipeline_component_construct.rq";
    private final String INSERT_QUERY = "/insert_constructed_triples.rq";
    private final Logger logger = LoggerFactory.getLogger(QanaryPipelineComponent.class);
    @Autowired
    private QanaryQuestionAnsweringController qanaryQuestionAnsweringController;
    @Autowired
    private QanaryConfigurator qanaryConfigurator;
    @Value("${qanary.components}")
    private List<String> QANARY_COMPONENTS;

    public QanaryPipelineComponent() {
    }

    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

        URI externalEndpoint = this.qanaryConfigurator.getEndpoint();
        URI internalEndpoint = myQanaryMessage.getEndpoint();
        QanaryTripleStoreProxy qanaryTripleStoreConnector = this.qanaryConfigurator.getQanaryTripleStoreConnector();
        qanaryTripleStoreConnector.setInternalConnector(internalEndpoint, this.getApplicationName());
        qanaryTripleStoreConnector.setInternEndpointGraph(myQanaryMessage.getInGraph());

        logger.info("external endpoint (local): {}, internal endpoint: {}", externalEndpoint.toASCIIString(), internalEndpoint.toASCIIString());

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
        updateTriplesFromModel(myQanaryMessage.getInGraph(), constructedTriples, INSERT_QUERY, qanaryTripleStoreConnector);

        return myQanaryMessage;
    }

    private void updateTriplesFromModel(URI graph, Model constructedTriples, String queryPath, QanaryTripleStoreProxy qanaryTripleStoreConnector) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            constructedTriples.write(byteArrayOutputStream, "Turtle");
            String triplesAsString = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
            String query = QanaryTripleStoreConnector.readFileFromResources(queryPath)
                    .replace("?graph", "<" + graph.toASCIIString() + ">")
                    .replace("?triples", triplesAsString);
            qanaryTripleStoreConnector.updateInternal(query);
        } catch (Exception e) {
            logger.warn("Error: {}", e.getMessage());
        }
    }

    public Model constructTriplesFromSubGraph(URI graph, QanaryTripleStoreConnector qanaryTripleStoreConnector) throws IOException {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("newComponent", ResourceFactory.createResource("urn:qanary:" + this.getApplicationName()));
        String sparqlQuery = QanaryTripleStoreConnector.readFileFromResourcesWithMap(CONSTRUCT_QUERY, qsm);
        return qanaryTripleStoreConnector.construct(sparqlQuery, graph);
    }

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


}
