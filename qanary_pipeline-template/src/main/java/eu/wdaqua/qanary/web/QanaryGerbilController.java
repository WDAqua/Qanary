package eu.wdaqua.qanary.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.QanaryComponentRegistrationChangeNotifier;
import eu.wdaqua.qanary.business.*;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.web.messages.GerbilExecuteResponse;
import io.swagger.v3.oas.annotations.Operation;

/**
 * controller for validating questions using Gerbil, i.e., validating the
 * question answering results
 *
 * @author Dennis Diefenbach
 */
@CrossOrigin
@Controller
public class QanaryGerbilController {

	private static final Logger logger = LoggerFactory.getLogger(QanaryGerbilController.class);
	public static final String GERBILEXECUTE = "/gerbil-execute/";
	private final QanaryConfigurator qanaryConfigurator;
	private final QanaryComponentRegistrationChangeNotifier qanaryComponentRegistrationChangeNotifier;

	private String host;
	private int port;

	// Set this to allow browser requests from other websites
	@ModelAttribute
	public void setVaryResponseHeader(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
	}

	/**
	 * inject QanaryConfigurator
	 */
	@Autowired
	public QanaryGerbilController(final QanaryConfigurator qanaryConfigurator,
			final QanaryPipelineConfiguration qanaryPipelineConfiguration,
			final QanaryQuestionController qanaryQuestionController,
			final QanaryComponentRegistrationChangeNotifier qanaryComponentRegistrationChangeNotifier) {
		this.qanaryConfigurator = qanaryConfigurator;
		this.qanaryComponentRegistrationChangeNotifier = qanaryComponentRegistrationChangeNotifier;
		this.host = qanaryPipelineConfiguration.getHost();
		this.port = qanaryPipelineConfiguration.getPort();
	}

	/**
	 * expose the model with the component names
	 */
	@ModelAttribute("componentList")
	public List<String> componentList() {
		List<String> components = qanaryComponentRegistrationChangeNotifier.getAvailableComponentNames();
		logger.info("available components: {}", components);
		return components;
	}

	/**
	 * a simple HTML input to generate a url-endpoint for Gerbil for QA,
	 * http://gerbil-qa.aksw.org/gerbil/config
	 */
	@RequestMapping(value = "/gerbil", method = RequestMethod.GET)
	@Operation(summary = "expose an HTML frontend for generating a Gerbil URL endpoint", operationId = "gerbil-html", description = "Generate a URL endpoint for Gerbil for QA (http://gerbil-qa.aksw.org/gerbil/config) through a simple HTML input form.")
	public String startquestionansweringwithtextquestion(Model model) {
		model.addAttribute("url", "Select components!");
		return "generategerbilendpoint";
	}

	/**
	 * given a list of components a url-endpoint for Gerbil for QA is generated
	 *
	 */
	@RequestMapping(value = "/gerbil", method = RequestMethod.POST)
	@Operation(summary = "expose a Gerbil URL endpoint for POST requests", operationId = "gerbilGenerator", description = "Generate a URL endpoint for Gerbil QA from a list of components.")
	public String gerbilGenerator(
			@RequestParam(value = QanaryStandardWebParameters.COMPONENTLIST, defaultValue = "") final List<String> componentsToBeCalled,
			Model model) throws Exception {
		String urlStr = "";
		if (componentsToBeCalled.size() == 0) {
			urlStr = "Select components!";
			model.addAttribute("url", urlStr);
		} else {
			// Generate a string like this "wdaqua-core0, QueryExecuter"
			String components = "/gerbil-execute/";
			for (String component : componentsToBeCalled) {
				components += component + ", ";
			}
			logger.info("components (0): {}", components);
			if (components.length() > 0) {
				components = components.substring(0, components.length() - 2);
			}
			logger.info("compoents (1): {}", components);
			// urlStr += URLEncoder.encode(components, "UTF-8")+"/";
			URI uri = new URI("http", null, new URL(host).getHost(), port, components + "/", null, null);
			URL url = uri.toURL();
			logger.info("created URL: {}", url.toString());
			model.addAttribute("url", url.toString());
		}
		return "generategerbilendpoint";
	}

	@RequestMapping(value = GERBILEXECUTE + "{components:.*}", method = RequestMethod.POST, produces = "application/json")
	@Operation(summary = "Start a Gerbil QA process with a list of components", operationId = "gerbil", description = "examples: curl -X POST http://localhost:8080/gerbil-execute/QAnswerQueryBuilderAndExecutor -d query='What is the capital of France?'  or curl -X POST http://localhost:8080/gerbil-execute/NED-DBpediaSpotlight -d query='What is the capital of France?'")
	public ResponseEntity<GerbilExecuteResponse> gerbil(
			@RequestParam(value = "query", required = true) final String question, //
			@RequestParam(value = "lang", required = false) String languageOfQuestion, //
			@RequestParam(value = QanaryStandardWebParameters.PRIORCONVERSATION, defaultValue = "", required = false) URI priorConversation, //
			@PathVariable("components") final String componentsToBeCalled //
	) throws URISyntaxException, Exception, SparqlQueryFailed {
		logger.info("Asked question: {}", question);
		logger.info("Language of question: {}", languageOfQuestion);
		logger.info("priorConversation: |{}|", priorConversation);
		logger.info("QA pipeline components: {}", componentsToBeCalled);
		logger.debug("available components: {}", this.componentList());

		// prepare message to process executor
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("question", question);
		map.add("language", languageOfQuestion);
		map.add("componentlist[]", componentsToBeCalled);
		if (priorConversation != null && !priorConversation.toASCIIString().trim().isEmpty()) {
			map.add(QanaryStandardWebParameters.PRIORCONVERSATION, priorConversation.toASCIIString());
			logger.info("priorConversation was not null and was not an empty string: |{}|",
					priorConversation.toASCIIString());
		} else {
			priorConversation = null;
		}

		// call process executor
		RestTemplate restTemplate = new RestTemplate();
		String response = restTemplate.postForObject(qanaryConfigurator.getHost() + ":" + qanaryConfigurator.getPort()
				+ "/startquestionansweringwithtextquestion", map, String.class);
		
		org.json.JSONObject json = new org.json.JSONObject(response);
		URI currentGraph = new URI((String) json.get("outGraph"));

		// retrieve text representation, SPARQL and JSON result
		QanaryMessage myQanaryMessage = new QanaryMessage(new URI((String) json.get("endpoint")),
				new URI((String) json.get("inGraph")), new URI((String) json.get("outGraph")));
		@SuppressWarnings("rawtypes")
		QanaryQuestion<?> myQanaryQuestion = new QanaryQuestion(myQanaryMessage, this.qanaryConfigurator);

		// Generates the following output
		/**
		 * <pre>
		 {
		     "questions": [{
		         "question": [{
		             "language": "en",   //(ISO 639-1)
		   			"string": "..."     //(textual representation of asked Question)
		   			    }],
		   		"query": {
		   		    "sparql": "..."     //(SPARQL Query constructed by QB component)
		         },
		   		"answers": [{"..."}]    //(Answers returned by QE component)
		     }]
		 }
		 * </pre>
		 **/
		// retrieve the content (question, SPARQL Query and answer)
		// getTextualRepresentation needs Exception to be thrown
		// tries to retrieve the language from Qanary triplestore, if not retrievable
		// use "en" as default
		String language = "en";
		try {
			String annotatedLang = myQanaryQuestion.getLanguage();
			if (annotatedLang != null && !annotatedLang.isEmpty()) {
				language = annotatedLang;
			}
		} catch (Exception e) {
			logger.warn("Could not retrieve language from triplestore, using \"{}\" instead", language);
		}
		String questionText = myQanaryQuestion.getTextualRepresentation();
		String sparqlQueryString = myQanaryQuestion.getSparqlResult(); // returns empty String if no Query was found
		String jsonAnswerString = "{}"; // TODO: myQanaryQuestion.getJsonResult(); // returns empty String if no answer was found

		GerbilExecuteResponse obj = new GerbilExecuteResponse(Jackson2ObjectMapperBuilder.json().build(), //
				questionText, language, sparqlQueryString, jsonAnswerString);

		// create a new header containing the
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Access-Control-Expose-Headers", QanaryStandardWebParameters.QANARYGRAPHHEADERNAME);
		responseHeaders.set(QanaryStandardWebParameters.QANARYGRAPHHEADERNAME, currentGraph.toASCIIString());
		logger.info("X-qanary-graph: {}", currentGraph.toASCIIString());
		logger.info("result: {}", obj.toString());

		return ResponseEntity.ok().headers(responseHeaders).body(obj);
	}
}
