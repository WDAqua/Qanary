package eu.wdaqua.qanary.tgm;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.UUID;

/**
 * represents a wrapper of the OKBQA componet of template generator 
 *
 * @author Kuldeep
 */

@Component
public class Tgm extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(Tgm.class);
    

    /**
     * runCurlPOSTWithParam is a function to fetch the response from a CURL command using POST.
     */
    public static String runCurlPOSTWithParam(String weburl,String data,String contentType) throws Exception
	{
		
    	
    	//The String xmlResp is to store the output of the Template generator web service accessed via CURL command
    	
        String xmlResp = "";
        try {
        	URL url = new URL(weburl);
    		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    		
    		connection.setRequestMethod("POST");
    		connection.setDoOutput(true);
    		
    		connection.setRequestProperty("Content-Type", contentType);
    				
    		DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
    		wr.writeBytes(data);
    		wr.flush();
    		wr.close();
    		
    		
    	
    		BufferedReader in = new BufferedReader(
    		        new InputStreamReader(connection.getInputStream()));
    		String inputLine;
    		StringBuffer response = new StringBuffer();

    		while ((inputLine = in.readLine()) != null) {
    			response.append(inputLine);
    		}
    		in.close();
    		xmlResp = response.toString();
    		
    		System.out.println("Curl Response: \n"+xmlResp);
            logger.info("Response {}", xmlResp);
        } catch (Exception e) {
        }
        return (xmlResp);

	}
    
    public QanaryMessage process(QanaryMessage myQanaryMessage) {
    	System.out.println("The pipeline process is starting");
        long startTime = System.currentTimeMillis();
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
        logger.info("Qanary Message: {}", myQanaryMessage);

        // STEP1: Retrieve the named graph and the endpoint
        String endpoint = myQanaryMessage.getEndpoint().toASCIIString();
        String namedGraph = myQanaryMessage.getInGraph().toASCIIString();
        System.out.println("Graph is" +namedGraph);
        logger.info("Endpoint: {}", endpoint);
        logger.info("InGraph: {}", namedGraph);

        // STEP2: Retrieve information that are needed for the computations
        //Here, we need two parameters as input to be fetched from triplestore- question and language of the question.
        //So first, Retrive the uri where the question is exposed
        String sparql = "PREFIX qa:<http://www.wdaqua.eu/qa#> "
                + "SELECT ?questionuri "
                + "FROM <" + namedGraph + "> "
                + "WHERE {?questionuri a qa:Question}";
        
        ResultSet result = selectTripleStore(sparql, endpoint);
        String uriQuestion = result.next().getResource("questionuri").toString();
        logger.info("Uri of the question: {}", uriQuestion);
        //Retrive the question itself
        RestTemplate restTemplate = new RestTemplate();
        //TODO: pay attention to "/raw" maybe change that
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(uriQuestion + "/raw", String.class);
        String question = responseEntity.getBody();
        logger.info("Question: {}", question);
        
        //the below mentioned SPARQL query to fetch annotation of language from triplestore
        String questionlang = "PREFIX qa:<http://www.wdaqua.eu/qa#> "
                + "SELECT ?lang "
                + "FROM <" + namedGraph + "> "
                + "WHERE {?q a qa:Question ."
                + " ?anno <http://www.w3.org/ns/openannotation/core/hasTarget> ?q ."
                + " ?anno <http://www.w3.org/ns/openannotation/core/hasBody> ?lang ."
                + " ?anno a qa:AnnotationOfQuestionLanguage}";
        // Now fetch the language, in our case it is "en".
        ResultSet result1 = selectTripleStore(questionlang, endpoint);
        String language1 = result1.next().getLiteral("lang").toString();
        logger.info("Langauge of the Question: {}",language1);
        
       
        
        String url = "";
		String data = "";
		String contentType = "application/json";
		 
		//http://repository.okbqa.org/components/21 is the template generator URL
		//Sample input	for this is mentioned below. 
		/* 
		 * {
		  	"string": "Which river flows through Seoul?",
		  	"language": "en"
		   } http://ws.okbqa.org:1515/templategeneration/rocknrole
		*/
		
		//now arrange the Web service and input parameters in the way, which is needed for CURL command
		url = "http://ws.okbqa.org:1515/templategeneration/rocknrole";
		data = "{  \"string\":\""+question+"\",\"language\":\""+language1+"\"}";//"{  \"string\": \"Which river flows through Seoul?\",  \"language\": \"en\"}";
		System.out.println("\ndata :" +data);
		System.out.println("\nComponent : 21");
		String output1="";
		// pass the input in CURL command and call the function.
		
		try
		{
		output1= Tgm.runCurlPOSTWithParam(url, data, contentType);
		}catch(Exception e){}
		System.out.println("The output template is:" +output1);
		
        
		
		//return output1;
         
		
		// NOTE: Now the work remaning is: To push this output back to Triplestore.
		
		//Hence below code is commented as it needs to be modified.
		
		
       /* // STEP4: Vocabulary alignment
        logger.info("Apply vocabulary alignment on outgraph");
        //Retrieve the triples from FOX
        JSONObject obj = new JSONObject(response);
        String triples = URLDecoder.decode(obj.getString("output"));

        //Create a new temporary named graph
        final UUID runID = UUID.randomUUID();
        String namedGraphTemp = "urn:graph:" + runID.toString();

        //Insert data into temporary graph
        sparql = "INSERT DATA { GRAPH <" + namedGraphTemp + "> {" + triples + "}}";
        logger.info(sparql);
        loadTripleStore(sparql, endpoint);

        //Align to QANARY vocabulary
        sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
                + "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
                + "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
                + "INSERT { "
                + "GRAPH <" + namedGraph + "> { "
                + "  ?a a qa:AnnotationOfSpotInstance . "
                + "  ?a oa:hasTarget [ "
                + "           a    oa:SpecificResource; "
                + "           oa:hasSource    <" + uriQuestion + ">; "
                + "           oa:hasSelector  [ "
                + "                    a oa:TextPositionSelector ; "
                + "                    oa:start ?begin ; "
                + "                    oa:end  ?end "
                + "           ] "
                + "  ] ; "
                + "     oa:annotatedBy <http://fox-demo.aksw.org> ; "
                + "	    oa:AnnotatedAt ?time  "
                + "}} "
                + "WHERE { "
                + "	SELECT ?a ?s ?begin ?end ?time "
                + "	WHERE { "
                + "		graph <" + namedGraphTemp + "> { "
                + "			?s	<http://ns.aksw.org/scms/beginIndex> ?begin . "
                + "			?s  <http://ns.aksw.org/scms/endIndex> ?end . "
                + "			BIND (IRI(str(RAND())) AS ?a) ."
                + "			BIND (now() as ?time) "
                + "		} "
                + "	} "
                + "}";
        loadTripleStore(sparql, endpoint);

        //Drop the temporary graph
        sparql = "DROP SILENT GRAPH <" + namedGraphTemp + ">";
        loadTripleStore(sparql, endpoint);
*/
        long estimatedTime = System.currentTimeMillis() - startTime;
        logger.info("Time: {}", estimatedTime);

        return myQanaryMessage;
    }

    private void loadTripleStore(String sparqlQuery, String endpoint) {
        UpdateRequest request = UpdateFactory.create(sparqlQuery);
        UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);
        proc.execute();
    }

    private ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
        return qExe.execSelect();
    }

    private class Selection {
        public int begin;
        public int end;
    }

}


