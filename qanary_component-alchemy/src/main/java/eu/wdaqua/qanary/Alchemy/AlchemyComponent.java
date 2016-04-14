package eu.wdaqua.qanary.Alchemy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

/**
 * represents a wrapper of the Stanford NER tool used here as a spotter
 * 
 * @author Dennis Diefenbach
 *
 */

@Component
public class AlchemyComponent extends QanaryComponent {
	private String alchemyKey="apikey=7fdef5a245edb49cfc711e80217667be512869b9";
	private static final Logger logger = LoggerFactory.getLogger(AlchemyComponent.class);
	/**
	 * default processor of a QanaryMessage
	 */
	public QanaryMessage process(QanaryMessage QanaryMessage) {
		//org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		logger.info("process: {}", QanaryMessage);
		// TODO: implement processing of question
		
		try {
			
			//STEP1: Retrive the named graph and the endpoint
			String endpoint=QanaryMessage.get(new URL(QanaryMessage.endpointKey)).toString();
			String namedGraph=QanaryMessage.get(new URL(QanaryMessage.inGraphKey)).toString();
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
			String question="Brooklyn Bridge was designed by Alfred";
			logger.info("question {}", question);
			
			//STEP3: Call the alchemy service
			RestTemplate restTemplate = new RestTemplate();
			String service="http://access.alchemyapi.com/calls/text/TextGetRankedNamedEntities?"+alchemyKey
							+"&text"+URLEncoder.encode(question, "UTF-8");
			ResponseEntity<String> responseEntity =  restTemplate.getForEntity(service, String.class);
			String result=responseEntity.getBody();
			
			ArrayList<Selection> selections=new ArrayList<Selection>();
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(result);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("entity");
			boolean flg = true;
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					String entity = eElement.getElementsByTagName("text").item(0).getTextContent();
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
						 +"     oa:annotatedBy <http://nlp.stanford.edu/software/CRF-NER.shtml> ; "
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

