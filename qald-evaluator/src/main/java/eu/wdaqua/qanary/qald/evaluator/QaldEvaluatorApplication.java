package eu.wdaqua.qanary.qald.evaluator;

import eu.wdaqua.qanary.qald.evaluator.metrics.Metrics;
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
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

    private int maxQuestions = 350;

    private void evaluate(String components, int maxQuestionsToBeProcessed) throws UnsupportedEncodingException, IOException {
        Double globalPrecision = 0.0;
        Double globalRecall = 0.0;
        Double globalFMeasure = 0.0;
        int count = 0;

        ArrayList<Integer> fullRecall = new ArrayList<Integer>();
        ArrayList<Integer> fullFMeasure = new ArrayList<Integer>();

        FileReader filereader = new FileReader();

        filereader.getQuestion(1).getUris();

        // send to pipeline
        List<QaldQuestion> questions = new LinkedList<>(filereader.getQuestions());

        for (int i = 0; i < questions.size(); i++) {
            List<String> expectedAnswers = questions.get(i).getResourceUrisAsString();
            logger.info("{}. Question: {}", questions.get(i).getQaldId(), questions.get(i).getQuestion());

            // questions.get(0).setQuestion("How many goals did Pelé score?");

            // Send the question
            RestTemplate restTemplate = new RestTemplate();
            UriComponentsBuilder service = UriComponentsBuilder.fromHttpUrl(this.uriServer);

            MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<String, String>();
            bodyMap.add("question", questions.get(i).getQuestion());
            bodyMap.add("componentlist[]", components);
            String response = restTemplate.postForObject(service.build().encode().toUri(), bodyMap, String.class);
            logger.info("Response pipline: {}", response);

            // Retrieve the computed uris
            JSONObject responseJson = new JSONObject(response);
            String endpoint = responseJson.getString("endpoint");
            String namedGraph = responseJson.getString("graph");
            logger.debug("{}. named graph: {}", questions.get(i).getQaldId(), namedGraph);
            String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
                    + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
                    + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
                    + "SELECT ?uri { " //
                    + "  GRAPH <" + namedGraph + "> { " //
                    + "    ?a a qa:AnnotationOfInstance . " //
                    + "    ?a oa:hasBody ?uri " //
                    + "} }";
            logger.debug("SPARQL: {}", sparql);
            ResultSet r = this.selectTripleStore(sparql, endpoint);
            List<String> systemAnswers = new ArrayList<String>();
            while (r.hasNext()) {
                QuerySolution s = r.next();
                if (s.getResource("uri") != null && !s.getResource("uri").toString().endsWith("null")) {
                    logger.info("System answers: {} ", s.getResource("uri").toString());
                    systemAnswers.add(s.getResource("uri").toString());
                }
            }

            // Retrieve the expected resources from the SPARQL query
            //List<String> expectedAnswers = questions.get(i).getResourceUrisAsString();
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

    private ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
        return qExe.execSelect();
    }

    public void process() throws UnsupportedEncodingException, IOException {

        // TODO:


//        QaldEvaluatorApplication app = new QaldEvaluatorApplication();

        List<String> componentConfigurations = new LinkedList<>();

        List<String> nerComponents = new LinkedList<>();
        List<String> nedComponents = new LinkedList<>();

        // TODO: move to config
        //nerComponents.add("StanfordNER");
        //nerComponents.add("DBpediaSpotlightSpotter");
        //nerComponents.add("FOX");

        // TODO: move to config
        //nedComponents.add("agdistis");
        //nedComponents.add("DBpediaSpotlightNED");

        // monolithic configurations (NER+NED)
        componentConfigurations.add("NED-DBpediaSpotlight");
        //componentConfigurations.add("luceneLinker");

        // create all configurations
        for (String ner : nerComponents) {
            for (String ned : nedComponents) {
                componentConfigurations.add(ner + "," + ned);
            }
        }

        for (String componentConfiguration : componentConfigurations) {
            this.evaluate(componentConfiguration, this.maxQuestions);
        }
    }
}
