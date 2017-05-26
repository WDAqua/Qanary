package eu.wdaqua.qanary.web;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
    private final QanaryConfigurator qanaryConfigurator;
    private final QanaryQuestionController qanaryQuestionController;
 
     @Value("${server.host}")
     private String host;
     @Value("${server.port}")
     private int port;
 
    //Set this to allow browser requests from other websites
    @ModelAttribute
    public void setVaryResponseHeader(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
    }

    /**
     * inject QanaryConfigurator
     */
    @Autowired
    public QanaryGerbilController(final QanaryConfigurator qanaryConfigurator,
                                  final QanaryQuestionController qanaryQuestionController) {
        this.qanaryConfigurator = qanaryConfigurator;
        this.qanaryQuestionController = qanaryQuestionController;
    }

    /**
     * expose the model with the component names
     */
    @ModelAttribute("componentList")
    public List<String> componentList() {
        logger.info("available components: {}", qanaryConfigurator.getComponentNames());
        return qanaryConfigurator.getComponentNames();
    }

    /**
     * a simple HTML input to generate a url-endpoint for gerbil for QA, http://gerbil-qa.aksw.org/gerbil/config
     */
    @RequestMapping(value = "/gerbil", method = RequestMethod.GET)
    public String startquestionansweringwithtextquestion(Model model) {
        model.addAttribute("url", "Select components!");
        return "generategerbilendpoint";
    }

    /**
     * given a list of components a url-endpoint for gerbil for QA is generated
     *
     */
    @RequestMapping(value = "/gerbil", method = RequestMethod.POST)
    public String gerbilGenerator(
            @RequestParam(value = QanaryStandardWebParameters.COMPONENTLIST, defaultValue = "") final List<String> componentsToBeCalled,
            Model model
    ) throws Exception {
        String urlStr = "";
        if (componentsToBeCalled.size()==0){
            urlStr = "Select components!";
            model.addAttribute("url", urlStr);
        } else {
            //Generate a string like this "wdaqua-core0, QueryExecuter"
            String components = "/gerbil-execute/";
            for (String component : componentsToBeCalled) {
                components += component + ", ";
            }
            System.out.println(components);
            if (components.length() > 0) {
                components = components.substring(0, components.length() - 2);
            }
            System.out.println(components);
            //urlStr += URLEncoder.encode(components, "UTF-8")+"/";
            URI uri = new URI(
                    "http",
                    null,
                    new URL(host).getHost(),
                    port,
                    components+"/",
                    null,
                    null);
            URL url = uri.toURL();
            System.out.println(url.toString());
            model.addAttribute("url", url.toString());
        }
        return "generategerbilendpoint";
    }

    @RequestMapping(value="/gerbil-execute/{components:.*}",  method = RequestMethod.POST, produces = "application/json")
	public ResponseEntity<?> gerbil(
			@RequestParam(value = "query", required = true) final String query,
            @PathVariable("components") final String componentsToBeCalled
    ) throws URISyntaxException {
    	logger.info("Asked question: {}", query);
        logger.info("QA pipeline components: {}", componentsToBeCalled);
    	MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("question", query);
        map.add("componentlist[]", componentsToBeCalled);
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
