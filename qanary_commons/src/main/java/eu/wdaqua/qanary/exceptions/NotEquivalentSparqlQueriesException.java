package eu.wdaqua.qanary.exceptions;

/**
 * Exception thrown when two SPARQL queries are not equivalent.
 */
public class NotEquivalentSparqlQueriesException extends Exception {

    private final String expectedQuery;
    private final String actualQuery;

    /**
     * Constructor that accepts a message.
     * 
     * @param message the message to be displayed when the exception is thrown
     */
    public NotEquivalentSparqlQueriesException(String message, String expectedQuery, String actualQuery) {
        super(message);
        this.expectedQuery = expectedQuery;
        this.actualQuery = actualQuery;
    }

    public String getExpectedQuery() {
        return expectedQuery;
    }

    public String getActualQuery() {
        return actualQuery;
    }
}
