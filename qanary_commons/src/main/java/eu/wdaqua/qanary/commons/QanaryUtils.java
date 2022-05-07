package eu.wdaqua.qanary.commons;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;

/**
 * the class is a covering standard tasks users of the Qanary methodology might
 * have
 *
 * if you are missing helper methods than please request them via
 * {@see <a href="https://github.com/WDAqua/Qanary/issues/new">Github</a>}
 *
 * @author AnBo
 */
public class QanaryUtils {

	private static final Logger logger = LoggerFactory.getLogger(QanaryUtils.class);

	private final URI endpoint;
	private final URI inGraph;
	private final URI outGraph;
	private final QanaryTripleStoreConnector qanaryTripleStoreConnector;

	public QanaryUtils(QanaryMessage qanaryMessage, QanaryTripleStoreConnector myQanaryTripleStoreConnector) { 
		this.endpoint = qanaryMessage.getEndpoint();
		this.inGraph = qanaryMessage.getInGraph();
		this.outGraph = qanaryMessage.getOutGraph();
		this.qanaryTripleStoreConnector = myQanaryTripleStoreConnector;
	}

	public QanaryUtils(QanaryQuestionAnsweringRun qanaryRun, QanaryTripleStoreConnector myQanaryTripleStoreConnector) {
		this.endpoint = qanaryRun.getEndpoint();
		this.inGraph = qanaryRun.getInGraph();
		this.outGraph = qanaryRun.getOutGraph();
		this.qanaryTripleStoreConnector = myQanaryTripleStoreConnector;
	}

	/**
	 * returns the endpoint provided by the QanaryMessage object provided via
	 * constructor
	 */
	public URI getEndpoint() {
		return this.endpoint;
	}

	/**
	 * returns the inGraph provided by the QanaryMessage object provided via
	 * constructor
	 */
	public URI getInGraph() {
		return this.inGraph;
	}

	/**
	 * returns the outGraph provided by the QanaryMessage object provided via
	 * constructor
	 */
	public URI getOutGraph() {
		return this.outGraph;
	}
	
	/**
	 * returns the instance of QanaryTripleStoreConnector
	 * 
	 * @return
	 */
	public QanaryTripleStoreConnector getQanaryTripleStoreConnector() {
		return this.qanaryTripleStoreConnector;
	}

	/**
	 * query a SPARQL endpoint with a given SELECT query
	 * 
	 * @throws SparqlQueryFailed
	 */
	@Deprecated
	public ResultSet selectFromTripleStore(String sparqlQuery, String endpoint) throws SparqlQueryFailed {
		return this.qanaryTripleStoreConnector.select(sparqlQuery);
	}

	/**
	 * query a SPARQL endpoint with a given ASK query
	 * @throws SparqlQueryFailed 
	 */
	@Deprecated
	public boolean askTripleStore(String sparqlQuery, String endpoint) throws SparqlQueryFailed {
		logger.debug("askTripleStore on {} execute {}", endpoint, sparqlQuery);
		return this.qanaryTripleStoreConnector.ask(sparqlQuery);
	}

	/**
	 * wrapper for updateTripleStore
	 * 
	 * @throws URISyntaxException
	 * @throws SparqlQueryFailed
	 */
	@Deprecated
	public void updateTripleStore(String sparqlQuery, QanaryConfigurator myQanaryConfigurator)
			throws URISyntaxException, SparqlQueryFailed {
		this.updateTripleStore(sparqlQuery, myQanaryConfigurator.getEndpoint());
	}

	/**
	 * wrapper for updateTripleStore
	 * 
	 * @param sparqlQuery
	 * @param endpoint
	 * @throws SparqlQueryFailed
	 */
	@Deprecated
	public void updateTripleStore(String sparqlQuery, URL endpoint) throws SparqlQueryFailed {
		this.updateTripleStore(sparqlQuery, endpoint.toString());
	}

	/**
	 * wrapper for updateTripleStore
	 * 
	 * @param sparqlQuery
	 * @param endpoint
	 * @throws SparqlQueryFailed
	 */
	@Deprecated
	public void updateTripleStore(String sparqlQuery, URI endpoint) throws SparqlQueryFailed {
		this.updateTripleStore(sparqlQuery, endpoint.toString());
	}

	/**
	 * insert data into triplestore
	 * 
	 * @throws SparqlQueryFailed
	 */
	@Deprecated
	public void updateTripleStore(String sparqlQuery, String endpoint) throws SparqlQueryFailed {
		logger.debug("updateTripleStore on {}: {}", endpoint, sparqlQuery);
		this.getQanaryTripleStoreConnector().update(sparqlQuery);
	}

	/**
	 * wrapper for retrieving the URI where the service is currently running
	 */
	public String getComponentUri() {
		return QanaryConfiguration.getServiceUri().toString();
	}

	/**
	 * wrapper for retrieving the URI where the service is currently running
	 */
	public String getHostUri() {
		return QanaryConfiguration.getHostUri().toString();
	}

	/**
	 * get current time in milliseconds
	 */
	public static long getTime() {
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
