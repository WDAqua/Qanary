package eu.wdaqua.qanary.commons.triplestoreconnectors;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;

import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests for the static SPARQL-template helpers of {@link QanaryTripleStoreConnector}.
 * These do not require a live triple store: they read *.rq templates from the
 * resources and bind variables, returning parseable SPARQL strings.
 */
class QanaryTripleStoreConnectorStaticTest {

    private static final URI GRAPH = URI.create("urn:qanary:graph:1");

    private QuerySolutionMap graphBinding() {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource(GRAPH.toASCIIString()));
        bindings.add("targetQuestion", ResourceFactory.createResource("urn:qanary:question:1"));
        bindings.add("application", ResourceFactory.createResource("urn:qanary:component:Test"));
        return bindings;
    }

    @Test
    void readFileFromResourcesReturnsContentForExistingFile() throws IOException {
        String content = QanaryTripleStoreConnector.readFileFromResources("/queries/select_count_all_triples.rq");
        assertNotNull(content);
        assertTrue(content.contains("count"));
    }

    @Test
    void readFileFromResourcesReturnsNullForMissingFile() throws IOException {
        assertNull(QanaryTripleStoreConnector.readFileFromResources("/queries/does_not_exist.rq"));
    }

    @Test
    void selectBuildersBindGraphAndProduceParseableQueries() throws IOException {
        assertTrue(QanaryTripleStoreConnector.getCountAllTriplesInGraph(GRAPH).toLowerCase().contains("count"));
        assertNotNull(QanaryTripleStoreConnector.getAllAnnotationOfAnswerInGraph(GRAPH));
        assertNotNull(QanaryTripleStoreConnector.getHighestScoreAnnotationOfAnswerInGraph(GRAPH));
        assertNotNull(QanaryTripleStoreConnector.getLowestIndexAnnotationOfAnswerInGraph(GRAPH));
        assertNotNull(QanaryTripleStoreConnector.getAllAnnotationOfAnswerSPARQL(GRAPH));
        // the graph IRI must appear in the generated query
        assertTrue(QanaryTripleStoreConnector.getCountAllTriplesInGraph(GRAPH).contains(GRAPH.toASCIIString()));
    }

    @Test
    void insertBuildersProduceUpdateQueries() throws IOException {
        QuerySolutionMap bindings = graphBinding();
        assertTrue(QanaryTripleStoreConnector.insertAnnotationOfAnswerDataType(bindings).contains("INSERT"));
        assertTrue(QanaryTripleStoreConnector.insertAnnotationOfAnswerSPARQL(bindings).contains("INSERT"));
        assertTrue(QanaryTripleStoreConnector.insertAnnotationOfAnswerJson(bindings).contains("INSERT"));
        assertTrue(QanaryTripleStoreConnector.insertAnnotationOfImprovedQuestion(bindings).contains("INSERT"));
        assertTrue(QanaryTripleStoreConnector.insertAnnotationOfTypedLiteral(bindings).contains("INSERT"));
    }

    @Test
    void readFileFromResourcesWithMapHandlesSelectAndUpdateTemplates() throws IOException {
        QuerySolutionMap bindings = graphBinding();
        // SELECT branch
        String select = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                "/queries/select_count_all_triples.rq", bindings);
        assertTrue(select.toUpperCase().contains("SELECT"));
        // UPDATE branch
        String update = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                "/queries/insert_one_AnnotationOfAnswerJson.rq", bindings);
        assertTrue(update.contains("INSERT"));
    }

    @Test
    void readFileFromResourcesWithMapHandlesAskAndConstructTemplates() throws IOException {
        QuerySolutionMap bindings = graphBinding();
        // ASK branch (test-only template)
        String ask = QanaryTripleStoreConnector.readFileFromResourcesWithMap("/queries/test_ask.rq", bindings);
        assertTrue(ask.toUpperCase().contains("ASK"));
        // CONSTRUCT branch (test-only template)
        String construct = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
                "/queries/test_construct.rq", bindings);
        assertTrue(construct.toUpperCase().contains("CONSTRUCT"));
    }

    @Test
    void guardNonEmptyFileFromResourcesAcceptsExistingFileAndRejectsMissing() {
        assertDoesNotThrow(() ->
                QanaryTripleStoreConnector.guardNonEmptyFileFromResources("/queries/select_count_all_triples.rq"));
        assertThrows(RuntimeException.class, () ->
                QanaryTripleStoreConnector.guardNonEmptyFileFromResources("/queries/does_not_exist.rq"));
    }
}
