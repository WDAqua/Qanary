package eu.wdaqua.qanary.commons.triplestoreconnectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.complexible.stardog.api.Connection;
import com.complexible.stardog.api.ConnectionPool;
import com.complexible.stardog.api.UpdateQuery;
import com.complexible.stardog.jena.SDJenaFactory;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

/**
 * Tests {@link QanaryTripleStoreConnectorStardog} without a Stardog server. The
 * connector is built with its real configuration code; the connection pool is
 * replaced by a stub whose connections are exposed as an in-memory Jena model
 * (where the Stardog client would expose the remote database), so queries are
 * really parsed and evaluated by the connector's own code path.
 */
class QanaryTripleStoreConnectorStardogTest {

    private static final URI SERVER = URI.create("http://127.0.0.1:5820");

    private final ConnectionPool pool = mock(ConnectionPool.class);
    private final Connection connection = mock(Connection.class);
    private final Model database = ModelFactory.createDefaultModel();
    private MockedStatic<SDJenaFactory> jenaFactory;
    private QanaryTripleStoreConnectorStardog connector;

    @BeforeEach
    void setUp() {
        connector = new QanaryTripleStoreConnectorStardog(SERVER, "admin", "secret", "qanary", true, 0, 10, 60, 5);
        assertNotNull(connector.getConnectionPool(), "the constructor creates a connection pool");
        connector.getConnectionPool().shutdown(); // the real pool is replaced by the stub below
        connector.setConnectionPool(pool);
        when(pool.obtain()).thenReturn(connection);
        database.add(ResourceFactory.createResource("urn:s"), ResourceFactory.createProperty("urn:p"), "o1");
        database.add(ResourceFactory.createResource("urn:s"), ResourceFactory.createProperty("urn:p"), "o2");
        jenaFactory = mockStatic(SDJenaFactory.class);
        jenaFactory.when(() -> SDJenaFactory.createModel(connection)).thenReturn(database);
    }

    @AfterEach
    void tearDown() {
        jenaFactory.close();
    }

    @Test
    void keepsItsConfigurationAndDescribesTheEndpoint() {
        assertEquals(SERVER, connector.getUrl());
        assertEquals("admin", connector.getUsername());
        assertEquals("secret", connector.getPassword());
        assertEquals("qanary", connector.getDatabase());
        assertTrue(connector.isReasoningType());
        assertEquals("'" + SERVER + "' with database 'qanary'", connector.getFullEndpointDescription());
    }

    @Test
    void selectEvaluatesTheQueryOnAPooledConnectionAndReturnsItToThePool() throws Exception {
        ResultSet results = connector.select("SELECT ?o WHERE { <urn:s> <urn:p> ?o } ORDER BY ?o");

        assertTrue(results instanceof ResultSetRewindable, "results stay readable after the connection is released");
        List<String> values = new ArrayList<>();
        results.forEachRemaining(row -> values.add(row.getLiteral("o").getString()));
        assertEquals(List.of("o1", "o2"), values);
        verify(pool).release(connection);
    }

    @Test
    void askEvaluatesTheQueryOnAPooledConnection() throws Exception {
        assertTrue(connector.ask("ASK { <urn:s> <urn:p> \"o1\" }"));
        assertFalse(connector.ask("ASK { <urn:s> <urn:p> \"missing\" }"));
    }

    @Test
    void failingQueriesAreReportedWithTheEndpoint() {
        Model broken = mock(Model.class);
        when(broken.getGraph()).thenThrow(new IllegalStateException("connection lost"));
        jenaFactory.when(() -> SDJenaFactory.createModel(connection)).thenReturn(broken);

        SparqlQueryFailed select = assertThrows(SparqlQueryFailed.class,
                () -> connector.select("SELECT * WHERE { ?s ?p ?o }"));
        SparqlQueryFailed ask = assertThrows(SparqlQueryFailed.class,
                () -> connector.ask("ASK { ?s ?p ?o }"));

        for (SparqlQueryFailed failure : List.of(select, ask)) {
            assertEquals(SERVER.toString(), failure.getTriplestore());
            assertTrue(failure.getMessage().contains("connection lost"));
        }
        assertEquals("SELECT * WHERE { ?s ?p ?o }", select.getSparqlQuery());
    }

    @Test
    void updateWithGraphTargetsTheGraphAndReleasesTheConnection() throws Exception {
        UpdateQuery update = mock(UpdateQuery.class);
        String sparql = "INSERT DATA { <urn:s> <urn:p> <urn:o> }";
        when(connection.update(sparql, "urn:qanary:graph")).thenReturn(update);

        connector.update(sparql, URI.create("urn:qanary:graph"));

        verify(update).execute();
        verify(connection, never()).update(sparql);
        verify(pool).release(connection);
    }

    @Test
    void updateWithoutGraphUsesTheDefaultGraph() throws Exception {
        UpdateQuery update = mock(UpdateQuery.class);
        String sparql = "INSERT DATA { <urn:s> <urn:p> <urn:o> }";
        when(connection.update(sparql)).thenReturn(update);

        connector.update(sparql);

        verify(update).execute();
        verify(connection, never()).update(any(String.class), any(String.class));
        verify(pool).release(connection);
    }

    @Test
    void constructIsNotSupported() {
        assertNull(connector.construct("CONSTRUCT WHERE { ?s ?p ?o }"));
        assertNull(connector.construct("CONSTRUCT WHERE { ?s ?p ?o }", URI.create("urn:qanary:graph")));
    }

    @Test
    void reconnectingCreatesAFreshPool() {
        connector.connect();

        assertNotNull(connector.getConnectionPool());
        assertFalse(connector.getConnectionPool() == pool, "connect() replaces the pool");
        connector.getConnectionPool().shutdown();
        connector.setConnectionPool(pool);
        assertSame(pool, connector.getConnectionPool());
    }
}
