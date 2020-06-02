package eu.wdaqua.qanary.qald.evaluator;

import eu.wdaqua.qanary.qald.evaluator.evaluation.Metrics;
import eu.wdaqua.qanary.qald.evaluator.qaldreader.FileReader;
import eu.wdaqua.qanary.qald.evaluator.qaldreader.QaldQuestion;
import org.apache.jena.query.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * start the spring application
 *
 * @author AnBo
 */

@Component
public class QaldEvaluatorApplication {
    private static final Logger logger = LoggerFactory.getLogger(QaldEvaluatorApplication.class);

    @Value(value = "${server.uri}")
    private String uriServer;

    @Value(value = "${qanary.triplestore.stardog5}")
    private boolean stadog5;

    @Value(value = "${qanary.component.ner}")
    private List<String> nerComponents;

    @Value(value = "${qanary.component.ned}")
    private List<String> nedComponents;

    @Value(value = "${qanary.component.configuration}")
    private List<String> componentConfigurations;


    private void evaluate(String components) throws IOException {
        Double globalPrecision = 0.0;
        Double globalRecall = 0.0;
        Double globalFMeasure = 0.0;
        int count = 0;

        ArrayList<Integer> fullRecall = new ArrayList<Integer>();
        ArrayList<Integer> fullFMeasure = new ArrayList<Integer>();

        FileReader filereader = new FileReader();

        // send to pipeline
        for (QaldQuestion question : filereader.getQuestions()) {
            List<String> expectedAnswers = question.getResourceUrisAsString();
            logger.info("{}. Question: {}", question.getQaldId(), question.getQuestion());

            // question.setQuestion("How many goals did Pelé score?");

            // Send the question
            RestTemplate restTemplate = new RestTemplate();
            UriComponentsBuilder service = UriComponentsBuilder.fromHttpUrl(this.uriServer);

            MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<String, String>();
            bodyMap.add("question", question.getQuestion());
            bodyMap.add("componentlist[]", components);
            String response = restTemplate.postForObject(service.build().encode().toUri(), bodyMap, String.class);
            logger.info("Response pipline: {}", response);

            // Retrieve the computed uris
            JSONObject responseJson = new JSONObject(response);
            String endpoint = responseJson.getString("endpoint");
            String namedGraph = responseJson.getString("outGraph");
            logger.debug("{}. named graph: {}", question.getQaldId(), namedGraph);
            ResultSet r = this.selectTripleStore(namedGraph, endpoint);

            //Process answers
            List<String> systemAnswers = new ArrayList<String>();
            while (r.hasNext()) {
                QuerySolution s = r.next();
                if (s.getResource("uri") != null && !s.getResource("uri").toString().endsWith("null")) {
                    logger.info("System answers: {} ", s.getResource("uri").toString());
                    systemAnswers.add(s.getResource("uri").toString());
                }
            }

            // Retrieve the expected resources from the SPARQL query
            for (String expected : expectedAnswers) {
                logger.info("Expected answers: {} ", expected);
            }

            // Compute precision and recall
            Metrics m = new Metrics();
            m.compute(expectedAnswers, systemAnswers);
            globalPrecision += m.getPrecision();
            globalRecall += m.getRecall();
            globalFMeasure += m.getfMeasure();
            count++;

            if (m.getRecall() == 1) {
                fullRecall.add(1);
            } else {
                fullRecall.add(0);
            }
        }
        logger.info("Global Precision={}", globalPrecision / count);
        logger.info("Global Recall={}", globalRecall / count);
        logger.info("Global F-measure={}", globalFMeasure / count);
    }

    private ResultSet selectTripleStore(String namedGraph, String endpoint) {
        String sparqlQuery = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
                + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
                + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
                + "SELECT ?uri { " //
                + "  GRAPH <" + namedGraph + "> { " //
                + "    ?a a qa:AnnotationOfInstance . " //
                + "    ?a oa:hasBody ?uri " //
                + "} }";
        logger.debug("SPARQL: {}", sparqlQuery);
        Query query = QueryFactory.create(sparqlQuery);

        if (this.stadog5) endpoint += "/query";
        QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
        return qExe.execSelect();
    }

    public void process() throws IOException {

        // create all NER+NED configurations
        for (String ner : this.nerComponents) {
            for (String ned : this.nedComponents) {
                this.componentConfigurations.add(ner + "," + ned);
            }
        }

        for (String componentConfiguration : this.componentConfigurations) {
            logger.info("Component configuration: {}", componentConfiguration);
            this.evaluate(componentConfiguration);
        }
    }
}
