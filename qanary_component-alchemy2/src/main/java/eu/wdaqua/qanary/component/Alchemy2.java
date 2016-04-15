package eu.wdaqua.qanary.component;

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

import java.util.Map.Entry;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
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
public class Alchemy2 extends QanaryComponent {
	private String alchemyKey="7fdef5a245edb49cfc711e80217667be512869b9";
	private String alchemyService="http://access.alchemyapi.com/calls/text/TextGetRankedNamedEntities";
	private static final Logger logger = LoggerFactory.getLogger(Alchemy2.class);
	/**
	 * default processor of a QanaryMessage
	 */
	public QanaryMessage process(QanaryMessage QanaryMessage) {
		System.out.println("Hello!");
		//org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		logger.info("process: {}", QanaryMessage);
		// TODO: implement processing of question
		
		try {
			
			//STEP1: Retrive the named graph and the endpoint
			String endpoint=QanaryMessage.get(new URI(QanaryMessage.endpointKey)).toString();
			endpoint="http://admin:admin@localhost:5820/question/query";
			String namedGraph=QanaryMessage.get(new URI(QanaryMessage.inGraphKey)).toString();
			namedGraph="aaaaa";
			logger.info("store data at endpoint {}", endpoint);
			logger.info("store data in graph {}", namedGraph);
			
			//STEP2: Retrive information that are needed for the computations
			//TODO when "/question" is properly implemented and all things are loaded into the named graph 
			// - The question
			//String sparql = "PREFIX qa:<http://www.wdaqua.eu/qa#> "
			//				+"SELECT ?questionuri "
			//				+"FROM "+namedGraph+" "
			//				+"WHERE {?questionuri a qa:Question}";
			//ResultSet result=selectTripleStore(sparql,endpoint);
			//String uriQuestion =result.next().getResource("questionuri").toString();
			//logger.info("uri of the question {}", uriQuestion);
			
			//RestTemplate restTemplate = new RestTemplate();
			//ResponseEntity<String> responseEntity =  restTemplate.getForEntity(uriQuestion, String.class);
			//String question=responseEntity.getBody();
			//logger.info("question {}", question);
			
			String uriQuestion="http://wdaqua.eu/dummy";
			String question="Who meet Barack Obama and Michelle Obama?";
			logger.info("question {}", question);
			
			//STEP3: Call the alchemy service
			UriComponentsBuilder service = UriComponentsBuilder.fromHttpUrl(alchemyService)
			        .queryParam("apikey", alchemyKey)
			        .queryParam("text", question);
			logger.info("Service request "+service);
			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<String> response = restTemplate.exchange(service.build().encode().toUri(), HttpMethod.GET, null, String.class);
			logger.info("Xml document from alchemy api {}", response.getBody());
			
			ArrayList<Selection> selections=new ArrayList<Selection>();
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(response.getBody()));
			Document doc = dBuilder.parse(is);
			System.out.println(doc.toString());
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("entity");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					String entity = eElement.getElementsByTagName("text").item(0).getTextContent();
					System.out.println(entity);
					for (int i = -1; (i = question.indexOf(entity, i + 1)) != -1; ) {
					    Selection s = new Selection();
						s.begin=i;
					    s.end=i+entity.length();
					    selections.add(s);
					}
				}
			}
			
			logger.info("apply vocabulary alignment on outgraph");
			
			//STEP4: Push the result of the component to the triplestore
			long startTime = System.currentTimeMillis();
			for (Selection s: selections){
				String sparql="prefix qa: <http://www.wdaqua.eu/qa#> "
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
						 +"     oa:annotatedBy <http://www.alchemyapi.com> ; "
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

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
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
	
	class Selection {
		public int begin;
		public int end;
	}
	
}