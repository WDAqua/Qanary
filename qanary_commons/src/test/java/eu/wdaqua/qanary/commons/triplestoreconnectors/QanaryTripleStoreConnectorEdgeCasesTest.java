package eu.wdaqua.qanary.commons.triplestoreconnectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Edge cases of the shared {@link QanaryTripleStoreConnector} behaviour that the
 * template tests do not reach: debug output of template bindings, blank and
 * unreadable templates, and the runtime log of slow queries; plus the operations
 * the in-memory connector deliberately does not support.
 */
class QanaryTripleStoreConnectorEdgeCasesTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(QanaryTripleStoreConnector.class);
    private final ListAppender<ILoggingEvent> log = new ListAppender<>();
    private Level previousLevel;

    @BeforeEach
    void captureLog() {
        previousLevel = logger.getLevel();
        log.start();
        logger.addAppender(log);
    }

    @AfterEach
    void releaseLog() {
        logger.detachAppender(log);
        logger.setLevel(previousLevel);
    }

    private List<String> messages(Level level) {
        return log.list.stream().filter(e -> e.getLevel() == level).map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    void debugLoggingListsTheBindingsAppliedToATemplate() throws IOException {
        logger.setLevel(Level.DEBUG);
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource("urn:qanary:graph:debug"));

        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap("/queries/select_count_all_triples.rq",
                bindings);

        assertTrue(query.contains("urn:qanary:graph:debug"));
        assertTrue(messages(Level.DEBUG).stream().anyMatch(m -> m.contains("graph -> urn:qanary:graph:debug")),
                "the debug output names each binding");
    }

    @Test
    void blankTemplatesAreRejected() {
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> QanaryTripleStoreConnector.guardNonEmptyFileFromResources("/queries/test_blank.rq"));
        assertEquals("no content: /queries/test_blank.rq", failure.getMessage());
    }

    @Test
    void unreadableTemplatesAreRejected() {
        try (MockedStatic<QanaryTripleStoreConnector> connector =
                     mockStatic(QanaryTripleStoreConnector.class, CALLS_REAL_METHODS)) {
            connector.when(() -> QanaryTripleStoreConnector.readFileFromResources(anyString()))
                    .thenThrow(new IOException("disk error"));

            RuntimeException failure = assertThrows(RuntimeException.class,
                    () -> QanaryTripleStoreConnector.guardNonEmptyFileFromResources("/queries/select_count_all_triples.rq"));
            assertEquals("not available: /queries/select_count_all_triples.rq", failure.getMessage());
        }
    }

    @Test
    void slowQueriesAreLoggedAsWarnings() {
        QanaryTripleStoreConnectorInMemory connector = new QanaryTripleStoreConnectorInMemory();
        log.list.clear(); // the constructor logs its own warning

        connector.logTime(10_001, "SELECT slow");
        connector.logTime(10_000, "SELECT fast");
        connector.logTime(42, "SELECT on endpoint", "urn:endpoint");

        assertEquals(List.of("runtime measurement: 10001 ms for SELECT slow (was very long)"), messages(Level.WARN));
        assertTrue(messages(Level.INFO).contains("runtime measurement: 10000 ms for SELECT fast"));
        assertTrue(messages(Level.INFO).contains("runtime measurement: 42 ms on urn:endpoint for SELECT on endpoint"));
    }

    @Test
    void inMemoryConnectorDoesNotSupportGraphUpdatesOrConstruct() {
        QanaryTripleStoreConnectorInMemory connector = new QanaryTripleStoreConnectorInMemory();

        assertThrows(NotImplementedException.class,
                () -> connector.update("INSERT DATA { <urn:s> <urn:p> <urn:o> }", URI.create("urn:g")));
        assertNull(connector.construct("CONSTRUCT WHERE { ?s ?p ?o }"));
        assertNull(connector.construct("CONSTRUCT WHERE { ?s ?p ?o }", URI.create("urn:g")));
        assertEquals("This is an in-memory triplestore. For now, it is intended to be used only in unit tests.",
                connector.getFullEndpointDescription());
    }
}
