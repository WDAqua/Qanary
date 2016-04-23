package eu.wdaqua.qanary.spotlightNED;

import java.io.IOException;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Iterator;

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
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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

import java.io.FileReader;
import java.util.HashSet;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
/**
 * represents a wrapper of the DBpedia Spotlight as NED
 * 
 * @author Kuldeep Singh
 *
 */

@Component
public class DBpediaSpotlightNED extends QanaryComponent {
	//private String agdistisService="http://139.18.2.164:8080/AGDISTIS";
	private static final Logger logger = LoggerFactory.getLogger(DBpediaSpotlightNED.class);
	/**
	 * default processor of a QanaryMessage
	 */
	
	public String runCurl1(String question)
	{
		
	
		String xmlResp = "";
		try{
			URL url = new URL("http://spotlight.sztaki.hu:2222/rest/disambiguate/");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		//String temp = "text=<?xml version=\"1.0\" encoding=\"UTF-8\"?><annotation text=\""+question+"\"><surfaceForm name=\"published\" offset=\"23\" /><surfaceForm name=\"Heart\" offset=\"63\" /></annotation>";
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		
		DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
		
		wr.write(("text="+question).getBytes("UTF-8"));
		wr.flush();
		wr.close();
		
		
		
		
		//InputStreamReader ir = new InputStreamReader(connection.getInputStream());
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		xmlResp = response.toString();
		
		System.out.println("Curl1 Response: \n"+xmlResp);
		}catch(Exception e) {}
		return(xmlResp);
		
	}
	

	
	public String getXmlFromQuestion(String question, ArrayList<Link> offsets)
	{
	 String xmlFileContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><annotation text=\""+question+"\">";
	 
	 for(Link sel:offsets)
	 {
	  int begin = sel.begin;
	  int end = sel.end;
	  String surNam = question.substring(begin, end);
	  xmlFileContent += "<surfaceForm name=\""+surNam+"\" offset=\""+begin+"\"/>";
	 }
	 xmlFileContent+= "</annotation>";

	 return xmlFileContent;
	}

	
	public QanaryMessage process(QanaryMessage QanaryMessage) {
		logger.info("process: {}", QanaryMessage);
		
		try {
			long startTime = System.currentTimeMillis();
			logger.info("process: {}", QanaryMessage);
			//STEP1: Retrive the named graph and the endpoint
			String endpoint=QanaryMessage.getEndpoint().toASCIIString();
			String namedGraph=QanaryMessage.getInGraph().toASCIIString();
			logger.info("Endpoint: {}", endpoint);
			logger.info("InGraph: {}", namedGraph);
			
			//STEP2: Retrive information that are needed for the computations
			
			
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
			ArrayList<Link> links = new ArrayList<Link>();
			while (r.hasNext()){
				QuerySolution s=r.next();
				Link link = new Link();
				link.begin=s.getLiteral("start").getInt();
				link.end=s.getLiteral("end").getInt();
				links.add(link);
			}
			
			
		
			
			//it will create XML coneten, which needs to be input in DBpedia NED with curl command
			String content= getXmlFromQuestion(question,links);
			
			//storing the output of DBpediaNED, which is a JSON 
			String response= runCurl1(content);
			
			
			
			

			//STEP3: Call the DBpedia NED service
			
			//Now the output of DBPediaNED, which is JSON, is parsed below to fetch the corresponding URIs
			try{
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(response);
			JSONArray arr = (JSONArray) json.get("Resources");
			Iterator i = arr.iterator();
			int cnt=0;
			while (i.hasNext()) {
				
				JSONObject obj = (JSONObject) i.next();
				String uri = (String) obj.get("@URI");
				links.get(cnt++).link=uri;
				
			}
			}catch (Exception e){}
			
		/*	JSONArray arr = new JSONArray(response);
			for (int i = 0; i < arr.length(); i++){
				System.out.println("Here");
			    Link l=new Link();
				l.link = arr.getJSONObject(i).getString("@URI");
				l.begin = arr.getJSONObject(i).getInt("start");
				l.end = arr.getJSONObject(i).getInt("start")+arr.getJSONObject(i).getInt("offset");
				links.add(l);
			}
			*/
			logger.info("apply vocabulary alignment on outgraph");
			
			//STEP4: Push the result of the component to the triplestore
			//long startTime = System.currentTimeMillis();
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

		} catch (Exception e) {
			// TODO Auto-generated catch block
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