package eu.wdaqua.qanary.FOX;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.UUID;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

/**
 * represents a wrapper of the Stanford NER tool used here as a spotter
 * 
 * @author Dennis Diefenbach
 *
 */

@Component
public class FOXComponent extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(FOXComponent.class);
	private static final String foxService="http://fox-demo.aksw.org/api"; 
	/**
	 * default processor of a QanaryMessage
	 */
	public QanaryMessage process(QanaryMessage myQanaryMessage) {
		long startTime = System.currentTimeMillis();
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		logger.info("Qanary Message: {}", myQanaryMessage);

		// STEP1: Retrieve the named graph and the endpoint
		String endpoint = myQanaryMessage.getEndpoint().toASCIIString();
		String namedGraph = myQanaryMessage.getInGraph().toASCIIString();
		logger.info("Endpoint: {}", endpoint);
		logger.info("InGraph: {}", namedGraph);

		// STEP2: Retrieve information that are needed for the computations
		//Retrive the uri where the question is exposed 
		String sparql = "PREFIX qa:<http://www.wdaqua.eu/qa#> "
						+"SELECT ?questionuri "
						+"FROM <"+namedGraph+"> "
						+"WHERE {?questionuri a qa:Question}";
		ResultSet result=selectTripleStore(sparql,endpoint);
		String uriQuestion=result.next().getResource("questionuri").toString();
		logger.info("Uri of the question: {}", uriQuestion);
		//Retrive the question itself
		RestTemplate restTemplate = new RestTemplate();
		//TODO: pay attention to "/raw" maybe change that
		ResponseEntity<String> responseEntity = restTemplate.getForEntity(uriQuestion+"/raw", String.class);
		String question=responseEntity.getBody();
		logger.info("Question: {}", question);

		// STEP3: Pass the information to the component and execute it
		//curl -d type=text -d task=NER -d output=N-Triples --data-urlencode "input=The foundation of the University of Leipzig in 1409 initiated the city's development into a centre of German law and the publishing industry, and towards being a location of the Reichsgericht (High Court), and the German National Library (founded in 1912). The philosopher and mathematician Gottfried Leibniz was born in Leipzig in 1646, and attended the university from 1661-1666." -H "Content-Type: application/x-www-form-urlencoded" http://fox-demo.aksw.org/api 
		//Create body
		MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<String, String>();
		bodyMap.add("type", "text");
		bodyMap.add("task", "NER");
		bodyMap.add("output", "N-Triples");
		bodyMap.add("lang", "en");
		bodyMap.add("input", question);
		//Set Header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		//Set request
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(bodyMap, headers);
		//Execute service
		ResponseEntity<String> model = restTemplate.exchange(foxService, HttpMethod.POST, request, String.class);
		String response = model.getBody();
		logger.info("Response from FOX API"+response);
		
		// STEP4: Vocabulary alignment
		logger.info("Apply vocabulary alignment on outgraph");
		//Retrieve the triples from FOX
		JSONObject obj = new JSONObject(response);
		String triples=URLDecoder.decode(obj.getString("output"));
		
		//Create a new temporary named graph
		final UUID runID = UUID.randomUUID();
		String namedGraphTemp="urn:graph:" + runID.toString();
		
		//Insert data into temporary graph
		sparql="INSERT DATA { GRAPH <"+namedGraphTemp+"> {"+triples+"}}";
		logger.info(sparql);
		loadTripleStore(sparql, endpoint);
		
		//Align to QANARY vocabulary
		sparql ="prefix qa: <http://www.wdaqua.eu/qa#> "
				 +"prefix oa: <http://www.w3.org/ns/openannotation/core/> "
				 +"prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
				 +"INSERT { "
				 +"GRAPH <"+namedGraph+"> { "
				 +"  ?a a qa:AnnotationOfSpotInstance . "
				 +"  ?a oa:hasTarget [ "
				 +"           a    oa:SpecificResource; "
				 +"           oa:hasSource    <"+uriQuestion+">; "
				 +"           oa:hasSelector  [ "
				 +"                    a oa:TextPositionSelector ; "
				 +"                    oa:start ?begin ; "
				 +"                    oa:end  ?end "
				 +"           ] "
				 +"  ] ; "
				 +"     oa:annotatedBy <http://fox-demo.aksw.org> ; "
				 +"	    oa:AnnotatedAt ?time  "
				 +"}} "
				 +"WHERE { " 
				 +"	SELECT ?a ?s ?begin ?end ?time "
				 +"	WHERE { " 
				 +"		graph <"+namedGraphTemp+"> { "
				 +"			?s	<http://ns.aksw.org/scms/beginIndex> ?begin . "
				 +"			?s  <http://ns.aksw.org/scms/endIndex> ?end . "
				 +"			BIND (IRI(str(RAND())) AS ?a) ."
				 +"			BIND (now() as ?time) "
				 +"		} "
				 +"	} "
				 +"}";
		loadTripleStore(sparql, endpoint); 

		//Drop the temporary graph
		sparql="DROP SILENT GRAPH <"+namedGraphTemp+">";
		loadTripleStore(sparql, endpoint);
		
		long estimatedTime = System.currentTimeMillis() - startTime;
		logger.info("Time: {}", estimatedTime);

		return myQanaryMessage;
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

	class Selection {
		public int begin;
		public int end;
	}

}
