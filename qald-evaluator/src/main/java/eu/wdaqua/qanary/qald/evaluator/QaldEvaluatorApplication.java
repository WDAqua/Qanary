package eu.wdaqua.qanary.qald.evaluator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import eu.wdaqua.qanary.qald.evaluator.qaldreader.FileReader;
import eu.wdaqua.qanary.qald.evaluator.qaldreader.QaldQuestion;
import eu.wdaqua.qanary.qald.evaluator.qaldreader.QaldQuestionUri;

/**
 * start the spring application
 * 
 * @author AnBo
 *
 */
// @SpringBootApplication
// @EnableAutoConfiguration
// @ComponentScan("eu.wdaqua.qanary.component")
public class QaldEvaluatorApplication {
	private static final Logger logger = LoggerFactory.getLogger(QaldEvaluatorApplication.class);

	public void process() throws UnsupportedEncodingException, IOException {
		Double globalPrecision = 0.0;
		Double globalRecall = 0.0;
		Double globalFMeasure = 0.0;
		int countPrecision = 0; // stores for how many questions the precision
								// can be computed, i.e. do not divide by zero
		int countRecall = 0; // analogusly to countPrecision
		int countFMeasure = 0; // analogusly to countRecall
		int count = 0;

		// SpringApplication.run(QaldEvaluatorApplication.class, args);

		String uriServer = "http://localhost:8080/startquestionansweringwithtextquestion";
		// String components="alchemy";
		// String components="StanfordNER ,agdistis";
		// String components = "luceneLinker";
		// String components="DBpediaSpotlightSpotter ,agdistis";
		// String components = "DBpediaSpotlightSpotter,DBpediaSpotlightNED";
		String components = "DBpediaSpotlightSpotter";

		FileReader filereader = new FileReader();

		filereader.getQuestion(1).getUris();

		// send to pipeline
		List<QaldQuestion> questions = new LinkedList<>(filereader.getQuestions());

		for (int i = 0; i < questions.size(); i++) {

			logger.info("Question {}", questions.get(i).getQuestion());

			// question="Which volcanos in Japan erupted since 2000?";
			// question="In which country does the Ganges start?";
			// question="What is the official website of Tom Cruise?";
			//question="How many goals did Pelé score?";
			//questions.get(0).setQuestion("How many goals did Pelé score?");
			
			System.out.println(URLEncoder.encode(questions.get(1).getQuestion(), "UTF-8"));
			
			// Send the question
			RestTemplate restTemplate = new RestTemplate();
			UriComponentsBuilder service = UriComponentsBuilder.fromHttpUrl(uriServer);

			MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<String, String>();
			bodyMap.add("question", questions.get(i).getQuestion());
			bodyMap.add("componentlist", components);
			// bodyMap.add("submit", "start QA process");
			String response = restTemplate.postForObject(service.build().encode().toUri(), bodyMap, String.class);
			logger.info("Response pipline: {}", response);

			// Retrieve the computed uris
			JSONObject responseJson = new JSONObject(response);
			String endpoint = responseJson.getString("endpoint");
			String namedGraph = responseJson.getString("graph");
			String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "SELECT ?uri { " //
					+ "  GRAPH <" + namedGraph + "> { " //
					+ "    ?a a qa:AnnotationOfInstance . " //
					+ "    ?a oa:hasBody ?uri " //
					+ "} }";
			ResultSet r = selectTripleStore(sparql, endpoint);
			List<String> systemAnswers = new ArrayList<String>();
			while (r.hasNext()) {
				QuerySolution s = r.next();
				logger.info("System answers {} ", s.getResource("uri").toString());
				systemAnswers.add(s.getResource("uri").toString());
			}

			// Retrieve the expected resources from the SPARQL query
			List<QaldQuestionUri> expectedAnswers = questions.get(i).getResourceUris();

			// Compute precision and recall
			int correctRetrieved = 0;
			for (String s : systemAnswers) {
				if (expectedAnswers.contains(s)) {
					correctRetrieved++;
				}
			}
			logger.info("Correctly retrieved: {}", correctRetrieved);

			if (systemAnswers.size() != 0) {
				Double precision = (double) correctRetrieved / systemAnswers.size();
				logger.info("PRECISION: {} ", precision);
				globalPrecision += precision;
				countPrecision++;
			}
			if (expectedAnswers.size() != 0) {
				Double recall = (double) correctRetrieved / expectedAnswers.size();
				logger.info("RECALL: {} ", recall);
				globalRecall += recall;
				countRecall++;
			}
			if (systemAnswers.size() != 0 && expectedAnswers.size() != 0 && correctRetrieved != 0) {
				Double precision = (double) correctRetrieved / systemAnswers.size();
				Double recall = (double) correctRetrieved / expectedAnswers.size();
				Double fMeasure = (2 * precision * recall) / (precision + recall);
				logger.info("PRECISION: {} ", precision);
				logger.info("RECALL: {} ", recall);
				logger.info("F-MEASURE: {} ", fMeasure);
				globalFMeasure += fMeasure;
				countFMeasure++;
			}
		}
		logger.info("Global Precision={}", (double) globalPrecision / countPrecision);
		logger.info("Global Recall={}", (double) globalRecall / countRecall);
		logger.info("Global F-measure={}", (double) globalFMeasure / countFMeasure);

		// retrieve recognized URIs from configured pipeline

		// compare to provided uris

		// String sparql = "SELECT DISTINCT ?uri WHERE {
		// <http://dbpedia.org/resource/Albert_Einstein>
		// <http://dbpedia.org/ontology/doctoralAdvisor> ?uri . }";
		// Query query = QueryFactory.create(sparql);

	}

	public ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
		return qExe.execSelect();
	}

	public static void main(String... args) throws UnsupportedEncodingException, IOException {
		QaldEvaluatorApplication app = new QaldEvaluatorApplication();
		app.process();
	}
}
