package eu.wdaqua.qanary.web;

import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;

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
    
    @RequestMapping(value="/gerbil",  method = RequestMethod.POST, produces = "application/json")
	public String gerbil(
			@RequestParam(value = "query", required = true) final String question) throws URISyntaxException {
    	logger.info("Asked question {}"+question);
    	UriComponentsBuilder service = UriComponentsBuilder.fromHttpUrl(host+":"+port+"/startquestionansweringwithtextquestion");
        logger.info("Service request " + service);
        //String body = "question="+URLEncoder.encode(question, "UTF-8")+"&componentlist[]=Monolitic";
        String body = "question="+question+"&componentlist[]=Monolitic";
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(service.build().encode().toUri(), body, String.class);
        System.out.println("RESPONSE"+response);
        QanaryMessage m = new QanaryMessage(response);
        
        String sparqlQuery =  "SELECT ?sparql ?json "
        		+ "FROM "+  m.getInGraph() + " "
        		+ "WHERE { "
        		+ "  ?a a qa:AnnotationOfAnswerSPARQL . "
        		+ "  ?a oa:hasBody ?sparql . "
        		+ "  ?b a qa:AnnotationOfAnswerJSON . "
                + "  ?b oa:hasBody ?json " 
        		+ "}";
        
        ResultSet r = selectFromTripleStore(sparqlQuery, m.getEndpoint().toString());
        org.json.simple.JSONObject obj = new org.json.simple.JSONObject();
        String jsonAnswer="";
    	String sparqlAnswer="";
        if (r.hasNext()){
        	jsonAnswer=r.next().getLiteral("json").toString();
        	sparqlAnswer=r.next().getLiteral("sparql").toString();
        }
        obj.put("query", sparqlAnswer);
    	obj.put("answers", jsonAnswer);
    	
    	return obj.toString();  	
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