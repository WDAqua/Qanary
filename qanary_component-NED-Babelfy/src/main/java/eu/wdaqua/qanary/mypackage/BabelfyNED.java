package eu.wdaqua.qanary.mypackage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class BabelfyNED extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(BabelfyNED.class);

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * @throws Exception 
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);
		// TODO: implement processing of question
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
	      QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion(myQanaryMessage);
	      String myQuestion = myQanaryQuestion.getTextualRepresentation();
	      ArrayList<Selection> selections = new ArrayList<Selection>();
	      ArrayList<Link> links = new ArrayList<Link>();
	      logger.info("Question {}", myQuestion);
	      
	      String thePath = "";
	      thePath = URLEncoder.encode(myQuestion, "UTF-8"); 
	      
	      logger.info("Path {}", thePath);
	      
	      HttpClient httpclient = HttpClients.createDefault();
	      HttpGet httpget = new HttpGet("https://babelfy.io/v1/disambiguate?text="+thePath+"&lang=AGNOSTIC&key=54c2f995-0b2a-4f46-beb0-002202765241");
	      //httpget.addHeader("User-Agent", USER_AGENT);
	      HttpResponse response = httpclient.execute(httpget);
	      try {
	    	 
	          HttpEntity entity = response.getEntity();
	          if (entity != null) {
	             InputStream instream = entity.getContent();
  // String result = getStringFromInputStream(instream);
	             String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
	             JSONArray jsonArray = new JSONArray(text); 
	            for (int i = 0; i < jsonArray.length(); i++) {
	                JSONObject explrObject = jsonArray.getJSONObject(i);
	                logger.info("JSON {}", explrObject);
	                double score = (double) explrObject.get("score");
	                if(score>=0.5)
	                {
	                JSONObject char_array = explrObject.getJSONObject("charFragment");
	               int begin = (int) char_array.get("start");
	                int end = (int) char_array.get("end");
	               logger.info("Question: {}", begin);
	                logger.info("Question: {}", end);
	               Selection s = new Selection();
	                s.begin = begin;
	                s.end = end+1;
	                selections.add(s);
	                Link l = new Link();
	                l.begin = begin;
	                l.end = end+1;
	                l.link= (String) explrObject.get("DBpediaURL");
	                links.add(l);
	                }
	            }
	          }
	      }
	      catch (ClientProtocolException e) {
	 		 logger.info("Exception: {}", e);
	         // TODO Auto-generated catch block
	     } catch (IOException e1) {
	     	logger.info("Except: {}", e1);
	         // TODO Auto-generated catch block
	     }
		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// TODO: insert data in QanaryMessage.outgraph

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)
		for (Selection s : selections) {
            String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
                    + "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
                    + "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " + "INSERT { " + "GRAPH <" + myQanaryMessage.getOutGraph() + "> { "
                    + "  ?a a qa:AnnotationOfSpotInstance . " + "  ?a oa:hasTarget [ "
                    + "           a    oa:SpecificResource; " + "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; "
                    + "           oa:hasSelector  [ " + "                    a oa:TextPositionSelector ; "
                    + "                    oa:start \"" + s.begin + "\"^^xsd:nonNegativeInteger ; "
                    + "                    oa:end  \"" + s.end + "\"^^xsd:nonNegativeInteger  " + "           ] "
                    + "  ] ; " + "     oa:annotatedBy <http://nlp.stanford.edu/software/CRF-NER.shtml> ; "
                    + "	    oa:AnnotatedAt ?time  " + "}} " + "WHERE { " + "BIND (IRI(str(RAND())) AS ?a) ."
                    + "BIND (now() as ?time) " + "}";
            myQanaryUtils.updateTripleStore(sparql);
        }
		
		for (Link l : links) {
            String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
                    + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
                    + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
                    + "INSERT { " + "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
                    + "  ?a a qa:AnnotationOfInstance . " //
                    + "  ?a oa:hasTarget [ " //
                    + "           a    oa:SpecificResource; " //
                    + "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; " //
                    + "           oa:hasSelector  [ " //
                    + "                    a oa:TextPositionSelector ; " //
                    + "                    oa:start \"" + l.begin + "\"^^xsd:nonNegativeInteger ; " //
                    + "                    oa:end  \"" + l.end + "\"^^xsd:nonNegativeInteger  " //
                    + "           ] " //
                    + "  ] . " //
                    + "  ?a oa:hasBody <" + l.link + "> ;" //
                    + "     oa:annotatedBy <https://github.com/dbpedia-spotlight/dbpedia-spotlight> ; " //
                    + "	    oa:AnnotatedAt ?time  " + "}} " //
                    + "WHERE { " //
                    + "  BIND (IRI(str(RAND())) AS ?a) ."//
                    + "  BIND (now() as ?time) " //
                    + "}";
            logger.debug("Sparql query: {}", sparql);
            myQanaryUtils.updateTripleStore(sparql);
        }
		return myQanaryMessage;
	}
	class Selection {
        public int begin;
        public int end;
    }
	class Link {
        public int begin;
        public int end;
        public String link;
    }
}
