package eu.wdaqua.qanary.web;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletResponse;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;

import eu.wdaqua.qanary.QanaryComponentRegistrationChangeNotifier;
import eu.wdaqua.qanary.business.QanaryComponent;
import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.business.TriplestoreEndpointIdentifier;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryQuestionTextual;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.exceptions.QanaryExceptionServiceCallNotOk;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.exceptions.TripleStoreNotProvided;
import eu.wdaqua.qanary.message.QanaryComponentNotAvailableException;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;
import eu.wdaqua.qanary.message.QanaryQuestionCreated;
import eu.wdaqua.qanary.web.messages.RequestQuestionAnsweringProcess;
import io.swagger.v3.oas.annotations.Operation;
import eu.wdaqua.qanary.web.messages.AdditionalTriples;
import eu.wdaqua.qanary.web.messages.NumberOfAnnotationsResponse;
import eu.wdaqua.qanary.web.messages.AdditionalInsertQuery;

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
	public static final String QUESTIONANSWERINGFULL = "/questionansweringfull";
	private static final Logger logger = LoggerFactory.getLogger(QanaryQuestionAnsweringController.class);
	private final QanaryConfigurator qanaryConfigurator;
	private final QanaryQuestionController qanaryQuestionController;
	// TODO include QanaryPipelineConfigurationController
	private final QanaryComponentRegistrationChangeNotifier myComponentNotifier;
	private final QanaryPipelineConfiguration myQanaryPipelineConfiguration;
	private TriplestoreEndpointIdentifier myTriplestoreEndpointIdentifier;
	private final QanaryTripleStoreConnector myQanaryTripleStoreConnector;

	// Set this to allow browser requests from other websites
	@ModelAttribute
	public void setVaryResponseHeader(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
	}

	/**
	 * inject QanaryConfigurator
	 */
	@Autowired
	public QanaryQuestionAnsweringController( //
			final QanaryConfigurator qanaryConfigurator, //
			final QanaryQuestionController qanaryQuestionController, //
			final QanaryComponentRegistrationChangeNotifier myComponentNotifier, //
			final QanaryPipelineConfiguration myQanaryPipelineConfiguration, //
			final TriplestoreEndpointIdentifier myTriplestoreEndpointIdentifier, //
			final QanaryTripleStoreConnector myQanaryTripleStoreConnector //
	) {
		this.qanaryConfigurator = qanaryConfigurator;
		this.qanaryQuestionController = qanaryQuestionController;
		this.myComponentNotifier = myComponentNotifier;
		this.myQanaryPipelineConfiguration = myQanaryPipelineConfiguration;
		this.myTriplestoreEndpointIdentifier = myTriplestoreEndpointIdentifier;
		this.myQanaryTripleStoreConnector = myQanaryTripleStoreConnector;
	}

	/**
	 * expose the model with the component names
	 */
	@ModelAttribute("componentList")
	public List<String> componentList() {
		logger.info("available components: {}", myComponentNotifier.getAvailableComponentNames());
		return myComponentNotifier.getAvailableComponentNames();
	}

	/**
	 * expose the model with the Qanary triplestore endpoint
	 * 
	 * @throws URISyntaxException
	 * @throws TripleStoreNotProvided
	 */
	@ModelAttribute("triplestoreEndpointOfCurrentQanaryPipeline")
	public String triplestoreEndpointOfCurrentQanaryPipeline() throws TripleStoreNotProvided {
		try {
			String selectEndpoint = this.myTriplestoreEndpointIdentifier
					.getSelectEndpoint(this.myQanaryPipelineConfiguration.getTriplestoreAsURI()).toString();
			logger.info("set available endpoint in frontend to {}.", selectEndpoint);
			return selectEndpoint;
		} catch (Exception e) {
			throw new TripleStoreNotProvided(this.myQanaryPipelineConfiguration.getTriplestore());
		}
	}

	/**
	 * expose the model with the Qanary sparql query endpoint
	 */
	@ModelAttribute("sparqlEndpointOfCurrentQanaryPipeline")
	public String sparqlEndpointOfCurrentQanaryPipeline() {
		String baseUrlString = this.getQuestionAnsweringHostUrlString();
		String sparqlEndpoint = baseUrlString + QanarySparqlProtocolController.SPARQL_ENDPOINT;
		return sparqlEndpoint;
	}

	/**
	 * a simple HTML input form for starting a question answering process with a
	 * QuestionURI
	 */
	@RequestMapping(value = "/startquestionansweringwithtextquestion", method = RequestMethod.GET)
	@Operation(summary = "Return a simple HTML input form for starting a question answering process" //
	)
	public String startquestionansweringwithtextquestion() {
		return "startquestionansweringwithtextquestion";
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
	@Deprecated
	@RequestMapping(value = "/startquestionansweringwithtextquestion", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	@Operation(summary = "(deprecated) Start a process directly with a textual question", //
			operationId = "startquestionansweringwithtextquestion", //
			description = "Parameters are supplied as form entries. Only the question parameter is required. " //
					+ "Examples: \"What is the capital of Germany?\",  " //
					+ "\"How many people live in Madrid?\", " //
					+ "\"Person born in France\", " //
					+ "\"What is the name of the President of America?\", " //
					+ "\"Is Berlin the capital of Germany?\" " //
	)
	public ResponseEntity<?> startquestionansweringwithtextquestion(
			@RequestParam(value = QanaryStandardWebParameters.QUESTION, required = true) final String question,
			@RequestParam(value = QanaryStandardWebParameters.COMPONENTLIST, defaultValue = "") final List<String> componentsToBeCalled,
			@RequestParam(value = QanaryStandardWebParameters.LANGUAGE, defaultValue = "", required = false) final String language, //
			@RequestParam(value = QanaryStandardWebParameters.TARGETDATA, defaultValue = "", required = false) final List<String> targetdata, //
			@RequestParam(value = QanaryStandardWebParameters.PRIORCONVERSATION, defaultValue = "", required = false) final URI priorConversation, //
			// @RequestParam(value = QanaryStandardWebParameters.ADDITIONALQUERY,
			// defaultValue = "", required = false) final AdditionalInsertQuery
			// additionalQuery, // TODO: re-enable additional queries
			@RequestParam(value = QanaryStandardWebParameters.ADDITIONALTRIPLES, defaultValue = "", required = false) final AdditionalTriples additionalTriples //
	) throws Exception {

		logger.info("startquestionansweringwithtextquestion: {} with {}", question, componentsToBeCalled);
		logger.warn(
				"mapping \"/startquestionansweringwithtextquestion\" for form-based requests is DEPRECATED in favour of requests using JSON");
		QanaryQuestionAnsweringRun myRun = this.createOrUpdateAndRunQuestionAnsweringSystemHelper(null, question, null,
				componentsToBeCalled, language, targetdata, priorConversation, additionalTriples);

		return new ResponseEntity<QanaryQuestionAnsweringRun>(myRun, HttpStatus.CREATED);
	}

	@PostMapping(value = "/startquestionansweringwithtextquestion", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Start a process directly with a textual question", //
			operationId = "startquestionansweringwithtextquestion", //
			description = "Parameters are supplied via JSON. Only the question parameter is required. " //
					+ "Examples: {\"question\": \"What is the capital of Germany?\"}, " //
					+ "{\"question\": \"Person born in France?\"}" //
	)
	public ResponseEntity<?> startquestionansweringwithtextquestion(
			@RequestBody RequestQuestionAnsweringProcess myRequestQuestionAnsweringProcess) throws Exception {
		logger.info("startquestionansweringwithtextquestion: {}", myRequestQuestionAnsweringProcess);
		QanaryQuestionAnsweringRun myRun = this
				.createOrUpdateAndRunQuestionAnsweringSystemHelper(myRequestQuestionAnsweringProcess);
		return new ResponseEntity<QanaryQuestionAnsweringRun>(myRun, HttpStatus.CREATED);
	}

	/**
	 * a simple HTML input form for starting a question answering process with an
	 * audio question
	 */
	@RequestMapping(value = "/startquestionansweringwithaudioquestion", method = RequestMethod.GET)
	@Operation(summary = "Return a simple HTML form for starting a question answering process with an audio question")
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
	@Operation(summary = "Start a process directly with an audio question", //
			operationId = "startquestionansweringwithaudioquestion", //
			description = "Only the audio question is required. " //
					+ "Examples: t.b.a" //
	)
	// TODO: How would I give examples here? with path to audio file?
	public ResponseEntity<?> startquestionansweringwithaudioquestion(
			@RequestParam(value = QanaryStandardWebParameters.QUESTION, required = true) final MultipartFile question,
			@RequestParam(value = QanaryStandardWebParameters.COMPONENTLIST, defaultValue = "") final List<String> componentsToBeCalled, //
			@RequestParam(value = QanaryStandardWebParameters.LANGUAGE, defaultValue = "", required = false) final String language, //
			@RequestParam(value = QanaryStandardWebParameters.TARGETDATA, defaultValue = "", required = false) final List<String> targetdata, //
			@RequestParam(value = QanaryStandardWebParameters.PRIORCONVERSATION, defaultValue = "", required = false) final URI priorConversation//
	) throws Exception {

		logger.info("startquestionansweringwithaudioquestion: {} with {}", question, componentsToBeCalled);
		QanaryQuestionAnsweringRun myRun = this.createOrUpdateAndRunQuestionAnsweringSystemHelper(null, null, question,
				componentsToBeCalled, language, targetdata, priorConversation, null);

		return new ResponseEntity<QanaryQuestionAnsweringRun>(myRun, HttpStatus.CREATED);
	}

	/**
	 * exposing the oa commons
	 */
	@RequestMapping(value = "/oa.owl", method = RequestMethod.GET, produces = "application/sparql-results+xml")
	@ResponseBody
	@Operation(summary = "Expose the OA commons", //
			operationId = "getFile1", // TODO: shouldn't this be a more telling ID?
			description = "View or download the Open Annotation Data Model." //
	)
	public ClassPathResource getFile1() {
		return new ClassPathResource("/oa.owl");
	}

	/**
	 * exposing the Qanary ontology
	 */
	@RequestMapping(value = "/qanaryOntology.ttl", method = RequestMethod.GET, produces = "text/turtle")
	@ResponseBody
	@Operation(summary = "Expose the Qanary ontology", //
			operationId = "getFile2", //
			description = "View or download the Qanary ontology." //
	)
	public ClassPathResource getFile2() {
		return new ClassPathResource("/qanaryOntology.ttl");
	}

	/**
	 * exposing additional triples
	 */
	@RequestMapping(value = "/additional-triples/{id}", method = RequestMethod.GET, produces = "text/turtle")
	@ResponseBody
	@Operation(summary = "Expose additonal Triples", //
			operationId = "getAdditionalTriples", //
			description = "View additional triples that were passed and stored when starting the " //
					+ "question answering process. Requires a valid ID.")
	public InputStreamResource getAdditionalTriples(@PathVariable final String id) throws FileNotFoundException {
		String filename = Paths.get(myQanaryPipelineConfiguration.getAdditionalTriplesDirectory(), id + ".ttl")
				.toString();
		InputStream in = new FileInputStream(filename);
		return new InputStreamResource(in);
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
	@Operation(summary = "Return information about a specific question answering run", //
			operationId = "getQuestionAnsweringGraphInformation", //
			description = "(not yet implemented) The run is identified by the provided runId" // TODO: udpate
																								// description once
																								// fully implemented
	)
	public ResponseEntity<?> getQuestionAnsweringGraphInformation(@PathVariable(value = "runId") final UUID runId)
			throws Exception {
		throw new Exception("not yet implemented");
	}

	/**
	 * returns information about the run identified by the provided runId
	 * 
	 * @param runId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = QUESTIONANSWERING
			+ "/{runId}", method = RequestMethod.DELETE, produces = "application/json")
	@ResponseBody
	@Operation(summary = "Delete information about a specific question answering run", //
			operationId = "deleteQuestionAnsweringGraph", //
			description = "(not yet implemented) The run is identified by the provided runId" // TODO: udpate
																								// description once
																								// fully implemented
	)
	public ResponseEntity<?> deleteQuestionAnsweringGraph(@PathVariable(value = "runId") final UUID runId)
			throws Exception {
		throw new Exception("not yet implemented");
	}

	/**
	 * returns information about the run identified by the provided runId
	 * 
	 * @param runId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = QUESTIONANSWERING + "/{runId}", method = RequestMethod.PUT, produces = "application/json")
	@ResponseBody
	@Operation(summary = "Update information about a specific question answering run", //
			operationId = "createOrUpdateQuestionAnsweringGraph", //
			description = "(not yet implemented) The run is identified by the provided runId" // TODO: udpate
																								// description once
																								// fully implemented
	)
	public ResponseEntity<?> createOrUpdateQuestionAnsweringGraph(@PathVariable(value = "runId") final UUID runId)
			throws Exception {
		throw new Exception("not yet implemented");
	}

	/**
	 * create a new Question Answering process
	 * 
	 * @param textquestion
	 * @param audioquestion
	 * @param componentsToBeCalled
	 * @param language
	 * @param targetdata
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = QUESTIONANSWERING, method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	@Operation(summary = "Create a new Question Answering Process", //
			operationId = "createQuestionAnswering", //
			description = "No parameters are required." // TODO: extend
	)
	public ResponseEntity<?> createQuestionAnswering( //
			@RequestParam(value = QanaryStandardWebParameters.TEXTQUESTION, defaultValue = "", required = false) final String textquestion, //
			@RequestParam(value = QanaryStandardWebParameters.AUDIOQUESTION, required = false) final MultipartFile audioquestion, //
			@RequestParam(value = QanaryStandardWebParameters.GRAPH, defaultValue = "", required = false) final URI graph, //
			@RequestParam(value = QanaryStandardWebParameters.COMPONENTLIST, defaultValue = "", required = false) final List<String> componentsToBeCalled, //
			@RequestParam(value = QanaryStandardWebParameters.LANGUAGE, defaultValue = "", required = false) final String language, //
			@RequestParam(value = QanaryStandardWebParameters.TARGETDATA, defaultValue = "", required = false) final List<String> targetdata, //
			@RequestParam(value = QanaryStandardWebParameters.PRIORCONVERSATION, defaultValue = "", required = false) final URI priorConversation//
	) throws Exception {

		// create a new question answering system
		logger.info(
				"create and start a new question answering system for question={}, componentlist={}, language={}, targetdata={}",
				textquestion, componentsToBeCalled, language, targetdata);

		QanaryQuestionAnsweringRun myRun = this.createOrUpdateAndRunQuestionAnsweringSystemHelper(graph, textquestion,
				audioquestion, componentsToBeCalled, language, targetdata, priorConversation, null);

		return new ResponseEntity<QanaryQuestionAnsweringRun>(myRun, HttpStatus.CREATED);
	}

	/**
	 * create a new Question Answering process
	 *
	 * @param textquestion
	 * @param audioquestion
	 * @param componentsToBeCalled
	 * @param language
	 * @param targetdata
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = QUESTIONANSWERINGFULL, method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	@Operation(summary = "Create a new Question Answering Process", //
			operationId = "createQuestionAnsweringFull", //
			description = "No parameters are required." // TODO: how does this differ from /questionanswering
	)
	public ResponseEntity<?> createQuestionAnsweringFull( //
			@RequestParam(value = QanaryStandardWebParameters.TEXTQUESTION, defaultValue = "", required = false) final String textquestion, //
			@RequestParam(value = QanaryStandardWebParameters.AUDIOQUESTION, required = false) final MultipartFile audioquestion, //
			@RequestParam(value = QanaryStandardWebParameters.GRAPH, defaultValue = "", required = false) final URI graph, //
			@RequestParam(value = QanaryStandardWebParameters.COMPONENTLIST, defaultValue = "", required = false) final List<String> componentsToBeCalled, //
			@RequestParam(value = QanaryStandardWebParameters.LANGUAGE, defaultValue = "", required = false) final String language, //
			@RequestParam(value = QanaryStandardWebParameters.TARGETDATA, defaultValue = "", required = false) final List<String> targetdata, //
			@RequestParam(value = QanaryStandardWebParameters.PRIORCONVERSATION, defaultValue = "", required = false) final URI priorConversation//
	) throws Exception {

		// create a new question answering system
		logger.info(
				"create and start a new question answering system for question={}, componentlist={}, language={}, targetdata={}",
				textquestion, componentsToBeCalled, language, targetdata);

		QanaryQuestionAnsweringRun myRun = this.createOrUpdateAndRunQuestionAnsweringSystemHelper(graph, textquestion,
				audioquestion, componentsToBeCalled, language, targetdata, priorConversation, null);
		// retrieve text representation, SPARQL and JSON result
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<String>(myRun.getInGraph(), qanaryConfigurator);

		JSONObject obj = new JSONObject();
		obj.put("endpoint", myRun.getEndpoint());
		obj.put("namedgraph", myRun.getInGraph());
		obj.put("textrepresentation", myQanaryQuestion.getTextualRepresentation());
		obj.put("answer_found", myQanaryQuestion.getAnswerFound());
		JSONArray sparql = new JSONArray();
		List<QanaryQuestion<String>.SparqlAnnotation> ss = myQanaryQuestion.getSparqlResults();
		for (@SuppressWarnings("rawtypes")
		QanaryQuestion.SparqlAnnotation s : ss) {
			logger.debug("createQuestionAnsweringFull: raw result: {}", s);
			JSONObject o = new JSONObject();
			o.put("query", s.query);
			o.put("confidence", s.confidence);
			o.put("kb", s.knowledgegraphEndpoint);
			sparql.put(o);
		}
		obj.put("sparql", sparql);
		obj.put("json", myQanaryQuestion.getJsonResult());

		return new ResponseEntity<JSONObject>(obj, HttpStatus.OK);
	}

	/**
	 * helper for creating or updating a Question Answering system (used for POST
	 * and PUT requests) for a given textual question
	 * 
	 * @param graph
	 * @param question
	 * @param componentsToBeCalled
	 * @param language
	 * @param targetdata
	 * @return
	 * @throws Exception
	 */
	private QanaryQuestionAnsweringRun createOrUpdateAndRunQuestionAnsweringSystemHelper(URI graph, String question,
			MultipartFile questionaudio, List<String> componentsToBeCalled, String language, List<String> targetdata,
			URI priorConversation, AdditionalTriples additionalTriples) throws Exception {

		// create a QanaryQuestion from given question and graph
		logger.info("createOrUpdateAndRunQuestionAnsweringSystemHelper: \"{}\" with {} (priorConversation: {})", //
				question, componentsToBeCalled, priorConversation);

		// no question string was given, so it is tried to fetch it from the
		// triplestore
		QanaryQuestion<?> qanaryQuestion;
		if ((question == null || question.trim().isEmpty()) && questionaudio == null) {
			if (graph == null) {
				throw new Exception(
						"graph URI was not provided to retrieve information about the question (which also was not provided).");
			} else {
				logger.info("question was empty, data is retrieved from graph \"{}\"", graph);
				qanaryQuestion = new QanaryQuestion<String>(graph, qanaryConfigurator);
			}
		} else {
			// TODO: check control flow of next ITE structure
			if (questionaudio == null) {
				// store the question on the current server
				QanaryQuestionCreated qanaryQuestionCreated = qanaryQuestionController.storeQuestion(question);

				// create a textual question in a new graph
				qanaryQuestion = new QanaryQuestionTextual(qanaryQuestionCreated.getQuestionURI().toURL(),
						qanaryConfigurator, priorConversation);
			} else {
				// store the question on the current server
				QanaryQuestionCreated qanaryQuestionCreated = qanaryQuestionController
						.storeAudioQuestion(questionaudio);

				// create question
				qanaryQuestion = new QanaryQuestion(qanaryQuestionCreated.getQuestionURI().toURL(), qanaryConfigurator,
						priorConversation);

				// add annotation saying that it is an audio question
				qanaryQuestion.putAnnotationOfAudioRepresentation();

			}

		}
		// store language definition for current question
		if (language != null && language.isEmpty() == false) {
			qanaryQuestion.setLanguageText(language);
		} else {
			logger.info("no language was given, no change for question \"{}\"", question);
		}

		// store targetdata for the current question
		if (targetdata != null && targetdata.isEmpty() == false) {
			qanaryQuestion.setTargetData(targetdata);
		} else {
			logger.info("no targetdata was given, no change for question \"{}\"", question);
		}

		QanaryMessage myQanaryMessage = new QanaryMessage(this.qanaryConfigurator.getEndpoint(),
				qanaryQuestion.getNamedGraph());

		logger.info("calling components \"{}\" on named graph \"{}\" and endpoint \"{}\"", componentsToBeCalled,
				myQanaryMessage.getInGraph(), myQanaryMessage.getEndpoint());

//		TODO: re-enable additional insert queries 
//		if (myQanaryPipelineConfiguration.getInsertQueriesAllowed() && additionalQuery != null) {
//			if (additionalQuery.getInsertQuery() != null) loadAdditionalQuery(additionalQuery, myQanaryMessage);
//		}

		if (myQanaryPipelineConfiguration.getAdditionalTriplesAllowed() && additionalTriples != null) {
			if (additionalTriples.getUriFilePath() != null)
				loadAdditionalTriples(additionalTriples, myQanaryMessage);
		}

		QanaryQuestionAnsweringRun myRun = this.executeComponentList(qanaryQuestion.getUri(), componentsToBeCalled,
				myQanaryMessage);
		return myRun;
	}

	private void loadAdditionalTriples(AdditionalTriples additionalTriples, QanaryMessage myQanaryMessage)
			throws TripleStoreNotProvided, URISyntaxException, SparqlQueryFailed {
		String resource = myQanaryPipelineConfiguration.getHost() + ":" + myQanaryPipelineConfiguration.getPort()
				+ "/additional-triples/" + additionalTriples.getUUIDString();
		String sparqlquery = "" //
				+ "LOAD <" + resource + "> " //
				+ "INTO GRAPH <" + myQanaryMessage.getInGraph().toString() + ">";
		logger.info("load additional triples with SPARQL query: {}", sparqlquery);
		qanaryConfigurator.getQanaryTripleStoreConnector().update(sparqlquery);
	}

	/**
	 * create and execute a SPARQL insert query to load additional triples into the
	 * triplestore before processing the question.
	 *
	 * @param additionalPrefixes
	 * @param additionalTriples
	 * @param myQanaryMessage
	 */
//	TODO: re-enable additional insert queries 
//	private void loadAdditionalQuery(AdditionalInsertQuery additionalQuery, QanaryMessage myQanaryMessage) throws TripleStoreNotProvided, URISyntaxException, SparqlQueryFailed {
//		String sparqlInsert = additionalQuery.getInsertQuery();
//
//		logger.info("loading additional triples into graph \"{}\" with query: {}\n", myQanaryMessage.getEndpoint(), sparqlInsert);
//
//		QanaryUtils qanaryUtils = new QanaryUtils(myQanaryMessage);
//		qanaryConfigurator.getQanaryTripleStoreConnector().update(sparqlquery);
//	}

	/**
	 * wrapper: create new Question Answering process for a given textual question
	 * 
	 * @param myRequestQuestionAnsweringProcess
	 * @return
	 * @throws Exception
	 */
	private QanaryQuestionAnsweringRun createOrUpdateAndRunQuestionAnsweringSystemHelper(
			RequestQuestionAnsweringProcess myRequestQuestionAnsweringProcess) throws Exception {
		URI newGraph = null;
		return this.createOrUpdateAndRunQuestionAnsweringSystemHelper(newGraph, //
				myRequestQuestionAnsweringProcess.getQuestion(), //
				null, //
				myRequestQuestionAnsweringProcess.getcomponentlist(), //
				myRequestQuestionAnsweringProcess.getLanguage(), //
				myRequestQuestionAnsweringProcess.getTargetdata(), //
				myRequestQuestionAnsweringProcess.getPriorConversation(), null // addtional triples
		);
	}

	/**
	 * execute the components as defined by the parameters
	 * 
	 * @param question
	 * @param componentsToBeCalled
	 * @param myQanaryMessage
	 * @return
	 * @throws URISyntaxException
	 * @throws QanaryComponentNotAvailableException
	 * @throws QanaryExceptionServiceCallNotOk
	 */
	private QanaryQuestionAnsweringRun executeComponentList(URI question, List<String> componentsToBeCalled,
			QanaryMessage myQanaryMessage)
			throws URISyntaxException, QanaryComponentNotAvailableException, QanaryExceptionServiceCallNotOk {
		logger.info("executeComponentList on \"{}\": {}", question, componentsToBeCalled);
		// execute synchronous calls to all components with the same message
		// if no component is passed nothing is happening
		if (componentsToBeCalled.isEmpty() == false) {
			List<QanaryComponent> components = this.myComponentNotifier
					.getAvailableComponentsFromNames(componentsToBeCalled);
			qanaryConfigurator.callServices(components, myQanaryMessage);
		} else {
			logger.warn("Executing components is not done, as the componentlist parameter was empty.");
		}

		QanaryQuestionAnsweringRun myRun = new QanaryQuestionAnsweringRun(question, myQanaryMessage.getEndpoint(),
				myQanaryMessage.getInGraph(), myQanaryMessage.getOutGraph(), qanaryConfigurator);

		return myRun;
	}

	/**
	 * get the number of annotations created by a component
	 * 
	 * @throws SparqlQueryFailed
	 */
	@RequestMapping(value = "/numberOfAnnotations/", method = RequestMethod.GET, produces = "application/json")
	@Operation(summary = "Get the number of annotations created by a component", //
			operationId = "getNumberOfAnnotationsForComponent", //
			description = "Filter all annotations created for the current question answering run "
					+ "to find how many were created by a specific component. "
					+ "Requires the correct component name (case sensitive) and the graph.")
	public ResponseEntity<NumberOfAnnotationsResponse> getNumberOfAnnotationsForComponent( //
			@RequestParam String component, //
			@RequestParam String graph //
	) throws URISyntaxException, JSONException, SparqlQueryFailed {
		// TODO: a test is needed
		String sparqlGet = "" //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/>" //
				+ "SELECT " //
				+ "		(COUNT(*) AS ?NumberOfAnnotations)" //
				+ "		(SAMPLE(STR(?componentname)) AS ?ComponentName)" //
				+ "FROM <" + graph + ">" //
				+ "WHERE {    " //
				+ "		?s oa:annotatedBy ?componentname .    " //
				+ "		FILTER REGEX (STR(?componentname), \".*:" + component + "$\") . " //
				+ "}";

		logger.info("fetching number of annotations with query: {}", sparqlGet);

		ResultSet annotations = myQanaryTripleStoreConnector.select(sparqlGet);
		QuerySolution annotation = annotations.next();

		int annotationCount = 0;
		String componentUrl = null;

		try {
			annotationCount = annotation.get("NumberOfAnnotations").asLiteral().getInt();
			componentUrl = annotation.get("ComponentName").asLiteral().getString();
			logger.info("found {} annotations for component {} on graph {}", annotationCount, component, graph);
		} catch (NullPointerException nullPointer) {
			logger.info("No annotations were found for component {} on graph {}", component, graph);
		}

		return new ResponseEntity<>(new NumberOfAnnotationsResponse(componentUrl, annotationCount, graph, sparqlGet),
				HttpStatus.OK);
	}

	/**
	 * start a configured process
	 * 
	 * @param componentsToBeCalled
	 * @param jsonMessage
	 * @return
	 * @throws Exception
	 * 
	 */
	@Deprecated
	public ResponseEntity<?> questionansweringLegacy(final List<String> componentsToBeCalled, //
			// expected is a JSON message contains ingraph, outgraph, endpoint
			String jsonMessage) throws Exception {
		logger.warn("used deprecated method: questionansweringLegacy");
		QanaryMessage myQanaryMessage = new QanaryMessage(jsonMessage);
		QanaryQuestion<?> myQanaryQuestion = new QanaryQuestion(myQanaryMessage, this.qanaryConfigurator);
		URI question = myQanaryQuestion.getUri();

		logger.info("calling components \"{}\" on named graph \"{}\" and endpoint \"{}\"", componentsToBeCalled,
				myQanaryMessage.getEndpoint(), myQanaryMessage.getInGraph());

		QanaryQuestionAnsweringRun myRun = this.executeComponentList(question, componentsToBeCalled, myQanaryMessage);

		return new ResponseEntity<QanaryQuestionAnsweringRun>(myRun, HttpStatus.OK);
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
