package eu.wdaqua.qanary.dbpediaspotter;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
 * @author Kuldeep Singh
 *
 */

@Component
public class SpotlightWrapper extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(SpotlightWrapper.class);
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
				//System.out.println("Inside");
				Node nNode = nList.item(temp);
				
				//System.out.println("\nCurrent Element :" + nNode.getNodeName());

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;
					String text = eElement.getAttribute("name");
					String offset = eElement.getAttribute("offset");
					//System.out.println("Text : "    + text);
					//System.out.println("Offset : "    + offset );
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
		
		String madeUrlFromInput = "http://spotlight.sztaki.hu:2222/rest/spot?text=";
		String qns[] = input.split(" ");
		String append = String.join("%20", qns);
		madeUrlFromInput += append;//+"&executeSparqlQuery=on&relationExtractorType=Semantic";
		List<String> retLst = new ArrayList<String>();
		
		{
			
			System.out.println("URL is: "+madeUrlFromInput);
			retLst = usingXml(madeUrlFromInput);
		}
		
		return retLst;
	}
	
	
	
	
	
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
			//ResponseEntity<String> responseEntity =  restTemplate.getForEntity(questionuri, String.class);
			//String question=responseEntity.getBody();
			//logger.info("question {}", question);
			
			String uriQuestion="http://wdaqua.eu/dummy";
			String question="Brooklyn Bridge was designed by Alfred";
			
			//STEP3: Pass the information to the component and execute it	
			
			/*//TODO: ATTENTION: This should be done only ones when the component is started 
				//Define the properties needed for the pipeline of the Stanford parser
				Properties props = new Properties();
				props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
				//Create a new pipline
				StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
			
			//Create an empty annotation just with the given text
			Annotation document = new Annotation(question);
			//Run the stanford annotator on question
			pipeline.annotate(document);
			
			//Identify which parts of the question is tagged by the NER tool
			//Go ones through the question, 
			ArrayList<Selection> selections = new ArrayList<Selection>();
			CoreLabel startToken=null; //stores the last token with non-zero tag, if it does not exist set to null
			CoreLabel endToken=null; //stores the last found token with non-zero tag, if it does not exist set to null
			//Iterate over the tags
			for (CoreLabel token: document.get(TokensAnnotation.class)) {
				logger.info("tagged question {}", token.toString()+token.get(NamedEntityTagAnnotation.class));
	            //System.out.println(token.get(NamedEntityTagAnnotation.class));
	            if (token.get(NamedEntityTagAnnotation.class).equals("O")==false){
	            	if (startToken==null){
	            		startToken=token;
	            		endToken=token;
	            	} else {
	            		if (startToken.get(NamedEntityTagAnnotation.class)==token.get(NamedEntityTagAnnotation.class)){
	            			endToken=token;
	            		} else {
	            			Selection s = new Selection();
	            			s.begin=startToken.beginPosition();
	                		s.end=endToken.endPosition();
	                		selections.add(s);
	                		startToken=token;
	            		}
	            	}
	            } else {
	            	if (startToken!=null){ 
	            		Selection s = new Selection();
	            		s.begin=startToken.beginPosition();
	            		s.end=endToken.endPosition();
	            		selections.add(s);
	            		startToken=null;
	            		endToken=null;
	            	}
	            }
			}
			if (startToken!=null){ 
	    		Selection s = new Selection();
	    		s.begin=startToken.beginPosition();
	    		s.end=endToken.endPosition();
	    		selections.add(s);
	    	}
			
			logger.info("apply vocabulary alignment on outgraph");
			*/
			SpotlightWrapper qaw = new SpotlightWrapper();
			
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

