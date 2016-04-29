package eu.wdaqua.qanary.qald.evaluator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import eu.wdaqua.qanary.qald.evaluator.qaldreader.FileReader;
import eu.wdaqua.qanary.qald.evaluator.qaldreader.QaldQuestion;
import eu.wdaqua.qanary.qald.evaluator.qaldreader.TurtleResultWriter;

/**
 * start the spring application
 * 
 * @author AnBo
 *
 */
@SpringBootApplication
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class QaldEvaluatorApplication {
	private static final Logger logger = LoggerFactory.getLogger(QaldEvaluatorApplication.class);

	String uriServer = "http://localhost:8080/startquestionansweringwithtextquestion";

	public void process(String components, int maxQuestionsToBeProcessed)
			throws UnsupportedEncodingException, IOException {
		Double globalPrecision = 0.0;
		Double globalRecall = 0.0;
		Double globalFMeasure = 0.0;
		//int countPrecision = 0; // stores for how many questions the precision
								// can be computed, i.e. do not divide by zero
		//int countRecall = 0; // analogusly to countPrecision
		//int countFMeasure = 0; // analogusly to countRecall
		int count = 0;

		ArrayList<Integer> fullRecall = new ArrayList<Integer>();
		ArrayList<Integer> fullFMeasure = new ArrayList<Integer>();
		
		// SpringApplication.run(QaldEvaluatorApplication.class, args);

		TurtleResultWriter writer = new TurtleResultWriter("/tmp/results.ttl");
<<<<<<< HEAD
		String uriServer = "http://localhost:8080/startquestionansweringwithtextquestion";
		//String components="alchemy";
		//String components="StanfordNER ,agdistis";
		//String components="StanfordNER ,DBpediaSpotlightNED";
		//String components="FOX ,agdistis";
		//String components="FOX ,DBpediaSpotlightNED";
		//String components = "luceneLinker";
		//String components="DBpediaSpotlightSpotter ,agdistis";
		String components = "DBpediaSpotlightSpotter,DBpediaSpotlightNED";
		// String components = "DBpediaSpotlightSpotter";
=======
>>>>>>> 856da2e0bf40574ad820532c86a575a223da4ff7

		FileReader filereader = new FileReader();

		filereader.getQuestion(1).getUris();

		// send to pipeline
		List<QaldQuestion> questions = new LinkedList<>(filereader.getQuestions());

		for (int i = 0; i < questions.size(); i++) {

			logger.info("{}. Question: {}", questions.get(i).getQaldId(), questions.get(i).getQuestion());
			writer.writeQaldQuestionInformation(questions.get(i));

			// question="Which volcanos in Japan erupted since 2000?";
			// question="In which country does the Ganges start?";
			// question="What is the official website of Tom Cruise?";
			// question="How many goals did Pelé score?";
			// questions.get(0).setQuestion("How many goals did Pelé score?");

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
			ResultSet r = selectTripleStore(sparql, endpoint);
			List<String> systemAnswers = new ArrayList<String>();
			while (r.hasNext()) {
				QuerySolution s = r.next();
				if (s.getResource("uri") != null && !s.getResource("uri").toString().endsWith("null")) {
					logger.info("System answers: {} ", s.getResource("uri").toString());
					systemAnswers.add(s.getResource("uri").toString());
					writer.writeEntityInQuestion(questions.get(i).getQaldId(), s.getResource("uri").getURI(),
							"recognized");
				}
			}

			// Retrieve the expected resources from the SPARQL query
			List<String> expectedAnswers = questions.get(i).getResourceUrisAsString();
			for (String expected : expectedAnswers) {
				writer.writeEntityInQuestion(questions.get(i).getQaldId(), expected, "required");
			}

			// Compute precision and recall
			Metrics m = new Metrics();
			m.compute(expectedAnswers, systemAnswers);
			globalPrecision += m.precision;
			globalRecall += m.recall;
			globalFMeasure += m.fMeasure;
			count++;
			
			if (m.recall==1){
				fullRecall.add(1);
			} else {
				fullRecall.add(0);
			}
			
			if (m.fMeasure==1){
				fullFMeasure.add(1);
			} else {
				fullFMeasure.add(0);
			}
			
		}
		logger.info("Global Precision={}", (double) globalPrecision / count);
		logger.info("Global Recall={}", (double) globalRecall / count);
		logger.info("Global F-measure={}", (double) globalFMeasure / count);

		System.out.println("FullRecall");
		for (int i: fullRecall){
			System.out.println(i);
		}
		
		System.out.println("FullFMeasure");
		for (int i: fullFMeasure){
			System.out.println(i);
		}
		
		writer.close();
		// retrieve recognized URIs from configured pipeline

		// compare to provided uris

		// String sparql = "SELECT DISTINCT ?uri WHERE {
		// <http://dbpedia.org/resource/Albert_Einstein>
		// <http://dbpedia.org/ontology/doctoralAdvisor> ?uri . }";
		// Query query = QueryFactory.create(sparql);

	}

	class Metrics{
        private Double precision=0.0;
        private Double recall=0.0;
        private Double fMeasure=0.0;


        public void compute(List<String> expectedAnswers, List<String> systemAnswers){
                //Compute the number of retrieved answers
                int correctRetrieved = 0;
                for (String s:systemAnswers){
                        if (expectedAnswers.contains(s)){
                                correctRetrieved++;
                        }
                }
                //Compute precision and recall following the evaluation metrics of QALD
                if (expectedAnswers.size()==0){
                        if (systemAnswers.size()==0){
                                recall=1.0;
                                precision=1.0;
                                fMeasure=1.0;
                        } else {
                                recall=0.0;
                                precision=0.0;
                                fMeasure=0.0;
                        }
                } else {
                        if (systemAnswers.size()==0){
                                recall=0.0;
                                precision=1.0;
                        } else {
                                precision = (double)correctRetrieved/systemAnswers.size();
                                recall = (double)correctRetrieved/expectedAnswers.size();
                        }
                        if (precision==0 && recall==0){
                                fMeasure=0.0;
                        } else {
                                fMeasure = (2*precision*recall)/(precision+recall);
                        }
                }
        }
	}
	
	public ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
		return qExe.execSelect();
	}

	public static void main(String... args) throws UnsupportedEncodingException, IOException {

		// TODO:
		int maxQuestions = 350;

		QaldEvaluatorApplication app = new QaldEvaluatorApplication();

		List<String> componentConfigurations = new LinkedList<>();

		List<String> nerComponents = new LinkedList<>();
		List<String> nedComponents = new LinkedList<>();

		// TODO: move to config
		nerComponents.add("StanfordNER");
		nerComponents.add("DBpediaSpotlightSpotter");
		nerComponents.add("FOX");

		// TODO: move to config
		nedComponents.add("agdistis");
		nedComponents.add("DBpediaSpotlightNED");

		// monolithic configurations (NER+NED)
		componentConfigurations.add("alchemy");
		componentConfigurations.add("luceneLinker");

		// create all configurations
		for (String ner : nerComponents) {
			for (String ned : nedComponents) {
				componentConfigurations.add(ner + "," + ned);
			}
		}

		for (String componentConfiguration : componentConfigurations) {
			app.process(componentConfiguration, maxQuestions);
		}
	}
}
