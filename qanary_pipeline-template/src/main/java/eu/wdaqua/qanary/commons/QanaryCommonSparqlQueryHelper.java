package eu.wdaqua.qanary.commons;

import java.net.URI;

import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;

/**
 * contains some helper methods for accessing the data in the triplestore
 * 
 * @author AnBo
 *
 */
public class QanaryCommonSparqlQueryHelper {
	private static final Logger logger = LoggerFactory.getLogger(QanaryCommonSparqlQueryHelper.class);

	private final QanaryUtils myQanaryUtils;
	private final URI outGraph;
	private final URI endpoint;

	public QanaryCommonSparqlQueryHelper(QanaryQuestionAnsweringRun myQanaryQuestionAnsweringRun) {
		this.myQanaryUtils = new QanaryUtils(myQanaryQuestionAnsweringRun);
		this.outGraph = myQanaryQuestionAnsweringRun.getOutGraph();
		this.endpoint = myQanaryQuestionAnsweringRun.getEndpoint();
	}

	public URI getOutGraph() {
		return this.outGraph;
	}

	public URI getEndpoint() {
		return this.endpoint;
	}

	/**
	 * retrieves the answer if available as JSON from the defined
	 * triplestore/outGraph
	 * 
	 * @return
	 */
	public String getJsonAnswers() {
		String sparqlQuery = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "SELECT ?json " //
				+ "FROM <" + this.getOutGraph() + "> " //
				+ "WHERE { " //
				+ "  ?a a qa:AnnotationOfAnswerJSON . " //
				+ "  ?a oa:hasBody ?json " //
				+ "}";
		ResultSet r = myQanaryUtils.selectFromTripleStore(sparqlQuery, this.getEndpoint().toString());

		// If there are answers give them back
		String jsonAnswer = "";
		if (r.hasNext()) {
			jsonAnswer = r.next().getLiteral("json").toString();
			logger.info("JSONAnswer {}", jsonAnswer);
		}
		return jsonAnswer;
	}

	/**
	 * retrieves the answer if available as SPARQL query from the defined
	 * triplestore/outGraph
	 * 
	 * @return
	 */
	public String getSparqlQueryAnswer() {
		String sparqlQuery;
		ResultSet r;
		sparqlQuery = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "SELECT ?sparql " //
				+ "FROM <" + this.getOutGraph() + "> " //
				+ "WHERE { " //
				+ "  ?a a qa:AnnotationOfAnswerSPARQL . " //
				+ "  ?a oa:hasBody ?sparql " //
				+ "}";
		r = myQanaryUtils.selectFromTripleStore(sparqlQuery, this.getEndpoint().toString());

		String sparqlAnswer = "";
		if (r.hasNext()) {
			sparqlAnswer = r.next().getLiteral("sparql").toString();
			logger.info("SPARQLAnswer {}", sparqlAnswer);
		}
		return sparqlAnswer;
	}

}
