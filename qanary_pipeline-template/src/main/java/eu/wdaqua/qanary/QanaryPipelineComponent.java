package eu.wdaqua.qanary;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;
import eu.wdaqua.qanary.web.QanaryQuestionAnsweringController;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "pipeline.component", havingValue = "false")
public class QanaryPipelineComponent extends QanaryComponent {

    private final String QUESTION_QUERY = "/select_questionId.rq";
    private final Logger logger = LoggerFactory.getLogger(QanaryPipelineComponent.class);
    @Autowired
    private QanaryQuestionAnsweringController qanaryQuestionAnsweringController;
    @Value("${qanary.components}")
    private List<String> QANARY_COMPONENTS;
    private WebClient webClient;
    private RDFConnection connection;

    public QanaryPipelineComponent(
            //    @Value("${}") String sparqlEndpoint,
            //    @Value("${}") String sparqlEndpointUsername,
            //    @Value("${}") String sparqlEndpointPassword
    ) {
        webClient = WebClient.builder().baseUrl("http://localhost:8890").build();
        connection = RDFConnection.connectPW("http://localhost:8890/sparql", "dba", "dba");
    }

    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

        // This method is only executed when the Qanary pipeline component is called as component
        // It follows the standard of all process() implementations

        // 1. Get all information === Question
        // 2. Compute new information === Execute the own pipeline
        // 3. Store information === Response JSON

        String question = getQuestionWithQuery(myQanaryMessage.getInGraph().toASCIIString());

        // STEP 2:
        /*
         * Calls its own components
         * How and where are they defined? Are they somehow passed or determined?
         * (Call for registered components possible -> API working?)
         * ==> Extend /annotatequestion with componentListParam?
         * ==> Component should serve one explicit function -> Pre-defined components
         * Returns @see{QanaryQuestionAnsweringRun.class}
         */

        QanaryQuestionAnsweringRun qaRun = qanaryQuestionAnsweringController.createOrUpdateAndRunQuestionAnsweringSystemHelper(
                null, question, null, QANARY_COMPONENTS, "", null, null, null);

        URI subGraph = qaRun.getInGraph();

        // STEP 3: Insert data with CONSTRUCT Query to the parent graph
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource(subGraph.toASCIIString()));
        transferGraphData(myQanaryMessage.getInGraph(), subGraph);

        return null;
    }

    private void createSparqlInsertQueryFromModel(String graph, Model model) throws URISyntaxException {
        List<String> triples = new ArrayList<>();
        model.listStatements().forEach(statement -> {
            triples.add(statement.asTriple().getSubject().toString() + " " + statement.asTriple().getPredicate().toString() + " " + statement.asTriple().getObject().toString() + " .");
        });
        logger.info("Triples: {}", triples);
    }

    private void transferGraphData(URI targetGraph, URI graphUri) throws IOException {
        String sparqlQuery = QanaryTripleStoreConnector.readFileFromResources("/pipeline_component_construct.rq")
                .replace("?graph", "<" + graphUri.toASCIIString() + ">")
                .replace("?newcomponent", "<urn:qanary:" + this.getApplicationName() + ">");
        logger.info("Query Cons: {}", sparqlQuery);
        /*
        String queryEncoded = URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8.toString());
        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/sparql")
                        .queryParam("query", queryEncoded)
                        .queryParam("format", "application/rdf+xml")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        Model model = ModelFactory.createDefaultModel();
        model.read(response, "RDF/XML");
        logger.info("Response turtle: {}", model.toString());
        */
        try {
            Model model = connection.queryConstruct(sparqlQuery);
            // TODO: COPY TO TARGET_GRAPH
        } catch (Exception e) {
            logger.warn("Error: {}", e.getMessage());
            e.printStackTrace();
        }
    }


    private String getQuestionWithQuery(String graphUri) throws IOException {
        String query = QanaryTripleStoreConnector.readFileFromResources(QUESTION_QUERY).replace("?graph", "<" + graphUri + ">");
        logger.info("Query: {}", query);
        QueryExecution qe = connection.query(query);
        ResultSet result = qe.execSelect();
        String question = result.next().get("questionId").toString();
        return getQuestionWithQuestionId(question);
    }

    private String getQuestionWithQuestionId(String questionId) {
        logger.info("Question ID: {}", questionId);
        return webClient.get().uri(questionId + "/raw").retrieve().bodyToMono(String.class).block();
    }
}
