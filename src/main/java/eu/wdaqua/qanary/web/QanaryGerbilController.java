package eu.wdaqua.qanary.web;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletResponse;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;


/**
 * controller for processing questions, i.e., related to the question answering process
 *
 * @author Dennis Diefenbach
 */
@Controller
public class QanaryGerbilController {
	
    private static final Logger logger = LoggerFactory.getLogger(QanaryGerbilController.class);
    private QanaryConfigurator qanaryConfigurator;
 
     @Value("${server.host}")
     private String host;
     @Value("${server.port}")
     private String port;
 
    //Set this to allow browser requests from other websites
    @ModelAttribute
    public void setVaryResponseHeader(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
    }

    /**
     * inject QanaryConfigurator
     */
    @Autowired
    public QanaryGerbilController(final QanaryConfigurator qanaryConfigurator) {
        this.qanaryConfigurator = qanaryConfigurator;
    }
 
    @RequestMapping(value="/gerbil",  method = RequestMethod.POST, produces = "application/json")
	public ResponseEntity<?> gerbil(
			@RequestParam(value = "query", required = true) final String query) throws URISyntaxException {
    	logger.info("Asked question {}"+query);
    	MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("question", query);
        map.add("componentlist[]", "wdaqua-core0, QueryExecuter");
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(qanaryConfigurator.getHost()+":"+qanaryConfigurator.getPort()+"/startquestionansweringwithtextquestion", map, String.class);
        org.json.JSONObject json = new org.json.JSONObject(response);
        //retrive text representation, SPARQL and JSON result
        QanaryMessage myQanaryMessage = new QanaryMessage(new URI((String)json.get("endpoint")), new URI((String)json.get("inGraph")), new URI((String)json.get("outGraph")));
        QanaryQuestion myQanaryQuestion = new QanaryQuestion(myQanaryMessage);
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
        sparql.put("SPARQL", myQanaryQuestion.getSparqlResult());
    	language.add(sparql);
    	question.put("answers", myQanaryQuestion.getJsonResult());
    	question.put("language", language);
    	item.put("question", question);
    	questions.add(item);
    	obj.put("questions", questions);
    	return new ResponseEntity<org.json.simple.JSONObject>(obj,HttpStatus.OK);  	
	}
}
