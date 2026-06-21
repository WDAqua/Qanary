package eu.wdaqua.qanary.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.exceptions.QanaryExceptionServiceCallNotOk;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringFinished;

class QanaryConfiguratorTest {

    private QanaryConfigurator configurator(RestTemplate restTemplate) {
        return new QanaryConfigurator(
                restTemplate,
                List.of("ComponentA", "ComponentB"),
                "localhost",
                8080,
                URI.create("urn:qanary#ontology"),
                URI.create("http://localhost:8890/sparql"),
                mock(QanaryTripleStoreProxy.class));
    }

    private QanaryMessage message() throws URISyntaxException {
        return new QanaryMessage(URI.create("http://localhost:8890/sparql"), URI.create("urn:graph:in"));
    }

    @Test
    void exposesConfigurationViaGetters() {
        QanaryTripleStoreProxy proxy = mock(QanaryTripleStoreProxy.class);
        QanaryConfigurator config = new QanaryConfigurator(
                mock(RestTemplate.class), List.of("A", "B"), "myhost", 9090,
                URI.create("urn:onto"), URI.create("http://ts/sparql"), proxy);

        assertEquals(9090, config.getPort());
        assertEquals("myhost", config.getHost());
        assertEquals(URI.create("urn:onto"), config.getQanaryOntology());
        assertEquals(URI.create("http://ts/sparql"), config.getEndpoint());
        assertSame(proxy, config.getQanaryTripleStoreConnector());
        assertEquals(List.of("A", "B"), config.getDefaultComponentNames());
        assertEquals("A,B", config.getDefaultComponentNamesAsString());

        config.setDefaultComponentNames(List.of("C"));
        assertEquals("C", config.getDefaultComponentNamesAsString());
    }

    @Test
    void callServicesWithEmptyComponentListReturnsImmediately() throws Exception {
        QanaryConfigurator config = configurator(mock(RestTemplate.class));
        QanaryQuestionAnsweringFinished result = config.callServices(Collections.emptyList(), message());
        assertTrue(result.getCompactProtocol().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void callServicesAppendsProtocolForSuccessfulComponentCall() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        QanaryMessage responseBody = message();
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(QanaryMessage.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        QanaryConfigurator config = configurator(restTemplate);
        QanaryComponent component = new QanaryComponent("NER", "http://localhost:8081", true);

        QanaryQuestionAnsweringFinished result = config.callServices(List.of(component), message());
        assertEquals(1, result.getCompactProtocol().size());
        assertTrue(result.getCompactProtocol().get(0).contains("NER"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void callServicesThrowsWhenComponentReturnsNonOkStatus() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(QanaryMessage.class)))
                .thenReturn(new ResponseEntity<>(message(), HttpStatus.INTERNAL_SERVER_ERROR));

        QanaryConfigurator config = configurator(restTemplate);
        QanaryComponent component = new QanaryComponent("NED", "http://localhost:8082", true);

        assertThrows(QanaryExceptionServiceCallNotOk.class,
                () -> config.callServices(List.of(component), message()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void callServicesWrapsUnexpectedExceptionAsServiceCallNotOk() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(QanaryMessage.class)))
                .thenThrow(new RuntimeException("connection refused"));

        QanaryConfigurator config = configurator(restTemplate);
        QanaryComponent component = new QanaryComponent("LINKER", "http://localhost:8083", true);

        assertThrows(QanaryExceptionServiceCallNotOk.class,
                () -> config.callServices(List.of(component), message()));
    }
}
