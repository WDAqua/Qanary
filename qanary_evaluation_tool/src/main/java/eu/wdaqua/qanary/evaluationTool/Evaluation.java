package eu.wdaqua.qanary.evaluationTool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


/**
 * evaluation tool for the EL problem over QALD
 * 
 * @author Dennis Diefenbach & Kuldeep Singh
 *
 */

public class Evaluation {
	private static final Logger logger = LoggerFactory.getLogger(Evaluation.class);
	
	public void qald6_test() {
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		String uriServer="http://localhost:8080/startquestionansweringwithtextquestion";
		//String components="alchemy";
		//String components="StanfordNER ,agdistis";
		//String components="StanfordNER ,DBpediaSpotlightNED";
		//String components="luceneLinker";
		String components="DBpediaSpotlightSpotter ,agdistis";
		//String components="DBpediaSpotlightSpotter ,DBpediaSpotlightNED";
		//String components="FOX ,agdistis";
		//String components="FOX ,DBpediaSpotlightNED";
		
		long startTime = System.currentTimeMillis();
		
		try {
			//Considers all questions
			Double globalPrecision=0.0;
			Double globalRecall=0.0;
			Double globalFMeasure=0.0;
			ArrayList<Integer> rightQuestions = new ArrayList<Integer>();
			int count=0;
			int numerCorrectQuestion=0;
			int numerRecallOne=0;
			String path = Evaluation.class.getResource("/qald-benchmark/qald6-train-questions.json").getPath();
			File file = new File(path);
			String content=FileUtils.readFileToString(file);
			JSONObject json = new JSONObject(content);
			JSONArray tests = json.getJSONArray("questions");
			for (int i = 0; i<tests.length(); i++){
				JSONObject questionObject = tests.getJSONObject(i);
				JSONArray questions = questionObject.getJSONArray("question");
				String question=questions.getJSONObject(0).get("string").toString();
				
				logger.info("Question "+question);
				
				question="Who developed Minecraft?";
				//question="In which country does the Ganges start?";
				//question="What is the official website of Tom Cruise?";
				
				//Send the question to startquestionansweringwithtextquestion
				RestTemplate restTemplate = new RestTemplate();
				UriComponentsBuilder service = UriComponentsBuilder.fromHttpUrl(uriServer);
				
				MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<String, String>();
				bodyMap.add("question", question);
				bodyMap.add("componentlist", components);
				bodyMap.add("submit", "start QA process");
				String response = restTemplate.postForObject(service.build().encode().toUri(), bodyMap, String.class);
				logger.info("Response pipline {}"+response);
				
				//Retrieve the computed uris
				JSONObject responseJson = new JSONObject(response);
				String endpoint = responseJson.getString("endpoint");
				String namedGraph = responseJson.getString("graph");
				String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
						+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
						+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " 
						+ "SELECT ?uri { " + "GRAPH <" + namedGraph + "> { "
						+ "  ?a a qa:AnnotationOfInstance . "
						+ "  ?a oa:hasBody ?uri } }";
				ResultSet r=selectTripleStore(sparql,endpoint);
				List<String> systemAnswers = new ArrayList<String>();
				while (r.hasNext()){
					QuerySolution s=r.next();
					logger.info("System answers {} ",s.getResource("uri").toString());
					systemAnswers.add(s.getResource("uri").toString());
				}
				
				
				//Retrieve the expected resources from the SPARQL query
				List<String> expectedAnswers = new ArrayList<String>();
				if (questionObject.getJSONObject("query").has("sparql")){
					String query=questionObject.getJSONObject("query").get("sparql").toString();
					System.out.println(query);
					Pattern pattern=Pattern.compile("<http://dbpedia.org/resource/.*?>");
				    Matcher matcher = pattern.matcher(query);
				    while (matcher.find()){
				    	//System.out.println(matcher.group().toString());
				    	if (expectedAnswers.contains(matcher.group().toString().replace("<", "").replace(">", ""))==false){
				    		expectedAnswers.add(matcher.group().toString().replace("<", "").replace(">", ""));
				    		logger.info("Expected Answers {} ", matcher.group().toString().replace("<", "").replace(">", ""));
				    	}
				    }
				}
				
				//Compute precision and recall
				Metrics m = new Metrics();
				m.compute(expectedAnswers,systemAnswers);
				logger.info("PRECISION {} ",m.precision);
				logger.info("RECALL {} ",m.recall);
				logger.info("F-MEASURE {} ",m.fMeasure);
				globalPrecision+=m.precision;
				globalRecall+=m.recall;
				if (m.fMeasure==1){
					numerCorrectQuestion++;
				}
				if (systemAnswers.size()!=0 && m.recall==1.0){
					numerRecallOne++;
				}
				if (m.recall==1.0){
					rightQuestions.add(1);
				} else {
					rightQuestions.add(0);
				}
				globalFMeasure+=m.fMeasure;
				count++;
			}
			System.out.println("Global Precision="+(double)globalPrecision/count);
			System.out.println("Global Recall="+(double)globalRecall/count);
			System.out.println("Global F-measure="+(double)globalFMeasure/count);
			System.out.println("Total number of right questions"+numerCorrectQuestion);
			System.out.println("Total number of question with recall truly 1"+numerRecallOne);
			System.out.println("Right questions:");
			for (int i : rightQuestions){
				System.out.println(i);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
	public void loadTripleStore(String sparqlQuery, String endpoint) {
		UpdateRequest request = UpdateFactory.create(sparqlQuery);
		UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);
		proc.execute();
	}

	public ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
		return qExe.execSelect();
	}
}
