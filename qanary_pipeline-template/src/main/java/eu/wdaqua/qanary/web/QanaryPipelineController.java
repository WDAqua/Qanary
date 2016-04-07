package eu.wdaqua.qanary.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.wdaqua.qanary.business.QanaryConfigurator;

/**
 * represents the general Qanary pipeline service interfaces
 * 
 * @author AnBo
 *
 */
@Controller
public class QanaryPipelineController {

	private static final Logger logger = LoggerFactory.getLogger(QanaryPipelineController.class);

	private QanaryConfigurator qanaryConfigurator;

	/**
	 * register the URL of a service, optionally by calling via HTTP
	 * 
	 * @param questionstring
	 */
	@RequestMapping(value = "/component", headers = "Accept=text/plain", method = RequestMethod.POST, produces = {
			"text/plain;charset=UTF-8" })
	@ResponseBody
	public String registerComponent(@RequestParam(value = "question", required = true) String questionstring) {
		// TODO: fetch the triples about the question from the triplestore

		// start the NO QA process

		// TODO: return the complete RDF object
		return null;
	}

	/**
	 * wrapper for SPARQL endpoint
	 * 
	 * @param sparqlquerystring
	 * @return
	 */
	@RequestMapping(value = "/sparql", headers = "Accept=application/rdf+xml", method = RequestMethod.POST, produces = {
			"text/turtle;charset=UTF-8" })
	@ResponseBody
	public String executeSparqlQuery(@RequestParam(value = "query", required = true) String sparqlquerystring) {
		// TODO: execute the query

		// TODO: return fetched triples
		return null;
	}

}
