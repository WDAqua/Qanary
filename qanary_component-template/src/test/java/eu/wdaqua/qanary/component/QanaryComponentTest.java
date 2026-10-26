package eu.wdaqua.qanary.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import com.sun.net.httpserver.HttpServer;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorInMemory;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.explainability.QanaryExplanationData;

/**
 * Unit tests of the {@link QanaryComponent} base class without a Spring context:
 * access to environment and message, the guards against use before a message is
 * set, the utility and question factories, and the call to the explanation service.
 */
class QanaryComponentTest {

    private static final String NAME = "unit-test-component";

    /**
     * A component whose concrete methods are the real ones of {@link QanaryComponent}.
     * Deliberately not a compiled subclass: {@link QanaryServiceControllerRoot} scans the
     * classpath for exactly one {@code QanaryComponent} implementation, and a second one in
     * the test classes would push the integration test onto its "ambiguous" error path.
     */
    private static QanaryComponent component() {
        QanaryComponent component = mock(QanaryComponent.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        ReflectionTestUtils.setField(component, "env", new MockEnvironment().withProperty("spring.application.name", NAME));
        return component;
    }

    private static QanaryMessage message() throws Exception {
        // the endpoint is never contacted: connectors connect lazily
        return new QanaryMessage(new URI("http://localhost:65535/sparql"), new URI("urn:qanary:in"), new URI("urn:qanary:out"));
    }

    @Test
    void applicationNameAndEnvironmentComeFromSpring() {
        QanaryComponent component = component();

        assertEquals(NAME, component.getApplicationName());
        assertEquals(NAME, component.getEnvironment().getProperty("spring.application.name"));
    }

    @Test
    void utilitiesAndQuestionNeedAMessageFirst() {
        QanaryComponent component = component();

        assertNull(component.getQanaryMessage());
        assertThrows(IllegalStateException.class, component::getUtils);
        assertThrows(IllegalStateException.class, component::getQanaryQuestion);
        assertThrows(IllegalStateException.class, component::getQuestion);
    }

    @Test
    void setUtilsConnectsToTheTripleStoreOfTheMessage() throws Exception {
        QanaryComponent component = component();
        QanaryMessage message = message();

        component.setUtils(message);
        QanaryUtils utils = component.getUtils();

        assertEquals(message.getEndpoint(), utils.getEndpoint());
        assertEquals(message.getInGraph(), utils.getInGraph());
        QanaryTripleStoreConnectorQanaryInternal connector =
                assertInstanceOf(QanaryTripleStoreConnectorQanaryInternal.class, utils.getQanaryTripleStoreConnector());
        assertEquals("urn:qanary:" + NAME, connector.getApplicationName()); // the connector stores the name as a URN
    }

    @Test
    void theQuestionOfTheCurrentMessageUsesItsGraphs() throws Exception {
        QanaryComponent component = component();
        QanaryMessage message = message();
        component.setQanaryMessage(message);

        QanaryQuestion<String> question = component.getQanaryQuestion();

        assertSame(message, component.getQanaryMessage());
        assertEquals(message.getInGraph(), question.getInGraph());
        assertEquals(message.getOutGraph(), question.getOutGraph());
        assertEquals(message.getEndpoint(), question.getEndpoint());
        assertEquals(message.getInGraph(), component.getQuestion().getNamedGraph());
    }

    @Test
    void explicitConnectorAndConfiguratorAreUsedAsGiven() throws Exception {
        QanaryComponent component = component();
        QanaryMessage message = message();
        QanaryTripleStoreConnector connector = new QanaryTripleStoreConnectorInMemory();
        QanaryTripleStoreProxy proxy = new QanaryTripleStoreProxy();
        QanaryConfigurator configurator = mock(QanaryConfigurator.class);
        when(configurator.getQanaryTripleStoreConnector()).thenReturn(proxy);

        assertSame(connector, component.getUtils(message, connector).getQanaryTripleStoreConnector());
        assertSame(proxy, component.getQanaryQuestion(message, configurator).getQanaryTripleStoreConnector());
        assertInstanceOf(QanaryTripleStoreConnectorQanaryInternal.class,
                component.getQanaryQuestion(message).getQanaryTripleStoreConnector());
    }

    @Test
    void explainPostsTheDataWithTheComponentNameToTheExplanationService() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/explain", exchange -> {
            received.set(exchange.getRequestMethod() + " " + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "the component found one entity".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            QanaryComponent component = component();
            component.setupWebClient("http://127.0.0.1:" + server.getAddress().getPort());
            QanaryExplanationData data = new QanaryExplanationData("urn:qanary:graph", "urn:qanary:question", "http://pipeline");

            String explanation = component.explain(data);

            assertEquals("the component found one entity", explanation);
            assertEquals(NAME, data.getComponent());
            String request = received.get();
            assertTrue(request.startsWith("POST "), request);
            assertTrue(request.contains("\"component\":\"" + NAME + "\"") && request.contains("urn:qanary:graph"), request);
        } finally {
            server.stop(0);
        }
    }
}
