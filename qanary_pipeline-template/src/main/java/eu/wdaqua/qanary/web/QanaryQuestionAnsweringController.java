package eu.wdaqua.qanary.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.sparql.modify.GraphStoreBasic;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;

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
                + "<anno1> a  qa:AnnotationOfQuestion; " //
                + "   oa:hasTarget <" + questionUri.toString() + "> ;" //
                + "   oa:hasBody   <URIAnswer>   . " //
                + "<anno2> a  qa:AnnotationOfQuestion; " //
                + "   oa:hasTarget <" + questionUri.toString() + "> ; " //
                + "   oa:hasBody   <URIDataset>  ."
                + "<anno3> a  qa:AnnotationOfQuestionLanguage; " //
                + "   oa:hasTarget <" + questionUri.toString() + "> ; " //
                + "   oa:hasBody   \"en\" }}";
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
     * returns a valid URL (string) of configured properties
     */
    private String getQuestionAnsweringHostUrlString() {
        return this.qanaryConfigurator.getHost() + ":" + this.qanaryConfigurator.getPort() + "/";
    }
}
