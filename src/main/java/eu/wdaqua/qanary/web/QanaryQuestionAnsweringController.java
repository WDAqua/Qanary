package eu.wdaqua.qanary.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;

import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletResponse;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.message.QanaryComponentNotAvailableException;
import eu.wdaqua.qanary.message.QanaryExceptionQuestionNotProvided;
import eu.wdaqua.qanary.message.QanaryExceptionServiceCallNotOk;
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
     * inject QanaryConfigurator
     */
    @Autowired
    public QanaryQuestionAnsweringController(final QanaryConfigurator qanaryConfigurator,
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
	 * a simple HTML input form for starting a question answering process with a
	 * QuestionURI
	 */
	@RequestMapping(value = "/startquestionansweringwithtextquestion", method = RequestMethod.GET)
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
	@RequestMapping(value = "/startquestionansweringwithtextquestion", method = RequestMethod.POST, produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> startquestionansweringwithtextquestion(
			@RequestParam(value = "question", required = true) final String question,
			@RequestParam(value = "componentlist[]", defaultValue="") final List<String> componentsToBeCalled)
			throws Exception {

		logger.info("startquestionansweringwithtextquestion: {} with {}", question, componentsToBeCalled);

		// you cannot pass without a question
		if (question.trim().isEmpty()) {
			throw new QanaryExceptionQuestionNotProvided();
		} else {
			QanaryQuestionCreated qanaryQuestionCreated = qanaryQuestionController.storeQuestion(question);
			QanaryQuestion qanaryQuestion = new QanaryQuestion(qanaryQuestionCreated.getQuestionURI().toURL(),qanaryConfigurator);
			qanaryQuestion.putAnnotationOfTextRepresentation();
			return this.questionanswering(componentsToBeCalled, qanaryQuestion.getQanaryMessage().asJsonString());
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
			@RequestParam(value = "componentlist[]", defaultValue="") final List<String> componentsToBeCalled)
			throws Exception {

		logger.info("startquestionansweringwithtextquestion: {} with {}", question, componentsToBeCalled);
		// you cannot pass without a valid question
		if (question.isEmpty()) {
			throw new QanaryExceptionQuestionNotProvided();
		} else {
			QanaryQuestionCreated qanaryQuestionCreated = qanaryQuestionController.storeAudioQuestion(question);
			QanaryQuestion qanaryQuestion = new QanaryQuestion(qanaryQuestionCreated.getQuestionURI().toURL(),qanaryConfigurator);
			qanaryQuestion.putAnnotationOfAudioRepresentation();
			return this.questionanswering(componentsToBeCalled, qanaryQuestion.getQanaryMessage().asJsonString());
		}
	}


	/**
	 * exposing the oa commons
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
	) throws Exception {
		QanaryMessage myQanaryMessage = new QanaryMessage(jsonMessage);
		QanaryQuestion myQanaryQuestion = new QanaryQuestion(myQanaryMessage);
		URI question = myQanaryQuestion.getUri();

		final UUID runID = UUID.randomUUID();
		logger.info("calling component: {} on named graph {} and endpoint {} ", componentsToBeCalled,
				myQanaryMessage.getEndpoint(), myQanaryMessage.getInGraph());

		// execute synchronous calls to all components with the same message
		// TODO: execute asynchronously?
		if (componentsToBeCalled.isEmpty()==false) { //if no component is passed nothing is happening
			qanaryConfigurator.callServicesByName(componentsToBeCalled, myQanaryMessage);
		}

		QanaryQuestionAnsweringRun myRun = new QanaryQuestionAnsweringRun(runID, question,
				myQanaryMessage.getEndpoint(), myQanaryMessage.getInGraph(), myQanaryMessage.getOutGraph(), qanaryConfigurator);
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
