package eu.wdaqua.qanary.commons;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpException;
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

	public QanaryUtils(QanaryMessage qanaryMessage) {
		this.endpoint = qanaryMessage.getEndpoint();
		this.inGraph = qanaryMessage.getInGraph();
		this.outGraph = qanaryMessage.getOutGraph();
	}

	public QanaryUtils(QanaryQuestionAnsweringRun qanaryRun) {
		this.endpoint = qanaryRun.getEndpoint();
		this.inGraph = qanaryRun.getInGraph();
		this.outGraph = qanaryRun.getOutGraph();
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
	 * wrapper for selectTripleStore
	 */
	public ResultSet selectFromTripleStore(String sparqlQuery) {
		return this.selectFromTripleStore(sparqlQuery, this.getEndpoint().toString());
	}

	/**
	 * query a SPARQL endpoint with a given SELECT query
	 */
	public ResultSet selectFromTripleStore(String sparqlQuery, String endpoint) {
		logger.info("SELECT on {}: {}", endpoint, sparqlQuery);
		sparqlQuery = "X"+sparqlQuery;
		
		try {			
			return selectFromTripleStoreHelper(sparqlQuery, endpoint);
		} catch (Exception e) {
			
			// problem might be the Stardog v5+ infamous endpoint distinction for update and select queries 
			logger.warn("SELECT query failed on {}: {}", endpoint, e.getMessage() );
			
			String endpointForStardogToBeTested = endpoint.concat("/query");
			logger.info("try SELECT query on {}", endpointForStardogToBeTested);

			return selectFromTripleStoreHelper(sparqlQuery, endpointForStardogToBeTested);
		}
	}
	
	/**
	 * Execute a given query on the provided triplestore endpoint
	 * 
	 * @param sparqlQuery
	 * @param endpoint
	 * @return
	 */
	private ResultSet selectFromTripleStoreHelper(String sparqlQuery, String endpoint) {
		long start = getTime();		
		QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
		ResultSet resultset = qExe.execSelect();
		this.logTime(getTime() - start, "SELECT on " + endpoint + ": " + sparqlQuery);
		return resultset;
	}

	/**
	 * query a SPARQL endpoint with a given ASK query
	 */
	public boolean askTripleStore(String sparqlQuery, String endpoint) {
		logger.debug("askTripleStore on {} execute {}", endpoint, sparqlQuery);
		long start = getTime();
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
		boolean b = qExe.execAsk();
		this.logTime(getTime() - start, "askFromTripleStore: " + sparqlQuery);
		return b;
	}

	/**
	 * insert data into triplestore, endpoint is taken from QanaryMessage
	 * 
	 * @throws URISyntaxException
	 * @throws SparqlQueryFailed
	 */
	public void updateTripleStore(String sparqlQuery, QanaryConfigurator myQanaryConfigurator)
			throws URISyntaxException, SparqlQueryFailed {
		this.updateTripleStore(sparqlQuery, myQanaryConfigurator.getLoadEndpoint().toString());
	}

	/**
	 * insert data into triplestore
	 * 
	 * @throws SparqlQueryFailed
	 */
	public void updateTripleStore(String sparqlQuery, String endpoint) throws SparqlQueryFailed {
		logger.debug("updateTripleStore on {}: {}", endpoint, sparqlQuery);
		long start = getTime();
		UpdateRequest request = UpdateFactory.create(sparqlQuery);
		UpdateProcessor proc;

		try {
			proc = UpdateExecutionFactory.createRemote(request, endpoint);
			executeUpdateTripleStore(proc, sparqlQuery, endpoint);
		} catch (Exception e) {
			// problem might be the Stardog v5+ infamous endpoint distinction for update and
			// select queries
			logger.warn("update query failed on {}: {}", endpoint, e.getMessage());

			// re-try with extended endpoint URL
			String endpointForStardogToBeTested = endpoint.concat("/update");
			logger.info("try update query on {}", endpointForStardogToBeTested);
			proc = UpdateExecutionFactory.createRemote(request, endpointForStardogToBeTested);
			executeUpdateTripleStore(proc, sparqlQuery, endpointForStardogToBeTested);
		}

		this.logTime(getTime() - start, "updateTripleStore: " + sparqlQuery);
	}

	/**
	 * executes a SPARQL INSERT into the triplestore
	 *
	 * TODO: needs to be extracted
	 * TODO: add timeout
	 *
	 * @return map
	 * @throws SparqlQueryFailed 
	 */
	public static void loadTripleStore(final String sparqlQuery, final URI loadEndpoint) throws SparqlQueryFailed {
		final UpdateRequest request = UpdateFactory.create(sparqlQuery);
		final UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, loadEndpoint.toString());
		executeUpdateTripleStore(proc, sparqlQuery, loadEndpoint);
	}

	public static void loadTripleStore(final String sparqlQuery, final QanaryConfigurator myQanaryConfigurator)
			throws URISyntaxException, SparqlQueryFailed {
		loadTripleStore(sparqlQuery, myQanaryConfigurator.getLoadEndpoint());
	}

	/**
	 * wrapper for retrieving the URI where the service is currently running
	 */
	public String getComponentUri() {
		return QanaryConfiguration.getServiceUri().toString();
	}

	
	/**
	 * executes an UPDATE SPARQL query and creates a corresponding exception on errors (wrapper)
	 */
	private static void executeUpdateTripleStore(UpdateProcessor proc, String sparqlQuery, URI updateEndpoint)
			throws SparqlQueryFailed {
		executeUpdateTripleStore(proc, sparqlQuery, updateEndpoint.toString());
	}
	
	/**
	 * executes an UPDATE SPARQL query and creates a corresponding exception on errors
	 */
	private static void executeUpdateTripleStore(UpdateProcessor proc, String sparqlQuery, String updateEndpoint)
			throws SparqlQueryFailed {
		try {
			proc.execute(); // Execute the update
		} catch (Exception e) {
			logger.error("Execution of SPARQL query failed with error: {} \n SPARQL: {} \n Stacktrace {}", //
					e.getMessage(), sparqlQuery, ExceptionUtils.getStackTrace(e));
			throw new SparqlQueryFailed(sparqlQuery, updateEndpoint, e);
		}
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
