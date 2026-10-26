package eu.wdaqua.qanary.commons;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.jena.query.QueryParseException;
import org.junit.jupiter.api.Test;

import eu.wdaqua.qanary.exceptions.NotEquivalentSparqlQueriesException;

/**
 * Tests how {@link QanaryUtils#compareSparqlQueries(String, String)} rejects
 * queries that are not valid SPARQL, not equivalent, or not comparable at all.
 * The successful comparisons are covered by {@code qa.commons.QanaryUtilsTest}.
 */
class QanaryUtilsCompareFailuresTest {

    private static final String INSERT = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
            + "INSERT DATA { GRAPH <urn:g> { <urn:a> a qa:AnnotationOfInstance ; qa:score 0.5 . } }";

    @Test
    void textThatIsNeitherQueryNorUpdateIsReportedAsParseError() {
        assertThrows(QueryParseException.class,
                () -> QanaryUtils.compareSparqlQueries("this is not SPARQL", "neither is this"));
        assertThrows(QueryParseException.class,
                () -> QanaryUtils.compareAsUpdate("this is not SPARQL", INSERT));
    }

    @Test
    void updatesWithDifferentContentAreNotEquivalent() {
        String otherScore = INSERT.replace("0.5", "0.9");

        assertThrows(NotEquivalentSparqlQueriesException.class,
                () -> QanaryUtils.compareSparqlQueries(INSERT, otherScore));
    }

    @Test
    void anUpdateMissingPartsOfTheExpectedOneIsNotEquivalent() {
        String shorter = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
                + "INSERT DATA { GRAPH <urn:g> { <urn:a> a qa:AnnotationOfInstance . } }";

        assertThrows(NotEquivalentSparqlQueriesException.class,
                () -> QanaryUtils.compareSparqlQueries(INSERT, shorter));
    }

    @Test
    void aMissingExpectedQueryIsAnError() {
        assertThrows(RuntimeException.class, () -> QanaryUtils.compareSparqlQueries(null, INSERT));
    }
}
