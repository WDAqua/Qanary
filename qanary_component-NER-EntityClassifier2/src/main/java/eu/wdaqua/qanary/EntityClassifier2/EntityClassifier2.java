package eu.wdaqua.qanary.EntityClassifier2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
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
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
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
public class EntityClassifier2 extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(EntityClassifier2.class);

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * @throws Exception 
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);
		// TODO: implement processing of question
		 // STEP1: Retrieve the named graph and the endpoint
		
      QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
      QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion(myQanaryMessage);
      String myQuestion = myQanaryQuestion.getTextualRepresentation();
      ArrayList<Selection> selections = new ArrayList<Selection>();

        logger.info("Question: {}", myQuestion);
        //STEP2
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("https://ner.vse.cz/thd/api/v2/extraction?apikey=66adcc8aa934448582447714443ca5f2&format=json&priority_entity_linking=true&entity_type=all");
        httppost.setEntity(new StringEntity(myQuestion));
        try {
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
        	             InputStream instream = entity.getContent();
           // String result = getStringFromInputStream(instream);
            String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
            JSONArray jsonArray = new JSONArray(text); 
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject explrObject = jsonArray.getJSONObject(i);
                int begin = (int) explrObject.get("startOffset");
                int end = (int) explrObject.get("endOffset");
                logger.info("Question: {}", explrObject);
                logger.info("Question: {}", begin);
                logger.info("Question: {}", end);
                Selection s = new Selection();
                s.begin = begin;
                s.end = end;
                selections.add(s);
            }
//            JSONObject jsnobject = new JSONObject(text);
//            JSONArray jsonArray = jsnobject.getJSONArray("endOffset");
//            for (int i = 0; i < jsonArray.length(); i++) {
//                JSONObject explrObject = jsonArray.getJSONObject(i);
//                logger.info("JSONObject: {}", explrObject);
//                logger.info("JSONArray: {}", jsonArray.getJSONObject(i));
//                //logger.info("Question: {}", text);
//                
//        }
            logger.info("Question: {}", text);
            logger.info("Question: {}", jsonArray);
            try {
                // do something useful
            } finally {
                instream.close();
            }
        }
        }
	 catch (ClientProtocolException e) {
		 logger.info("Exception: {}", myQuestion);
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
		return myQanaryMessage;
	}
	class Selection {
        public int begin;
        public int end;
    }
}
