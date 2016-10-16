package eu.wdaqua.qanary.business;

import java.net.URI;

import eu.wdaqua.qanary.message.QanaryMessage;
import eu.wdaqua.qanary.message.QanaryQuestionCreated;
import eu.wdaqua.qanary.web.QanaryQuestionAnsweringController;

/**
 * represents typical operations on questions
 * 
 * @author AnBo
 * 
 */
public abstract class QanaryQuestion {
	// TODO: needs to be changed to a common interactor
	QanaryQuestionAnsweringController sparqlOperator;

	// the uri of the question in the triplestore
	private final URI uri;

	// the graph containing the question
	private final URI graph;

	// the endpoint where the graph is stored
	private final URI endpoint;

	/**
	 * note: dependency to {@link QanaryQuestionAnsweringController} needs to be
	 * removed
	 * 
	 * @param endpoint
	 * @param graph
	 * @param questionUri
	 * @param sparqlOperator
	 */
	public QanaryQuestion(URI endpoint, URI graph, URI questionUri, QanaryQuestionAnsweringController sparqlOperator) {
		this.sparqlOperator = sparqlOperator;
		this.uri = questionUri;
		this.graph = graph;
		this.endpoint = endpoint;
		this.createAnnotationForQuestionRepresentation();
	}

	// boilerplate: getter
	public URI getUri() {
		return this.uri;
	}

	// boilerplate: getter
	public URI getGraph() {
		return this.graph;
	}

	// boilerplate: getter
	public URI getEndpoint() {
		return this.endpoint;
	}

	// boilerplate: getter
	public QanaryQuestionAnsweringController getSparqlOperator() {
		return this.sparqlOperator;
	}

	//
	/**
	 * returns the concrete annotator classes (of RDF vocabulary named qa) that
	 * need to be used to create the correct annotations for the given question
	 * 
	 * note: needs to be implemented in concrete classes
	 * 
	 * @return
	 */
	public abstract String[] getAnnotationName();

	/**
	 * save a question to a triplestore
	 */
	private void createAnnotationForQuestionRepresentation() {
		String uri = this.getUri().toString();
		for (String annotation : this.getAnnotationName()) {
			String sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "INSERT { " //
					+ "	GRAPH <" + this.getGraph() + "> { " //
					+ "  ?a a " + annotation + " . " //
					+ "  ?a oa:hasTarget <" + uri + "> . " //
					+ "  ?a oa:hasBody <" + uri + "> . " //
					+ "	 ?a oa:annotatedAt ?time  "//
					+ "	} " //
					+ "} WHERE { " //
					+ "     BIND (IRI(str(RAND())) AS ?a) ." //
					+ "     BIND (now() as ?time) " //
					+ "}";
			this.getSparqlOperator().loadTripleStore(sparqlquery, this.getEndpoint());
		}
	}

}
