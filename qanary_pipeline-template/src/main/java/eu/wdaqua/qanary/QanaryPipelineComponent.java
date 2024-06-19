package eu.wdaqua.qanary;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorPipelineComponent;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;
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

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Component
@ConditionalOnProperty(name = "pipeline.component", havingValue = "false")
public class QanaryPipelineComponent extends QanaryComponent {

    private final String QUESTION_QUERY = "/select_questionId.rq";
    private final String CONSTRUCT_QUERY = "/pipeline_component_construct.rq";
    private final Logger logger = LoggerFactory.getLogger(QanaryPipelineComponent.class);
    private final WebClient webClient;
    @Autowired
    private QanaryQuestionAnsweringController qanaryQuestionAnsweringController;
    @Value("${qanary.components}")
    private List<String> QANARY_COMPONENTS;
    @Autowired
    private QanaryTripleStoreConnectorPipelineComponent qanaryTripleStoreConnector;


    public QanaryPipelineComponent(
    ) {
        webClient = WebClient.builder().baseUrl("http://localhost:8890").build();
    }

    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

        /*
         * STEP 1: Set up internal TripleStoreConnector and fetch plain question
         */
        this.getUtils();
        this.qanaryTripleStoreConnector.setInternalConnector((QanaryTripleStoreConnectorQanaryInternal) this.getUtils().getQanaryTripleStoreConnector());

        String question = getQuestionWithQuery(myQanaryMessage.getInGraph().toASCIIString());

        /*
         * STEP 2: Execute internal Qanary pipeline
         */
        QanaryQuestionAnsweringRun qaRun = qanaryQuestionAnsweringController.createOrUpdateAndRunQuestionAnsweringSystemHelper(
                null, question, null, this.QANARY_COMPONENTS, "", null, null, null);
        URI subGraph = qaRun.getInGraph();

        /*
         * Construct new triples and insert them to the triplestore, so that the components' created annotations appear as created by this component
         */
        // STEP 3: Insert data with CONSTRUCT Query to the parent graph
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource(subGraph.toASCIIString()));
        transferGraphData(myQanaryMessage.getInGraph(), subGraph);

        return myQanaryMessage;
    }

    private void transferGraphData(URI targetGraph, URI graphUri) throws IOException { // targetGraph really used/necessary?
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("newComponent", ResourceFactory.createResource(this.getApplicationName()));
        String sparqlQuery = QanaryTripleStoreConnector.readFileFromResourcesWithMap(CONSTRUCT_QUERY, qsm);
        try {
            Model model = qanaryTripleStoreConnector.construct(sparqlQuery, graphUri); // create model with constructed triples
            qanaryTripleStoreConnector.update(model); // insert triples to triplestore // TODO: CHECK the impact if the targetGraph isn't specified
        } catch (Exception e) {
            logger.warn("Error: {}", e.getMessage());
        }
    }

    private String getQuestionWithQuery(String graphUri) throws IOException, SparqlQueryFailed {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("graph", ResourceFactory.createResource(graphUri));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(QUESTION_QUERY, qsm);
        ResultSet result = qanaryTripleStoreConnector.select(query);
        try {
            String question = result.next().get("questionId").toString();
            return getQuestionWithQuestionId(question);
        } catch (Exception e) {
            logger.error("QuestionID couldn't be fetched from ResultSet with error: {}", e.getMessage());
            return null;
        }
    }

    protected String getQuestionWithQuestionId(String questionId) {
        logger.info("Question ID: {}", questionId);
        return webClient.get().uri(questionId + "/raw").retrieve().bodyToMono(String.class).block();
    }
}