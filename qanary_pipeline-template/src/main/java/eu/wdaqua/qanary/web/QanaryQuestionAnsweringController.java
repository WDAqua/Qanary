package eu.wdaqua.qanary.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.sparql.modify.GraphStoreBasic;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;


import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.message.QanaryComponentNotAvailableException;
import eu.wdaqua.qanary.message.QanaryExceptionQuestionNotProvided;
import eu.wdaqua.qanary.message.QanaryExceptionServiceCallNotOk;
import eu.wdaqua.qanary.message.QanaryMessage;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;
import eu.wdaqua.qanary.message.QanaryQuestionCreated;

/**
 * controller for processing questions, i.e., related to the question answering process
 *
 * @author AnBo
 */
@Controller
public class QanaryQuestionAnsweringController {

    // the string used for the endpoints w.r.t. the question answering process
    public static final String QUESTIONANSWERING = "/questionanswering";

    private static final Logger logger = LoggerFactory.getLogger(QanaryQuestionAnsweringController.class);

    private final QanaryConfigurator qanaryConfigurator;
    private final QanaryQuestionController qanaryQuestionController;
    
    @Value("${server.host}")
	private String host;
	@Value("${server.port}")
	private String port;

    /**
     * Jena model
     */
    private final GraphStore inMemoryStore;

    /**
     * inject QanaryConfigurator
     */
    @Autowired
    public QanaryQuestionAnsweringController(final QanaryConfigurator qanaryConfigurator,
                                             final QanaryQuestionController qanaryQuestionController) {
        this.qanaryConfigurator = qanaryConfigurator;
        this.qanaryQuestionController = qanaryQuestionController;

        inMemoryStore = new GraphStoreBasic(new DatasetImpl(ModelFactory.createDefaultModel()));
    }

    /**
     * a simple HTML input form for starting a question answering process with a QuestionURI
     */
    @RequestMapping(value = "/startquestionansweringwithtextquestion", method = RequestMethod.GET)
    public String startquestionansweringwithtextquestion() {
        return "startquestionansweringwithtextquestion";
    }

    /**
     * expose the model with the
     */
    @ModelAttribute("componentList")
    public List<String> componentList() {
        logger.info("available components: {}", qanaryConfigurator.getComponentNames());
        return qanaryConfigurator.getComponentNames();
    }

    /**
     * start a process directly with a textual question
     */
    @RequestMapping(value = "/startquestionansweringwithtextquestion", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> startquestionansweringwithtextquestion(
            @RequestParam(value = "question", required = true) final String question,
            @RequestParam(value = "componentlist[]") final List<String> componentsToBeCalled)
            throws URISyntaxException, QanaryComponentNotAvailableException, QanaryExceptionServiceCallNotOk,
            IOException, QanaryExceptionQuestionNotProvided {

        logger.info("startquestionansweringwithtextquestion: {} with {}", question, componentsToBeCalled);

        // you cannot pass without a question
        if (question.trim().isEmpty()) {
            throw new QanaryExceptionQuestionNotProvided();
        } else {
            QanaryQuestionCreated qanaryQuestionCreated = qanaryQuestionController.storeQuestion(question);
            return this.questionanswering(qanaryQuestionCreated.getQuestionURI().toURL(), componentsToBeCalled);
        }
    }

    @RequestMapping(value="/qa",  method = RequestMethod.GET)
    public String qa() {
        return "qa_input";
    }
    
    @RequestMapping(value="/qa",  method = RequestMethod.POST)
	public String qa(
			@RequestParam(value = "question", required = true) final String question,  Model model)
					throws URISyntaxException, ParseException, UnsupportedEncodingException {
    	logger.info("Asked question {}"+question);
    	model.addAttribute("question", question);
    	//Send the question to the startquestionansweringwithtextquestion interface, select as a component wdaqua-core0
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("question", question);
		map.add("componentlist[]", "wdaqua-core0");
		//map.add("componentlist[]", "Monolitic");
		RestTemplate restTemplate = new RestTemplate();
		String response = restTemplate.postForObject(host+":"+port+"/startquestionansweringwithtextquestion", map, String.class);
       		org.json.JSONObject json = new org.json.JSONObject(response);
		//TODO: replace previus line with QanaryQuestionAnsweringRun constructur
		//Retrive the answers as JSON object from the triplestore
    	String sparqlQuery =  "PREFIX qa: <http://www.wdaqua.eu/qa#> "
            + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
            + "SELECT ?json "
    		+ "FROM <"+  json.get("graph").toString() + "> "
    		+ "WHERE { "
    		+ "  ?a a qa:AnnotationOfAnswerJSON . "
            	+ "  ?a oa:hasBody ?json " 
    		+ "}";
        	ResultSet r = selectFromTripleStore(sparqlQuery, json.get("endpoint").toString());
        	//If there are answers give them back
        String jsonAnswer = "";
        if (r.hasNext()){
        		jsonAnswer=r.next().getLiteral("json").toString();
        		logger.info("JSONAnswer {}"+jsonAnswer);	
		}
        sparqlQuery =  "PREFIX qa: <http://www.wdaqua.eu/qa#> "
                + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
                + "SELECT ?sparql "
        		+ "FROM <"+  json.get("graph").toString() + "> "
        		+ "WHERE { "
        		+ "  ?a a qa:AnnotationOfAnswerSPARQL . "
                + "  ?a oa:hasBody ?json " 
        		+ "}";
        r = selectFromTripleStore(sparqlQuery, json.get("endpoint").toString());
        String sparqlAnswer="";
        if (r.hasNext()){
    		sparqlAnswer=r.next().getLiteral("json").toString();
    		logger.info("SPARQLAnswer {}"+jsonAnswer);	
        }
        if (jsonAnswer.equals("")==false && sparqlAnswer.equals("")==false ){
        	//Parse the json result set using jena
    		ResultSetFactory factory = new ResultSetFactory();
    		InputStream in = new ByteArrayInputStream(jsonAnswer.getBytes());
    		ResultSet result= factory.fromJSON(in);
    		List<String> var= result.getResultVars();
    		List<String> list = new ArrayList<>();
    		while (result.hasNext()){
        			for (String v:var){
        				list.add(result.next().get(v).toString());
        			}
    		}
			//Write the answers to the model such that it is available for the HTML template	
    		model.addAttribute("answers", list);
    		//Format the SPARQL query nicely
    		Query query = QueryFactory.create(sparqlAnswer);
        	query.setPrefix("dbr", "http://dbpedia.org/resource/");
        	query.setPrefix("dbp", "http://dbpedia.org/property/");
        	query.setPrefix("dbo", "http://dbpedia.org/ontology/");
        	String formatted=query.serialize();
        	formatted=formatted.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>");
        	model.addAttribute("sparqlQuery", formatted);
        	return "qa_output";
    	}
    	return "No answer";  	
	}
    
    
    
    /**
     * exposing the oa vocabulary
     */
    @RequestMapping(value = "/oa.owl", method = RequestMethod.GET, produces = "application/sparql-results+xml")
    @ResponseBody
    public ClassPathResource getFile1() {
        return new ClassPathResource("/oa.owl");
    }

    /**
     * exposing the qanary ontology
     */
    @RequestMapping(value = "/qanaryOntology.ttl", method = RequestMethod.GET, produces = "text/turtle")
    @ResponseBody
    public ClassPathResource getFile2() {
        return new ClassPathResource("/qanaryOntology.ttl");
    }

	/*
     * @ResponseBody public String plaintext(HttpServletResponse response) {
	 * response.setContentType("text/turtle");
	 * response.setCharacterEncoding("UTF-8"); return "qanaryOntology"; }
	 * 
	 */

    @RequestMapping(value = QUESTIONANSWERING + "/{runId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> questionanswering(@PathVariable(value = "runId") final UUID runId) throws Exception {
        throw new Exception("not yet implemented");
    }

    /**
     * start a configured process
     */
    @RequestMapping(value = QUESTIONANSWERING, method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> questionanswering(@RequestParam(value = "question", required = true) final URL questionUri,
                                               @RequestParam(value = "componentlist[]") final List<String> componentsToBeCalled)
            throws QanaryComponentNotAvailableException, URISyntaxException, QanaryExceptionServiceCallNotOk {
        logger.info("calling component: {} with question {}", componentsToBeCalled, questionUri);

        // Create a new named graph and insert it into the triplestore
        final UUID runID = UUID.randomUUID();
        URI namedGraph = new URI("urn:graph:" + runID.toString());

        URI endpoint = this.initGraphInTripelStore(namedGraph, questionUri);

        QanaryMessage myQanaryMessage = new QanaryMessage(endpoint, namedGraph);

        // execute synchronous calls to all components with the same message
        // TODO: execute asynchronously?
        qanaryConfigurator.callServicesByName(componentsToBeCalled, myQanaryMessage);

        QanaryQuestionAnsweringRun myRun = new QanaryQuestionAnsweringRun(runID, questionUri.toURI(), endpoint,
                namedGraph, qanaryConfigurator);
        return new ResponseEntity<QanaryQuestionAnsweringRun>(myRun, HttpStatus.OK);
    }

    /**
     * init the graph in the triplestore (c.f., applicationproperties)
     */
    private URI initGraphInTripelStore(URI namedGraph, final URL questionUri) {
        final URI triplestore = qanaryConfigurator.getEndpoint();
        logger.info("Triplestore " + triplestore);
        String sparqlquery = "";
        String namedGraphMarker = "<" + namedGraph.toString() + ">";

        // Load the Open Annotation Ontology
        sparqlquery = "LOAD <http://localhost:" + qanaryConfigurator.getPort() + "/oa.owl> INTO GRAPH "
                + namedGraphMarker;
        logger.info("Sparql query " + sparqlquery);
        loadTripleStore(sparqlquery, triplestore);

        // Load the Qanary Ontology
        sparqlquery = "LOAD <http://localhost:" + qanaryConfigurator.getPort() + "/qanaryOntology.ttl> INTO GRAPH "
                + namedGraphMarker;
        logger.info("Sparql query " + sparqlquery);
        loadTripleStore(sparqlquery, triplestore);

        // Prepare the question, answer and dataset objects
        sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
                + "INSERT DATA {GRAPH " + namedGraphMarker + " { <" + questionUri.toString() + "> a qa:Question}}";
        logger.info("Sparql query " + sparqlquery);
        loadTripleStore(sparqlquery, triplestore);

        sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#>" //
                + "INSERT DATA {GRAPH " + namedGraphMarker + " { " //
                + "<" + this.getQuestionAnsweringHostUrlString() + "/Answer> a qa:Answer}}";
        logger.info("Sparql query " + sparqlquery);
        loadTripleStore(sparqlquery, triplestore);

        sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#>" //
                + "INSERT DATA {GRAPH " + namedGraphMarker + " { " //
                + "  <" + qanaryConfigurator.getHost() + ":" + qanaryConfigurator.getPort() + "/Dataset> a qa:Dataset} " //
                + "}";
        logger.info("Sparql query " + sparqlquery);
        loadTripleStore(sparqlquery, triplestore);

        // Make the first two annotations
        sparqlquery = "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
                + "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
                + "INSERT DATA { " + "GRAPH " + namedGraphMarker + " { " //
                + "<anno1> a  oa:AnnotationOfQuestion; " //
                + "   oa:hasTarget <" + questionUri.toString() + "> ;" //
                + "   oa:hasBody   <URIAnswer>   . " //
                + "<anno2> a  oa:AnnotationOfQuestion; " //
                + "   oa:hasTarget <" + questionUri.toString() + "> ; " //
                + "   oa:hasBody   <URIDataset> " + "}}";
        logger.info("Sparql query " + sparqlquery);
        loadTripleStore(sparqlquery, triplestore);

        return triplestore;
    }

    /**
     * insert into local Jena triplestore
     *
     * TODO: needs to be extracted
     */
    public void insertSparqlIntoTriplestore(final String sparqlQuery) {
        final UpdateRequest request = UpdateFactory.create(sparqlQuery);
        final UpdateProcessor updateProcessor = UpdateExecutionFactory.create(request, inMemoryStore);
        updateProcessor.execute();
    }

    /**
     * executes a SPARQL INSERT into the triplestore
     *
     * @return map
     */
    private static void loadTripleStore(final String sparqlQuery, final URI endpoint) {
        final UpdateRequest request = UpdateFactory.create(sparqlQuery);
        final UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint.toString());
        proc.execute();
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

    /**
     * returns a valid URL (string) of configured properties
     */
    private String getQuestionAnsweringHostUrlString() {
        return this.qanaryConfigurator.getHost() + ":" + this.qanaryConfigurator.getPort() + "/";
    }
}
