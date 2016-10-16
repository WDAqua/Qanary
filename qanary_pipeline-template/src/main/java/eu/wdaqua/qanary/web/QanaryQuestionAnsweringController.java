package eu.wdaqua.qanary.web;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import com.hp.hpl.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.sparql.modify.GraphStoreBasic;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.business.QanaryQuestion;
import eu.wdaqua.qanary.business.QanaryQuestionWithAudio;
import eu.wdaqua.qanary.business.QanaryQuestionWithText;
import eu.wdaqua.qanary.message.QanaryComponentNotAvailableException;
import eu.wdaqua.qanary.message.QanaryExceptionQuestionNotProvided;
import eu.wdaqua.qanary.message.QanaryExceptionServiceCallNotOk;
import eu.wdaqua.qanary.message.QanaryMessage;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;
import eu.wdaqua.qanary.message.QanaryQuestionCreated;

/**
 * controller for processing questions, i.e., related to the question answering
 * process
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
	 * a simple HTML input form for starting a question answering process with a
	 * QuestionURI
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
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@RequestMapping(value = "/startquestionansweringwithtextquestion", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> startquestionansweringwithtextquestion(
			@RequestParam(value = "question", required = true) final String question,
			@RequestParam(value = "componentlist[]") final List<String> componentsToBeCalled)
					throws URISyntaxException, QanaryComponentNotAvailableException, QanaryExceptionServiceCallNotOk,
					IOException, QanaryExceptionQuestionNotProvided, InstantiationException, IllegalAccessException,
					IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		logger.info("startquestionansweringwithtextquestion: {} with {}", question, componentsToBeCalled);

		// you cannot pass without a question
		if (question.trim().isEmpty()) {
			throw new QanaryExceptionQuestionNotProvided();
		} else {
			QanaryQuestionCreated qanaryQuestionCreated = qanaryQuestionController.storeQuestion(question);
			QanaryMessage myQanaryMessage = createQuestionInTriplestore(qanaryQuestionCreated,
					QanaryQuestionWithText.class);
			return this.questionanswering(componentsToBeCalled, myQanaryMessage.asJsonString());
		}
	}

	/**
	 * a simple HTML input form for starting a question answering process with a
	 * audio question
	 */
	@RequestMapping(value = "/startquestionansweringwithaudioquestion", method = RequestMethod.GET)
	public String startquestionansweringwithaudioquestion() {
		return "startquestionansweringwithaudioquestion";
	}

	/**
	 * start a process directly with an audio question
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@RequestMapping(value = "/startquestionansweringwithaudioquestion", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> startquestionansweringwithaudioquestion(
			@RequestParam(value = "question", required = true) final MultipartFile question,
			@RequestParam(value = "componentlist[]") final List<String> componentsToBeCalled)
					throws URISyntaxException, QanaryComponentNotAvailableException, QanaryExceptionServiceCallNotOk,
					IOException, QanaryExceptionQuestionNotProvided, InstantiationException, IllegalAccessException,
					IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		logger.info("startquestionansweringwithtextquestion: {} with {}", question, componentsToBeCalled);

		// you cannot pass without a valid question
		if (question.isEmpty()) {
			throw new QanaryExceptionQuestionNotProvided();
		} else {
			QanaryQuestionCreated qanaryQuestionCreated = qanaryQuestionController.storeAudioQuestion(question);
			QanaryMessage myQanaryMessage = createQuestionInTriplestore(qanaryQuestionCreated,
					QanaryQuestionWithAudio.class);
			return this.questionanswering(componentsToBeCalled, myQanaryMessage.asJsonString());
		}
	}

	/**
	 * creates question in triplestore, including the correct annotation
	 * depending on the passed class (needs to be a concrete implementation of
	 * {@link QanaryQuestion}
	 * 
	 * @param qanaryQuestionCreated
	 * @param myQanaryQuestionClass
	 * @return
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	private QanaryMessage createQuestionInTriplestore(QanaryQuestionCreated qanaryQuestionCreated,
			Class<? extends QanaryQuestion> myQanaryQuestionClass)
					throws URISyntaxException, MalformedURLException, InstantiationException, IllegalAccessException,
					IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		QanaryMessage myQanaryMessage = initGraphInTripelStore(qanaryQuestionCreated.getQuestionURI().toURL());

		// The question has a specific representation, therefore an anonymous
		// type is used here, prepare anonymous constructor for {@link
		// QanaryQuestion} class
		Class[] argTypes = new Class[4];
		argTypes[0] = URI.class;
		argTypes[1] = URI.class;
		argTypes[2] = URI.class;
		argTypes[3] = this.getClass();
		myQanaryQuestionClass.getDeclaredConstructor(argTypes).newInstance(myQanaryMessage.getEndpoint(),
				myQanaryMessage.getInGraph(), qanaryQuestionCreated.getQuestionURI(), this);

		return myQanaryMessage;
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
	 * exposing the Qanary ontology
	 */
	@RequestMapping(value = "/qanaryOntology.ttl", method = RequestMethod.GET, produces = "text/turtle")
	@ResponseBody
	public ClassPathResource getFile2() {
		return new ClassPathResource("/qanaryOntology.ttl");
	}

	/**
	 * returns information about the run identified by the provided runId
	 * 
	 * @param runId
	 * @return
	 * @throws Exception
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
	public ResponseEntity<?> questionanswering(
			@RequestParam(value = "componentlist[]") final List<String> componentsToBeCalled,
			@RequestBody String jsonMessage // expected is a JSON message that
										// contains ingraph, outgraph, endpoint
	) throws QanaryComponentNotAvailableException, URISyntaxException, QanaryExceptionServiceCallNotOk {

		QanaryMessage myQanaryMessage = new QanaryMessage(jsonMessage);
		// TODO: substitute with eu.wdaqua.qanary.component.QanaryMessage method when shared
        String query = "SELECT ?question " +
                "FROM <" + myQanaryMessage.getInGraph() + "> {" +
                "?question <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.wdaqua.eu/qa#Question>" +
                "}";
        ResultSet r = selectFromTripleStore(query, myQanaryMessage.getEndpoint());
		URI question = new URI(r.next().getResource("question").toString());


		final UUID runID = UUID.randomUUID();
		logger.info("calling component: {} on named graph {} and endpoint {} ", componentsToBeCalled,
				myQanaryMessage.getEndpoint(), myQanaryMessage.getInGraph());

		// execute synchronous calls to all components with the same message
		// TODO: execute asynchronously?
		qanaryConfigurator.callServicesByName(componentsToBeCalled, myQanaryMessage);

		QanaryQuestionAnsweringRun myRun = new QanaryQuestionAnsweringRun(runID, question,
				myQanaryMessage.getEndpoint(), myQanaryMessage.getInGraph(), qanaryConfigurator);
		return new ResponseEntity<QanaryQuestionAnsweringRun>(myRun, HttpStatus.OK);
	}

	/**
	 * init the graph in the triplestore (c.f., applicationproperties)
	 * 
	 * TODO: needs to be extracted
	 */
	private QanaryMessage initGraphInTripelStore(final URL questionUri) throws URISyntaxException {
		// Create a new named graph and insert it into the triplestore
		URI namedGraph = new URI("urn:graph:" + UUID.randomUUID().toString());

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
		return new QanaryMessage(triplestore, namedGraph);
	}

    /**
     * query a SPARQL endpoint with a given query
     */
    public ResultSet selectFromTripleStore(String sparqlQuery, URI endpoint) {
        logger.debug("selectTripleStore on {} execute {}", endpoint, sparqlQuery);
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint.toString(), query);
        ResultSet resultset = qExe.execSelect();
        return resultset;
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
	 * TODO: needs to be extracted
	 *
	 * @return map
	 */
	public static void loadTripleStore(final String sparqlQuery, final URI endpoint) {
		final UpdateRequest request = UpdateFactory.create(sparqlQuery);
		final UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint.toString());
		proc.execute();
	}

	/**
	 * returns a valid URL (string) of configured properties
	 * 
	 * TODO: needs to be extracted
	 */
	private String getQuestionAnsweringHostUrlString() {
		return this.qanaryConfigurator.getHost() + ":" + this.qanaryConfigurator.getPort() + "/";
	}
}
