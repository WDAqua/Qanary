package eu.wdaqua.qanary;

import java.net.URL;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class QanaryPipelineController {

	/**
	 * get some information about your pipeline using the fancy HTML template
	 * description.html
	 * 
	 * @return
	 */
	@RequestMapping("/description")
	public String description() {
		// TODO: take from config "1st Qanary test service"
		return "description";
	}

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
	 * synchronous call to start the QA process (POST), return the URL of the
	 * created question
	 * 
	 * @param questionstring
	 */
	@RequestMapping(value = "/question", headers = "Accept=application/rdf+xml", method = RequestMethod.POST, produces = {
			"text/plain;charset=UTF-8" })
	@ResponseBody
	public URL createQuestion(@RequestParam(value = "question", required = true) String questionstring) {
		// TODO: insert the question into triplestore

		// TODO: execute the QA process: for each of your components call
		// /annotatequestion by passing a QanaryMessage to them, OPEN ISSUE:
		// execute the alignment on pipeline or component side?

		// TODO: return the answer RDF object
		return null;
	}

	/**
	 * fetch all triples about a given question
	 * 
	 * @param questionstring
	 */
	@RequestMapping(value = "/question/{question}", headers = "Accept=application/rdf+xml", method = RequestMethod.GET, produces = {
			"text/turtle;charset=UTF-8" })
	@ResponseBody
	public String getQuestion(@PathVariable URL questionuri) {
		// TODO: fetch the triples about the question from the triplestore

		// note: do NOT start QA process

		// TODO: return the complete RDF object
		return null;
	}

	/**
	 * fetch the processing status of a given question
	 * 
	 * @param questionstring
	 */
	@RequestMapping(value = "/question/{question}/status", headers = "Accept=application/json", method = RequestMethod.GET, produces = {
			"application/json;charset=UTF-8" })
	@ResponseBody
	public String getStatusOfQuestion(@PathVariable URL questionuri) {
		// TODO: fetch the processing status of the given question URL

		// TODO:

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
