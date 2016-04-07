package eu.wdaqua.qanary.web;

import java.net.URI;
import java.net.URL;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;

import eu.wdaqua.qanary.business.QanaryConfigurator;

/**
 * controller for processing questions, i.e., related to the question answering
 * process
 * 
 * @author AnBo
 *
 */
@Controller
public class QanaryQuestionAnsweringController {

	private static final Logger logger = LoggerFactory.getLogger(QanaryQuestionAnsweringController.class);

	private final QanaryConfigurator qanaryConfigurator;

	/**
	 * inject QanaryConfigurator
	 */
	@Autowired
	public QanaryQuestionAnsweringController(QanaryConfigurator qanaryConfigurator) {
		this.qanaryConfigurator = qanaryConfigurator;
	}

	/**
	 * a simple HTML input form for starting a question answering processs
	 * 
	 * @return
	 */
	@RequestMapping(value = "/startquestionanswering", method = RequestMethod.GET)
	public String startquestionanswering() {
		return "startquestionanswering";
	}

	/**
	 * start a configured process
	 * 
	 * @return
	 */
	@RequestMapping(value = "/questionanswering", method = RequestMethod.POST)
	@ResponseBody
	public String questionanswering(@RequestParam(value = "question", required = true) URL questionUri) {
		// Create the name of a new named graph
		UUID runID = UUID.randomUUID();
		String namedGraph = runID.toString();
		this.initGraphInTripelStore(namedGraph, questionUri);

		// TODO: call all defined components

		return runID.toString();
	}

	/**
	 * init the grpah in the triplestore (c.f., applicationproperties)
	 * 
	 * @param namedGraph
	 * @param questionUri
	 */
	private void initGraphInTripelStore(String namedGraph, URL questionUri) {
		URI triplestore = qanaryConfigurator.getEndpoint();
		namedGraph = "<urn:graph:" + namedGraph + ">";

		// Load the Open Annotation Ontology
		// TODO: store this locally for performance issues
		String sparqlquery = "";
		sparqlquery = "LOAD <http://www.openannotation.org/spec/core/20130208/oa.owl> INTO GRAPH " + namedGraph;
		System.out.println("\n ++++++++++++++++\n" + sparqlquery);
		loadTripleStore(sparqlquery, triplestore);

		logger.debug("UPDATED");

		// TODO: load ontology into graph
		// Load the QAontology
		sparqlquery = "LOAD <http://localhost:" + qanaryConfigurator.getPort() + "/QAOntology_raw.ttl> INTO GRAPH "
				+ namedGraph;
		loadTripleStore(sparqlquery, triplestore);

		// Prepare the question, answer and dataset objects
		sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#>" + "INSERT DATA {GRAPH " + namedGraph + "{ <"
				+ questionUri.toString() + "> a qa:Question}}";
		loadTripleStore(sparqlquery, triplestore);

		sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#>" + "INSERT DATA {GRAPH " + namedGraph + "{"
				+ this.getQuestionAnsweringHostUrlString() + "/Answer> a qa:Answer}}";
		loadTripleStore(sparqlquery, triplestore);

		sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#>" + "INSERT DATA {GRAPH " + namedGraph + "{"
				+ qanaryConfigurator.getHost() + ":" + qanaryConfigurator.getPort() + "/Dataset> a qa:Dataset}}";
		loadTripleStore(sparqlquery, triplestore);

		// Make the first two annotations
		sparqlquery = "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "INSERT DATA { " + "GRAPH " + namedGraph //
				+ "{ " //
				+ "<anno1> a  oa:AnnotationOfQuestion; " //
				+ "   oa:hasTarget <" + questionUri.toString() + "> ;" //
				+ "   oa:hasBody   <URIAnswer>   ." //
				+ "<anno2> a  oa:AnnotationOfQuestion;" //
				+ "   oa:hasTarget <" + questionUri.toString() + "> ;" //
				+ "   oa:hasBody   <URIDataset> " + "}}";
		loadTripleStore(sparqlquery, triplestore);
	}

	/**
	 * executes a SPARQL INSERT into the triplestore
	 * 
	 * @param query
	 * @return map
	 */
	public static void loadTripleStore(String sparqlQuery, URI endpoint) {
		UpdateRequest request = UpdateFactory.create(sparqlQuery);
		UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint.toString());
		proc.execute();
	}

	/**
	 * returns a valid URL (string) of configured properties
	 * 
	 */
	private String getQuestionAnsweringHostUrlString() {
		return this.qanaryConfigurator.getHost() + ":" + this.qanaryConfigurator.getPort() + "/";
	}
}
