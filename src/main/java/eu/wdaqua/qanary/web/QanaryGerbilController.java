package eu.wdaqua.qanary.web;

import java.net.URISyntaxException;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.QuerySolution;

import eu.wdaqua.qanary.message.QanaryMessage;


/**
 * controller for processing questions, i.e., related to the question answering process
 *
 * @author Dennis Diefenbach
 */
@Controller
public class QanaryGerbilController {
	
    private static final Logger logger = LoggerFactory.getLogger(QanaryGerbilController.class);

    @Value("${server.host}")
	private String host;
	@Value("${server.port}")
	private String port;
  
    //Set this to allow browser requests from other websites
    @ModelAttribute
    public void setVaryResponseHeader(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
    }    
 
    @RequestMapping(value="/gerbil",  method = RequestMethod.POST, produces = "application/json")
	public ResponseEntity<?> gerbil(
			@RequestParam(value = "query", required = true) final String query) throws URISyntaxException {
    	logger.info("Asked question {}"+query);
    	MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("question", query);
        map.add("componentlist[]", "wdaqua-core0");
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(host+":"+port+"/startquestionansweringwithtextquestion", map, String.class);
        org.json.JSONObject json = new org.json.JSONObject(response);
        String sparqlQuery =  "PREFIX qa: <http://www.wdaqua.eu/qa#> "
                        + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
			+ "SELECT ?sparql ?json "
        		+ "FROM <"+  json.get("graph").toString() + "> "
        		+ "WHERE { "
        		+ "  ?a a qa:AnnotationOfAnswerSPARQL . "
        		+ "  ?a oa:hasBody ?sparql . "
        		+ "  ?b a qa:AnnotationOfAnswerJSON . "
                	+ "  ?b oa:hasBody ?json " 
        		+ "}";
        
        ResultSet r = selectFromTripleStore(sparqlQuery, json.get("endpoint").toString());
        String jsonAnswer="";
    	String sparqlAnswer="";
        if (r.hasNext()){
        	QuerySolution t = r.next();
		jsonAnswer=t.getLiteral("json").toString();
        	sparqlAnswer=t.getLiteral("sparql").toString();
        }
        //Generates the following output
    	/*{
 		   "questions":[
 		      "question":{
 		         "answers":"...",
 		         "language":[
 		            {
 		               "SPARQL":"..."
 		            }
 		         ]
 		      }
 		   ]
 		}*/
        JSONObject obj = new JSONObject();
        JSONArray questions = new JSONArray();
        JSONObject item = new JSONObject();
        JSONObject question = new JSONObject();
        JSONArray language = new JSONArray();
        JSONObject sparql = new JSONObject();
        sparql.put("SPARQL", sparqlAnswer);
    	language.add(sparql);
    	question.put("answers", jsonAnswer);
    	question.put("language", language);
    	item.put("question", question);
    	questions.add(item);
    	obj.put("questions", questions);
    	return new ResponseEntity<org.json.simple.JSONObject>(obj,HttpStatus.OK);  	
	}

    
    /**
     * query a SPARQL endpoint with a given query
     */
    public ResultSet selectFromTripleStore(String sparqlQuery, String endpoint) {
        logger.debug("selectTripleStore on {} execute {}", endpoint, sparqlQuery);
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
        ResultSet resultset = qExe.execSelect();
        return resultset;
    }
    
}
