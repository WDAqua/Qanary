package eu.wdaqua.qanary.StanfordNER;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

/**
 * represents a wrapper of the DBpedia Spotlight tool used here as a spotter
 * 
 * @author Kuldeep Singh
 *
 */

@Component
public class WrapperSpotlight extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(WrapperSpotlight.class);
	/**
	 * default processor of a QanaryMessage
	 */
	
	public List<String> usingXml(String urladd) {
		String urladdress = "";
		List<String> retLst = new ArrayList<String>();
		try {
			
			URL url = new URL(urladd);
			URLConnection urlConnection = url.openConnection();
			HttpURLConnection connection = null;
			if (urlConnection instanceof HttpURLConnection) {
				connection = (HttpURLConnection) urlConnection;
			} else {
				System.out.println("Please enter an HTTP URL.");
				return retLst;
			}
			
			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(connection.getInputStream());

			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("surfaceForm");
			
			
			//System.out.println("----------------------------");
			boolean flg = true;
			for (int temp = 0; temp < nList.getLength(); temp++) {
				
				Node nNode = nList.item(temp);
				
				
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;
					String text = eElement.getAttribute("name");
					String offset = eElement.getAttribute("offset");

					String startEnd = Integer.parseInt(offset)+","+(text.length()+Integer.parseInt(offset));
					retLst.add(startEnd);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retLst;
	}
	
	
	public List<String> getResults(String input)
	{
		/*This can be an alternative for passing text using API
		 String SpotterService = "http://spotlight.sztaki.hu:2222/rest/spot";
		UriComponentsBuilder service = UriComponentsBuilder.fromHttpUrl(SpotterService)
		        .queryParam("text", qns);
		logger.info("Service request "+service);
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.exchange(service.build().encode().toUri(), HttpMethod.GET, null, String.class);
		logger.info("Xml document from alchemy api {}", response.getBody());
		*/
	
		String madeUrlFromInput = "http://spotlight.sztaki.hu:2222/rest/spot?text=";
		/*String qns[] = input.split(" ");
		String append = String.join("%20", qns);*/
		try{
		madeUrlFromInput += URLEncoder.encode(input, "UTF-8");;//+"&executeSparqlQuery=on&relationExtractorType=Semantic";
		}catch(Exception e){}
		List<String> retLst = new ArrayList<String>();
		{
			
			System.out.println("URL is: "+madeUrlFromInput);
			retLst = usingXml(madeUrlFromInput);
		}
		
		return retLst;
	}
	
	public QanaryMessage process(QanaryMessage myQanaryMessage) {
		long startTime = System.currentTimeMillis();
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		logger.info("Qanary Message: {}", myQanaryMessage);

		
		try {
			
			//STEP1: Retrive the named graph and the endpoint
			
			//String endpoint = myQanaryMessage.getEndpoint().toASCIIString();
		//endpoint= "http://admin:admin@104.155.21.91:5820/qanary/query";
		//http://admin:admin@localhost:5820/qanary/query
			//String namedGraph = myQanaryMessage.getInGraph().toASCIIString();
			//logger.info("store data at endpoint {}", endpoint);
			//logger.info("store data in graph {}", namedGraph);

			
			String endpoint = myQanaryMessage.getEndpoint().toASCIIString();
			String namedGraph = myQanaryMessage.getInGraph().toASCIIString();
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
				//Retrive the question itself
				RestTemplate restTemplate = new RestTemplate();
				//TODO: pay attention to "/raw" maybe change that
				ResponseEntity<String> responseEntity = restTemplate.getForEntity(uriQuestion+"/raw", String.class);
				String question=responseEntity.getBody();
				logger.info("Question: {}", question);
			
			//String uriQuestion="http://wdaqua.eu/dummy";
			//String question="Brooklyn Bridge was designed by Alfred";
			
			//STEP3: Pass the information to the component and execute it	
			//logger.info("apply vocabulary alignment on outgraph");
			
			WrapperSpotlight qaw = new WrapperSpotlight();
			
			List<String > stEn = new ArrayList<String>();
			stEn  = qaw.getResults(question);
			int cnt = 0;
			ArrayList<Selection> selections = new ArrayList<Selection>();
			for(String str: stEn)
			{
				Selection s1 = new Selection();
				String str1[] = str.split(",");
				s1.begin = Integer.parseInt(str1[0]);
				s1.end = Integer.parseInt(str1[1]);
				selections.add(s1);
			}
			//STEP4: Push the result of the component to the triplestore
			
			for (Selection s: selections){
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
						 +"                    oa:start \""+s.begin+"\"^^xsd:nonNegativeInteger ; "
						 +"                    oa:end  \""+s.end+"\"^^xsd:nonNegativeInteger  "
						 +"           ] "
						 +"  ] ; "
						 +"     oa:annotatedBy <http://spotlight.sztaki.hu:2222/rest/spot> ; "
						 +"	    oa:AnnotatedAt ?time  "
						 +"}} "
						 +"WHERE { " 
						 +"BIND (IRI(str(RAND())) AS ?a) ."
						 +"BIND (now() as ?time) "
					     +"}";
				loadTripleStore(sparql, endpoint);
			}
			long estimatedTime = System.currentTimeMillis() - startTime;
			logger.info("Time {}", estimatedTime);		

		} catch (Exception e) {//MalformedURLException e) {
			e.printStackTrace();
		}

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

