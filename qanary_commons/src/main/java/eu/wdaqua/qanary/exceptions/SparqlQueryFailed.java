package eu.wdaqua.qanary.exceptions;

@SuppressWarnings("serial")
public class SparqlQueryFailed extends Exception {

	private Exception baseException;
	private String sparqlQuery;
	private String triplestore;

	public SparqlQueryFailed(String sparqlQuery, String triplestore, Exception baseException) {
		super("SPARQL query on " + triplestore + " failed: \n" //
				+ sparqlQuery //
				+ "\nsee also:\n" //
				+ baseException.getMessage() //
		);
		this.baseException = baseException;
		this.sparqlQuery = sparqlQuery;
		this.triplestore = triplestore;
	}

	public Exception getBaseException() {
		return baseException;
	}

	public String getSparqlQuery() {
		return sparqlQuery;
	}

	public String getTriplestore() {
		return triplestore;
	}

}
