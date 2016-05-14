package eu.wdaqua.qanary.component;

import java.net.URI;
import java.util.Collection;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wdaqua.qanary.component.config.QanaryConfiguration;
import eu.wdaqua.qanary.component.ontology.TextPositionSelector;

/**
 * the class is a covering standard tasks users of the Qanary methodology might
 * have
 * 
 * @author AnBo
 *
 */
public class QanaryUtils {

	private static final Logger logger = LoggerFactory.getLogger(QanaryUtils.class);

	private final URI endpoint;
	private final URI inGraph;
	private final URI outGraph;

	QanaryUtils(QanaryMessage qanaryMessage) {
		this.endpoint = qanaryMessage.getEndpoint();
		this.inGraph = qanaryMessage.getInGraph();
		this.outGraph = qanaryMessage.getOutGraph();
	}

	/**
	 * returns the endpoint provided by the QanaryMessage object provided via
	 * constructor
	 * 
	 * @return
	 */
	public URI getEndpoint() {
		return this.endpoint;
	}

	/**
	 * returns the inGraph provided by the QanaryMessage object provided via
	 * constructor
	 *
	 * @return
	 */
	public URI getInGraph() {
		return this.inGraph;
	}

	/**
	 * returns the outGraph provided by the QanaryMessage object provided via
	 * constructor
	 *
	 * @return
	 */
	public URI getOutGraph() {
		return this.outGraph;
	}

	/**
	 * wrapper for selectTripleStore
	 * 
	 * @param sparqlQuery
	 * @param qanaryMessage
	 * @return
	 */
	public ResultSet selectFromTripleStore(String sparqlQuery) {
		return this.selectFromTripleStore(sparqlQuery, this.getEndpoint().toString());
	}

	/**
	 * query a SPARQL endpoint with a given query
	 * 
	 * @param sparqlQuery
	 * @param endpoint
	 * @return
	 */
	public ResultSet selectFromTripleStore(String sparqlQuery, String endpoint) {
		logger.debug("selectTripleStore on {} execute {}", endpoint, sparqlQuery);
		long start = getTime();
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
		ResultSet resultset = qExe.execSelect();
		this.logTime(getTime() - start, "selectFromTripleStore: " + sparqlQuery);
		return resultset;
	}

	/**
	 * insert data into triplestore, endpoint is taken from QanaryMessage
	 * 
	 * @param sparqlQuery
	 */
	public void updateTripleStore(String sparqlQuery) {
		this.updateTripleStore(sparqlQuery, this.getEndpoint().toString());
	}

	/**
	 * insert data into triplestore
	 * 
	 * @param sparqlQuery
	 * @param endpoint
	 */
	public void updateTripleStore(String sparqlQuery, String endpoint) {
		long start = getTime();
		UpdateRequest request = UpdateFactory.create(sparqlQuery);
		UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);
		proc.execute();
		this.logTime(getTime() - start, "updateTripleStore: " + sparqlQuery);
	}

	/**
	 * get the question URI from the pipeline endpoint
	 * 
	 * @param myQanaryMessage
	 * @return
	 * @throws Exception
	 */
	public QanaryQuestion getQuestion() throws Exception {
		ResultSet resultset = this.selectFromTripleStore(
				"SELECT ?question FROM <" + this.getInGraph()
						+ "> {?question <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.wdaqua.eu/qa#Question>}",
				this.getEndpoint().toString());

		int i = 0;
		String question = null;
		while (resultset.hasNext()) {
			question = resultset.next().get("question").asResource().getURI();
			logger.debug("{}: qa#Question = {}", i++, question);
		}
		if (i > 1) {
			throw new Exception("More than 1 question (count: " + i + ") in graph " + this.getInGraph() + " at "
					+ this.getEndpoint());
		} else if (i == 0) {
			throw new Exception("No question available in graph " + this.getInGraph() + " at " + this.getEndpoint());
		}

		URI questionUri = new URI(question);

		logger.info("question {} found in {} at {}", question, this.getInGraph(), this.getEndpoint());

		return new QanaryQuestion<Object>(questionUri);

	}

	/**
	 * adds oa:TextPositionSelectors to triplestore
	 * 
	 * @param selections
	 * @throws Exception
	 */
	public void addAnnotations(Collection<TextPositionSelector> selectors) throws Exception {
		String sparql;
		for (TextPositionSelector s : selectors) {
			sparql = "prefix qa: <http://www.wdaqua.eu/qa#> " //
					+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "INSERT { " + "GRAPH <" + this.getOutGraph() + "> { " //
					+ "  ?a a qa:AnnotationOfNamedEntity . " //
					+ "  ?a oa:hasTarget [ " //
					+ "                a oa:SpecificResource; " //
					+ "                oa:hasSource    <" + this.getQuestion() + ">; " //
					+ "                oa:hasSelector  [ " //
					+ "                    a oa:TextPositionSelector ; " //
					+ "                    oa:start \"" + s.getStart() + "\"^^xsd:nonNegativeInteger ; " //
					+ "                    oa:end  \"" + s.getEnd() + "\"^^xsd:nonNegativeInteger  " //
					+ "           ] " + "  ] ; " //
					+ "     oa:annotatedBy <" + this.getComponentUri() + "> ; " //
					+ "	    oa:AnnotatedAt ?time  " //
					+ "}} WHERE { " //
					+ "     BIND (IRI(str(RAND())) AS ?a) ." //
					+ "     BIND (now() as ?time) " //
					+ "}";
			this.updateTripleStore(sparql);
		}
	}

	/**
	 * wrapper for retrieving the URI where the service is currently running
	 * 
	 * @return
	 */
	private String getComponentUri() {
		return QanaryConfiguration.getServiceUri().toString();
	}

	/**
	 * get current time in milliseconds
	 * 
	 * @return
	 */
	static long getTime() {
		return System.currentTimeMillis();
	}

	/**
	 * 
	 * @param description
	 * @param duration
	 */
	private void logTime(long duration, String description) {
		logger.debug("runtime measurement: {} ms for {}", duration, description);
	}

}
