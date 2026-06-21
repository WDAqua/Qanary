package eu.wdaqua.qanary.commons.triplestoreconnectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

/**
 * Runs the {@link AbstractQanaryTripleStoreConnectorContract} against a real
 * Virtuoso instance (the project's own image) via Testcontainers, plus a couple of
 * Virtuoso-specific checks (CONSTRUCT, endpoint description) that the in-memory
 * connector does not support. Skipped automatically without a Docker daemon.
 */
@Testcontainers(disabledWithoutDocker = true)
class VirtuosoConnectorContractIT extends AbstractQanaryTripleStoreConnectorContract {

    private static final int ISQL_PORT = 1111;
    private static final int HTTP_PORT = 8890;

    @Container
    static final GenericContainer<?> VIRTUOSO = new GenericContainer<>("wseresearch/qanary-virtuoso:latest")
            .withExposedPorts(ISQL_PORT, HTTP_PORT)
            .withEnv("DBA_PASSWORD", "dba")
            .withEnv("VIRTUOSO_ADMIN_USER", "admin")
            .withEnv("VIRTUOSO_ADMIN_PASSWORD", "admin")
            .withEnv("VIRTUOSO_RW_USER", "rw")
            .withEnv("VIRTUOSO_RW_PASSWORD", "rw")
            .withEnv("VIRTUOSO_RO_USER", "ro")
            .withEnv("VIRTUOSO_RO_PASSWORD", "ro")
            .waitingFor(Wait.forHttp("/sparql").forPort(HTTP_PORT).forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private static QanaryTripleStoreConnectorVirtuoso connector;

    @BeforeAll
    static void connect() {
        String jdbcUrl = "jdbc:virtuoso://" + VIRTUOSO.getHost() + ":" + VIRTUOSO.getMappedPort(ISQL_PORT);
        connector = new QanaryTripleStoreConnectorVirtuoso(jdbcUrl, "rw", "rw", 30);
    }

    @Override
    protected QanaryTripleStoreConnector connector() {
        return connector;
    }

    // ---- documented deviations from the contract -----------------------------
    // The VirtGraph (virtuoso-jena) driver does not make a just-inserted triple
    // visible to an immediate SELECT for a specific subject on the same connection
    // (CONSTRUCT and the count query do see it). This is a known quirk -- see this
    // package's README.adoc -- so these contract methods are explicitly disabled
    // for Virtuoso rather than silently weakened for every connector.

    @Override
    @Disabled("VirtGraph: a just-inserted triple is not visible to an immediate SELECT on the same connection (see README.adoc)")
    @Test
    void insertedTripleIsSelectable() {
    }

    @Override
    @Disabled("VirtGraph: a just-inserted triple is not visible to an immediate SELECT on the same connection (see README.adoc)")
    @Test
    void multipleInsertsAreAllSelectable() {
    }

    // ---- Virtuoso-specific (not part of the cross-connector contract) --------

    @Test
    void exposesEndpointDescription() {
        assertNotNull(connector.getVirtuosoUrl());
        assertNotNull(connector.getFullEndpointDescription());
    }

    @Test
    void constructReturnsInsertedTriple() throws SparqlQueryFailed {
        String s = "urn:qanary:contract:construct:" + UUID.randomUUID();
        connector.update("INSERT DATA { GRAPH <" + GRAPH + "> { <" + s + "> <urn:p> <urn:o> . } }");
        Model model = connector.construct(
                "CONSTRUCT { <" + s + "> <urn:p> <urn:o> } WHERE { GRAPH <" + GRAPH + "> { <" + s + "> <urn:p> ?o } }",
                GRAPH);
        assertNotNull(model);
        assertFalse(model.isEmpty(), "CONSTRUCT should return the inserted triple");
    }
}
