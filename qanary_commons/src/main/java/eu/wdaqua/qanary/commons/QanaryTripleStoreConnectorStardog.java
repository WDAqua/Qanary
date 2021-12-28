 package eu.wdaqua.qanary.commons;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.complexible.stardog.api.Connection;
import com.complexible.stardog.api.ConnectionConfiguration;
import com.complexible.stardog.api.ConnectionPool;
import com.complexible.stardog.api.ConnectionPoolConfig;
import com.complexible.stardog.api.UpdateQuery;
import com.complexible.stardog.jena.SDJenaFactory;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

/**
 * 
 * @author both
 * 
 * the component is initialized if and only if the required information is available
 * 
 * required parameters
 * <pre>
 * <code>
 * stardog.url=
 * stardog.username=
 * stardog.password=
 * </code>
 * </pre>
 * optional parameters:
 * <pre>
 * <code>
 * stardog.database= (default: qanary)
 * stardog.reasoningType= (default: false)
 * </code>
 * </pre>
 */
@Component
public class QanaryTripleStoreConnectorStardog extends QanaryTripleStoreConnector {

	private final URI url;
	private final String username;
	private final String password;
	private final String database;
	private final boolean reasoningType;
	private ConnectionPool connectionPool;

	public QanaryTripleStoreConnectorStardog( //
			@Value("${stardog.url}") URI url, //
			@Value("${stardog.username}") String username, //
			@Value("${stardog.password}") String password, //
			@Value("${stardog.database:qanary}") String database, //
			@Value("${stardog.reasoningType:false}") boolean reasoningType //
	) {
		this.url = url;
		this.username = username;
		this.password = password;
		this.database = database;
		this.reasoningType = reasoningType;
		this.getLogger().info("Stardog Connection initialized: url:{}, username:{}, password:{}, database:{}, reasoningType:{}", url, username, password, database, reasoningType);
		this.connect();
		this.getLogger().info("Stardog Connection created.");
	}

	public URI getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getDatabase() {
		return database;
	}

	public boolean isReasoningType() {
		return reasoningType;
	}

	public ConnectionPool getConnectionPool() {
		return connectionPool;
	}

	public void setConnectionPool(ConnectionPool connectionPool) {
		this.connectionPool = connectionPool;
	}
	
	private ConnectionPool createConnectionPool(ConnectionConfiguration connectionConfig) {

		// TODO: move to application.properties, update constructor
		int minPool = 1;
		int maxPool = 3;
		long expirationTime = 600;
		TimeUnit expirationTimeUnit = TimeUnit.SECONDS;
		long blockCapacityTime = 60;
		TimeUnit blockCapacityTimeUnit = TimeUnit.SECONDS;

		// https://docs.stardog.com/archive/7.5.0/developing/programming-with-stardog/java#using-sesame
		ConnectionPoolConfig poolConfig = ConnectionPoolConfig.using(connectionConfig) //
				.minPool(minPool).maxPool(maxPool) //
				.expiration(expirationTime, expirationTimeUnit) //
				.blockAtCapacity(blockCapacityTime, blockCapacityTimeUnit); //
		return poolConfig.create();
	}

	@Override
	public void connect() {
		ConnectionConfiguration connectionConfig = ConnectionConfiguration.to(this.getDatabase())
				.server(this.getUrl().toASCIIString()).reasoning(this.isReasoningType())
				.credentials(this.getUsername(), this.getPassword());
		this.setConnectionPool(createConnectionPool(connectionConfig)); // creates the Stardog connection pool
	}

	@Override
	public void importData(String turtleFormat, URI graph) {
		Connection connection = this.getConnectionPool().obtain();
		connection.begin();
		// declare the transaction
		// TODO: implement me
		// connection.add().io().format(RDFFormat.N3).stream(new FileInputStream("src/main/resources/marvel.rdf"));
		// and commit the change
		connection.commit();
	}

	@Override
	public ResultSet select(String sparql) throws SparqlQueryFailed {
		long start = getTime();
		Connection connection = this.getConnectionPool().obtain();
		Model aModel = SDJenaFactory.createModel(connection);
		Query aQuery = QueryFactory.create(sparql);
		try (QueryExecution aExec = QueryExecutionFactory.create(aQuery, aModel)) {
			ResultSetRewindable resultsRewindable = ResultSetFactory.makeRewindable(aExec.execSelect());
			this.logTime(getTime() - start, "SELECT on " + this.getUrl().toASCIIString() + ": " + sparql);
			return resultsRewindable;
		} catch (Exception e) {
			throw new SparqlQueryFailed(sparql, this.getUrl().toASCIIString(), e);
		}
	}
	
	@Override 
	public void update(String sparql, URI graph) {
		Connection connection = this.getConnectionPool().obtain();
		UpdateQuery query = connection.update(sparql, graph.toASCIIString());
		query.execute();
	}

	@Override 
	public void update(String sparql) {
		Connection connection = this.getConnectionPool().obtain();
		UpdateQuery query = connection.update(sparql);
		query.execute();
	}
}
