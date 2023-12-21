package eu.wdaqua.qanary.commons.triplestoreconnectors;

import java.net.URI;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

/**
 * 
 * @author AnBo-de
 * 
 *         the component connects to a Virtuoso triplestore for connecting and
 *         executing queries
 * 
 *         the component is initialized if and only if the required information
 *         is available
 * 
 *         required parameters
 * 
 *         <pre>
 * <code>
 * virtuoso.url=
 * virtuoso.username=
 * virtuoso.password=
 * </code>
 *         </pre>
 */
@ConditionalOnProperty(name = { "virtuoso.url", "virtuoso.username", "virtuoso.password" }, matchIfMissing = false)
@Component
public class QanaryTripleStoreConnectorVirtuoso extends QanaryTripleStoreConnector {

	private final String virtuosoUrl;
	private final String username;
	private final String password;
	private VirtGraph connection;

	public QanaryTripleStoreConnectorVirtuoso( //
			@Value("${virtuoso.url}") String virtuosoUrl, //
			@Value("${virtuoso.username}") String username, //
			@Value("${virtuoso.password}") String password //
	) {
		getLogger().info("initialize Virtuoso triplestore connector as {} to {}", username, virtuosoUrl);
		this.virtuosoUrl = virtuosoUrl;
		this.username = username;
		this.password = password;
		this.connect();
	}

	public String getVirtuosoUrl() {
		return this.virtuosoUrl;
	}

	private String getUsername() {
		return this.username;
	}

	private String getPassword() {
		return this.password;
	}

	@Override
	public void connect() {
		getLogger().debug("Virtuoso server connecting to {}", this.getVirtuosoUrl());
		assert this.virtuosoUrl != null && !"".equals(this.virtuosoUrl);
		assert this.username != null && !"".equals(this.username);
		assert this.password != null && !"".equals(this.password);

		connection = new VirtGraph(this.getVirtuosoUrl(), this.getUsername(), this.getPassword());
		getLogger().info("Virtuoso server connected at {}", this.getVirtuosoUrl());
		assert connection != null;
	}

	@Override
	public ResultSet select(String sparql) throws SparqlQueryFailed {
		long start = getTime();
		getLogger().info("execute SELECT query: {}", sparql);
		Query query = QueryFactory.create(sparql);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(query, this.connection);
		ResultSetRewindable resultsRewindable = ResultSetFactory.makeRewindable(vqe.execSelect());
		this.logTime(getTime() - start, "SELECT on " + this.getVirtuosoUrl() + " resulted in " + resultsRewindable.size() + " rows: " + sparql);
		return resultsRewindable;
	}

	@Override
	public boolean ask(String sparql) throws SparqlQueryFailed {
		long start = getTime();
		getLogger().info("execute ASK query: {}", sparql);
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(QueryFactory.create(sparql), this.connection);
		boolean result = vqe.execAsk();
		this.logTime(getTime() - start, "ASK on " + this.getVirtuosoUrl() + ": " + sparql);
		return result;
	}

	@Override
	public void update(String sparql, URI graph) throws SparqlQueryFailed {
		long start = getTime();
		getLogger().info("execute UPDATE query on graph {}: {}", graph, sparql);
		VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(sparql, this.connection);
		vur.exec();
		this.logTime(getTime() - start, "UPDATE on " + this.getVirtuosoUrl() + ": " + sparql);
	}

	@Override
	public void update(String sparql) throws SparqlQueryFailed {
		long start = getTime();
		getLogger().info("execute UPDATE query: {}", sparql);
		VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(sparql, this.connection);
		vur.exec();
		this.logTime(getTime() - start, "UPDATE on " + this.getVirtuosoUrl() + ": " + sparql);
	}

	@Override
	public String getFullEndpointDescription() {
		return "Virtuoso tiplestore connected on the endpoint " + this.getVirtuosoUrl();
	}

}
