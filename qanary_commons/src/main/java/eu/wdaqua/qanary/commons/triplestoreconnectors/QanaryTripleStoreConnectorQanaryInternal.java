package eu.wdaqua.qanary.commons.triplestoreconnectors;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.util.Lists;
import com.stardog.stark.vocabs.RDF;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.sparql.exec.QueryExecBuilder; // QueryExecBuilder is required for RDFConnection
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;

/**
 * simple connection to open endpoint without authorization using the Apache Jena library
 * 
 * @author AnBo
 *
 */
public class QanaryTripleStoreConnectorQanaryInternal extends QanaryTripleStoreConnector {
	private static final Logger logger = LoggerFactory.getLogger(QanaryTripleStoreConnectorQanaryInternal.class);
	private URI endpoint;
	private RDFConnection connection;
	private String applicationName;
	@Value("${data.logging")
	private boolean dataLogging;
	
	public QanaryTripleStoreConnectorQanaryInternal(URI endpoint, String applicationName) throws URISyntaxException {
		this.setApplicationName(applicationName);
		this.endpoint = endpoint;
		this.connect();
	}

	public String getApplicationName() {
		return applicationName;
	}

	private void setApplicationName(String applicationName) {
		this.applicationName = applicationName; // set to "urn:qanary:" + name // Check vulnerabilities
	}

	private URI getEndpoint() {
		return this.endpoint;
	}
	
	@Override
	public void connect() {
		org.apache.jena.query.ARQ.init();  // maybe it helps to prevent problems?
		this.connection = RDFConnection.connect(this.getEndpoint().toASCIIString());
		/* 	TODO; RDFConnectionRemote doesn't accept org.apache.jena.HttpClient/ClosedHttpClient anymore, instead it uses java.HttpClient
			TODO; ClosedHttpClient cannot be casted into HttpClient. Therefore, it is not possible to pass the Header as
			TODO; java.HttpClient doesn't consist a header, but a HttpRequest does.
			TODO; QueryExecution (Jena) instead still accept a org.jena.HttpClient where a header can be added.
		Header header = new BasicHeader("QANARY_COMPONENT_LOGGING", String.valueOf(dataLogging));
		List<Header> headers = new ArrayList<>() {{add(header);}};
		org.apache.http.client.HttpClient httpClient = HttpClients.custom().setDefaultHeaders(headers).build();

		RDFConnection rdfConnection = RDFConnectionRemote.newBuilder().destination(this.getEndpoint().toASCIIString()).httpClient((HttpClient) httpClient).build();
		this.connection = rdfConnection;
		*/
	}

	@Override
	public ResultSet select(String sparql) throws SparqlQueryFailed {
		QueryExecution queryExecution = this.connection.query(sparql);
		logData(sparql);
		ResultSet myResultSet = queryExecution.execSelect();
		ResultSetRewindable resultSet = ResultSetFactory.makeRewindable(myResultSet);
		return resultSet;
	}

	private void logData(String sparql) {
		QuerySolutionMap querySolutionMap = new QuerySolutionMap();
		Query query = QueryFactory.create(sparql);
		querySolutionMap.add("graph",ResourceFactory.createResource(query.getGraphURIs().get(0)));
		querySolutionMap.add("component", ResourceFactory.createResource("urn:qanary:" + this.applicationName));
		querySolutionMap.add("questionID", ResourceFactory.createResource("urn:qanary:currentQuestion"));
		querySolutionMap.add("body", ResourceFactory.createPlainLiteral(sparql));
		try {
			this.update(QanaryTripleStoreConnector.readFileFromResourcesWithMap("/queries/insert_explanation_data_sparql_query.rq", querySolutionMap));
		} catch(Exception e) {
			getLogger().error("Logging failed, {}", e);
		}

	}

	@Override
	public void update(String sparql, URI graph) throws SparqlQueryFailed {
		this.update(sparql);
	}

	@Override
	public void update(String sparql) throws SparqlQueryFailed {
		logger.debug("UPDATE on {}: {}", this.getEndpoint().toASCIIString(), sparql);
		this.connection.update(sparql);
	}

	@Override
	public boolean ask(String sparql) throws SparqlQueryFailed {
		logData(sparql);
		return this.connection.queryAsk(sparql);
	}

	@Override
	public String getFullEndpointDescription() {
		return "simple open endpoint without authorization: " + this.getEndpoint().toASCIIString();
	}
}
