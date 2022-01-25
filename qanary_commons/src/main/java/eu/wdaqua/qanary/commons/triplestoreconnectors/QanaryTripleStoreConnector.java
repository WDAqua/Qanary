package eu.wdaqua.qanary.commons.triplestoreconnectors;

import java.net.URI;

import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

public abstract class QanaryTripleStoreConnector {
	private static final Logger logger = LoggerFactory.getLogger(QanaryTripleStoreConnector.class);

	public abstract void connect(); // TODO: add exception

	public abstract ResultSet select(String sparql) throws SparqlQueryFailed;

	public abstract boolean ask(String sparql) throws SparqlQueryFailed;

	public abstract void update(String sparql, URI graph) throws SparqlQueryFailed;

	public abstract void update(String sparql) throws SparqlQueryFailed;

	/**
	 * return a readable description of the triplestore endpoint
	 * 
	 * @return
	 */
	public abstract String getFullEndpointDescription();

	/**
	 * get current time in milliseconds
	 */
	protected static long getTime() {
		return System.currentTimeMillis();
	}
	
	/**
	 *
	 * @param description
	 * @param duration
	 */
	protected void logTime(long duration, String description) {
		logger.info("runtime measurement: {} ms for {}", duration, description);
	}

	/**
	 * 
	 * @param duration
	 * @param description
	 */
	protected void logTime(long duration, String description, String endpoint) {
		logger.info("runtime measurement: {} ms on {} for {}", duration, endpoint, description);
	}

	protected Logger getLogger() {
		return this.logger;
	}

}
