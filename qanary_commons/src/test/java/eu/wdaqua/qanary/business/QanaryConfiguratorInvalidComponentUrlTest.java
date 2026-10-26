package eu.wdaqua.qanary.business;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringFinished;

/**
 * A component whose URL is not a valid URI aborts the process before any
 * component is called. The configurator has no RestTemplate here, so any
 * attempted call would fail the test with an exception.
 */
class QanaryConfiguratorInvalidComponentUrlTest {

    @Test
    void anInvalidComponentUrlAbortsTheProcessBeforeAnyCall() throws Exception {
        QanaryConfigurator configurator = new QanaryConfigurator(null, List.of(), "localhost", 8080,
                URI.create("urn:qanary:ontology"), URI.create("urn:test:endpoint"), mock(QanaryTripleStoreProxy.class));
        QanaryMessage message = new QanaryMessage(URI.create("urn:test:endpoint"), URI.create("urn:graph:1"));

        QanaryQuestionAnsweringFinished result = configurator.callServices(List.of( //
                new QanaryComponent("broken", "http://localhost:8080/ned component", true), //
                new QanaryComponent("never-called", "http://localhost:8081", true)), message);

        assertTrue(result.getCompactProtocol().isEmpty(), "no component was executed");
    }
}
