package eu.wdaqua.qanary.web;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;

/**
 * controller providing an embedded front end for human users
 * 
 */
@Controller
public class QanaryEmbeddedQaWebFrontendController {
	// the string used for the endpoints w.r.t. the question answering process
	public static final String FRONTENDENDPOINT = "/qa";
	private static final Logger logger = LoggerFactory.getLogger(QanaryQuestionAnsweringController.class);
	private final QanaryConfigurator qanaryConfigurator;
	private final QanaryQuestionController qanaryQuestionController;
	private final QanaryQuestionAnsweringController qanaryQuestionAnsweringController;

	/**
	 * Set this to allow browser requests from other websites
	 */
	@ModelAttribute
	public void setVaryResponseHeader(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
	}

	/**
	 * inject QanaryConfigurator and required controller
	 */
	@Autowired
	public QanaryEmbeddedQaWebFrontendController(final QanaryConfigurator qanaryConfigurator,
			final QanaryQuestionController qanaryQuestionController,
			final QanaryQuestionAnsweringController qanaryQuestionAnsweringController) {
		this.qanaryConfigurator = qanaryConfigurator;
		this.qanaryQuestionController = qanaryQuestionController;
		this.qanaryQuestionAnsweringController = qanaryQuestionAnsweringController;

		logger.info("default question answering system will run with the following components: {}",
				this.qanaryConfigurator.getDefaultComponentNames());
	}

	/**
	 * publish the HTML search front end
	 * 
	 * @return
	 */
	@RequestMapping(value = "/qa", method = RequestMethod.GET)
	public String qa() {
		return "qa_input";
	}

	/**
	 * start a predefined question answering process
	 * 
	 * @param question
	 * @param model
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/qa", method = RequestMethod.POST)
	public String qa(@RequestParam(value = "question", required = true) final String question, Model model)
			throws Exception {

		logger.info("Asked question {}", question);
		model.addAttribute("question", question);

		// define the question answering system run, select as a component
		// the component from the default list in the currently used
		// application.properties
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("question", question);
		map.add("componentlist[]", qanaryConfigurator.getDefaultComponentNamesAsString());

		// Send the question to the startquestionansweringwithtextquestion
		ResponseEntity<?> response = qanaryQuestionAnsweringController.startquestionansweringwithtextquestion(question,
				qanaryConfigurator.getDefaultComponentNames());
		QanaryQuestionAnsweringRun run = (QanaryQuestionAnsweringRun) response.getBody();
		logger.warn("response from startquestionansweringwithtextquestion: {}", run);

		// retrieve the answers as JSON object from the triplestore
		QanaryUtils myQanaryUtils = new QanaryUtils(run);
		String sparqlQuery = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "SELECT ?json " + "FROM <" + run.getOutGraph() + "> " //
				+ "WHERE { " + "  ?a a qa:AnnotationOfAnswerJSON . " + "  ?a oa:hasBody ?json " + "}";
		ResultSet r = myQanaryUtils.selectFromTripleStore(sparqlQuery, run.getEndpoint().toString());

		// If there are answers give them back
		String jsonAnswer = "";
		if (r.hasNext()) {
			jsonAnswer = r.next().getLiteral("json").toString();
			logger.info("JSONAnswer {}", jsonAnswer);
		}
		sparqlQuery = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "SELECT ?sparql " + "FROM <" + run.getOutGraph() + "> " //
				+ "WHERE { " + "  ?a a qa:AnnotationOfAnswerSPARQL . " + "  ?a oa:hasBody ?sparql " + "}";
		r = myQanaryUtils.selectFromTripleStore(sparqlQuery, run.getEndpoint().toString());
		
		String sparqlAnswer = "";
		if (r.hasNext()) {
			sparqlAnswer = r.next().getLiteral("sparql").toString();
			logger.info("SPARQLAnswer {}", sparqlAnswer);
		}
		if (jsonAnswer.equals("") == false && sparqlAnswer.equals("") == false) {
			// Parse the JSON result set using Jena
			ResultSetFactory factory = new ResultSetFactory();
			InputStream in = new ByteArrayInputStream(jsonAnswer.getBytes());
			ResultSet result = factory.fromJSON(in);
			List<String> var = result.getResultVars();
			List<String> list = new ArrayList<>();
			while (result.hasNext()) {
				for (String v : var) {
					list.add(result.next().get(v).toString());
				}
			}
			// Write the answers to the model such that it is available for the
			// HTML template
			model.addAttribute("answers", list);
			// Format the SPARQL query nicely
			Query query = QueryFactory.create(sparqlAnswer);
			query.setPrefix("dbr", "http://dbpedia.org/resource/");
			query.setPrefix("dbp", "http://dbpedia.org/property/");
			query.setPrefix("dbo", "http://dbpedia.org/ontology/");
			String formatted = query.serialize();
			formatted = formatted.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>");
			model.addAttribute("sparqlQuery", formatted);
			return "qa_output";
		}
		return "qa_no-answer";
	}

}
