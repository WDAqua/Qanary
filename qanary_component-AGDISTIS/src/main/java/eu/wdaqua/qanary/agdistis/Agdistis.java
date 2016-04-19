package eu.wdaqua.qanary.agdistis;

import java.io.IOException;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.util.Map.Entry;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

/**
 * represents a wrapper of the Stanford NER tool used here as a spotter
 * 
 * @author Dennis Diefenbach
 *
 */

@Component
public class Agdistis extends QanaryComponent {
	private String agdistisService="http://139.18.2.164:8080/AGDISTIS";
	private static final Logger logger = LoggerFactory.getLogger(Agdistis.class);
	
	public QanaryMessage process(QanaryMessage QanaryMessage) {
		try {
			long startTime = System.currentTimeMillis();
			logger.info("process: {}", QanaryMessage);
			
			//STEP1: Retrive the named graph and the endpoint
			String endpoint=QanaryMessage.getEndpoint().toASCIIString();
			String namedGraph=QanaryMessage.getInGraph().toASCIIString();
			logger.info("Endpoint: {}", endpoint);
			logger.info("InGraph: {}", namedGraph);
			
			// STEP2: Retrieve information that are needed for the computations
			//Retrieve the uri where the question is exposed 
			String sparql = "PREFIX qa:<http://www.wdaqua.eu/qa#> "
							+"SELECT ?questionuri "
							+"FROM <"+namedGraph+"> "
							+"WHERE {?questionuri a qa:Question}";
			ResultSet result=selectTripleStore(sparql,endpoint);
			String uriQuestion=result.next().getResource("questionuri").toString();
			logger.info("Uri of the question: {}", uriQuestion);
			//Retrieve the question itself
			RestTemplate restTemplate = new RestTemplate();
			//TODO: pay attention to "/raw" maybe change that
			ResponseEntity<String> responseEntity = restTemplate.getForEntity(uriQuestion+"/raw", String.class);
			String question=responseEntity.getBody();
			logger.info("Question: {}", question);
			//Retrieves the spots from the knowledge graph
			sparql="PREFIX qa: <http://www.wdaqua.eu/qa#> "
						+"PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " 
						+"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
						+"SELECT ?start ?end "
						+"FROM <"+namedGraph+"> "
						+"WHERE { " 
						+"?a a qa:AnnotationOfNamedEntity . " 
						+"?a oa:hasTarget [ "
						+"		a    oa:SpecificResource; "
				        +"		oa:hasSource    ?q; "
				        +"		oa:hasSelector  [ " 
				        +"			a oa:TextPositionSelector ; "
				        +"			oa:start ?start ; "
				        +"			oa:end  ?end "
				        +"		] "
				        +"] ; "
				        +"oa:annotatedBy ?annotator "
						+"} "
						+"ORDER BY ?start ";
			ResultSet r=selectTripleStore(sparql,endpoint);
			ArrayList<Spot> spots = new ArrayList<Spot>();
			while (r.hasNext()){
				QuerySolution s=r.next();
				Spot spot = new Spot();
				spot.begin=s.getLiteral("start").getInt();
				spot.end=s.getLiteral("end").getInt();
				logger.info("Spot: {}-{}", spot.begin, spot.end);
				spots.add(spot);
			}
			
			//STEP3: Call the AGDISTIS service
			//Informations about the AGDISTIS API can be found here: https://github.com/AKSW/AGDISTIS/wiki/2-Asking-the-webservice
			//curl --data-urlencode "text='The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>.'" -d type='agdistis' http://139.18.2.164:8080/AGDISTIS
			//Match the format "The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>."
			String input=question;
			Integer offset=0;
			for (Spot spot : spots){
				input=input.substring(0,spot.begin+offset)+"<entity>"+input.substring(spot.begin+offset,spot.end+offset)+"</entity>"+input.substring(spot.end+offset,input.length());
				offset+="<entity>".length()+"</entity>".length();
			}
			//String input="The <entity>University of Leipzig</entity> in <entity>Barack Obama</entity>.";
			logger.info("Input to Agdistis: "+input);
			UriComponentsBuilder service = UriComponentsBuilder.fromHttpUrl(agdistisService);
			logger.info("Service request "+service);
			String body="type=agdistis&"+"text='"+URLEncoder.encode(input, "UTF-8")+"'";
			//RestTemplarestTemplate = new RestTemplate();
			String response = restTemplate.postForObject(service.build().encode().toUri(), body, String.class);
			logger.info("JSON document from Agdistis api {}", response);
			//Extract entities
			ArrayList<Link> links=new ArrayList<Link>();
			JSONArray arr = new JSONArray(response);
			for (int i = 0; i < arr.length(); i++){
				if (arr.getJSONObject(i).isNull("disambiguatedURL")==false){
					Link l=new Link();
					l.link = arr.getJSONObject(i).getString("disambiguatedURL");
					l.begin = arr.getJSONObject(i).getInt("start")-1;
					l.end = arr.getJSONObject(i).getInt("start")-1+arr.getJSONObject(i).getInt("offset");
					links.add(l);
				}
			}
			
			//STEP4: Push the result of the component to the triplestore
			logger.info("Apply vocabulary alignment on outgraph");
			for (Link l: links){
				sparql="prefix qa: <http://www.wdaqua.eu/qa#> "
						 +"prefix oa: <http://www.w3.org/ns/openannotation/core/> "
						 +"prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
						 +"INSERT { "
						 +"GRAPH <"+namedGraph+"> { "
						 +"  ?a a qa:AnnotationOfNamedEntity . "
						 +"  ?a oa:hasTarget [ "
						 +"           a    oa:SpecificResource; "
						 +"           oa:hasSource    <"+uriQuestion+">; "
						 +"           oa:hasSelector  [ "
						 +"                    a oa:TextPositionSelector ; "
						 +"                    oa:start \""+l.begin+"\"^^xsd:nonNegativeInteger ; "
						 +"                    oa:end  \""+l.end+"\"^^xsd:nonNegativeInteger  "
						 +"           ] "
						 +"  ] . "
						 +"  ?a oa:hasBody <"+l.link+"> ;"
						 +"     oa:annotatedBy <http://agdistis.aksw.org> ; "
						 +"	    oa:AnnotatedAt ?time  "
						 +"}} "
						 +"WHERE { " 
						 +"BIND (IRI(str(RAND())) AS ?a) ."
						 +"BIND (now() as ?time) "
					     +"}";
				logger.info("Sparql query {}", sparql);
				loadTripleStore(sparql, endpoint);
			}
			long estimatedTime = System.currentTimeMillis() - startTime;
			logger.info("Time {}", estimatedTime);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return QanaryMessage;
	}
	
	public void loadTripleStore(String sparqlQuery, String endpoint){
		UpdateRequest request = UpdateFactory.create(sparqlQuery) ;
		UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);
	    proc.execute() ;
	}
	
	public ResultSet selectTripleStore(String sparqlQuery, String endpoint){
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query );
		return qExe.execSelect();
	}
	
	class Spot {
		public int begin;
		public int end;
	}
	
	class Link {
		public int begin;
		public int end;
		public String link;
	}
	
}