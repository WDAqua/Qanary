package eu.wdaqua.qanary.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.MissingArgumentException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import eu.wdaqua.qanary.exceptions.MissingRequiredConfiguration;
import eu.wdaqua.qanary.exceptions.TripleStoreNotProvided;

/**
 * Checks the pipeline configuration: required parameters, defaults derived
 * from other parameters, and the rejection of unusable values.
 */
class QanaryPipelineConfigurationCoverageTest {

    /**
     * an environment whose properties come only from a source named like Spring Boot's
     * application config: OS environment variables and JVM system properties (e.g. a
     * SERVER_PORT set on a CI runner) are removed so they cannot change the outcome
     */
    static StandardEnvironment environment(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources()
                .addFirst(new MapPropertySource("applicationConfig: [classpath:/application.properties]", properties));
        return environment;
    }

    static Map<String, Object> required() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("server.host", "http://127.0.0.1");
        properties.put("server.port", "8080");
        properties.put("qanary.ontology", "https://example.qanary/qanary.owl");
        return properties;
    }

    @AfterEach
    void resetRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void completeConfigurationIsAcceptedAndExposed() {
        Map<String, Object> properties = required();
        properties.put("qanary.process.allow-insert-queries", "true");
        properties.put("qanary.process.allow-additional-triples", "false");
        properties.put("qanary.process.additional-triples-directory", "/tmp/qanary-triples");
        properties.put("qanary.components", "NED-DBpediaSpotlight ,  QB-Simple,REL");
        properties.put("unrelated.property", "ignored");
        StandardEnvironment environment = environment(properties);
        QanaryPipelineConfiguration configuration = new QanaryPipelineConfiguration(environment);

        assertDoesNotThrow(configuration::validateAfterInitialization);
        assertEquals("http://127.0.0.1", configuration.getHost());
        assertEquals(8080, configuration.getPort());
        assertTrue(configuration.getInsertQueriesAllowed());
        assertFalse(configuration.getAdditionalTriplesAllowed());
        assertEquals("/tmp/qanary-triples", configuration.getAdditionalTriplesDirectory());
        assertEquals(List.of("NED-DBpediaSpotlight", "QB-Simple", "REL"), configuration.getPredefinedComponents());
        assertEquals(URI.create("https://example.qanary/qanary.owl"), configuration.getQanaryOntologyAsURI());
        assertSame(environment, configuration.getEnv());

        Map<String, Object> known = configuration.getAllKnownProperties(environment);
        assertEquals("8080", known.get("server.port"));
        assertFalse(known.containsKey("unrelated.property"));
        assertTrue(configuration.toString().contains("qanary.components"));
    }

    @Test
    void missingRequiredParameterIsReported() {
        Map<String, Object> properties = required();
        properties.remove("server.port");
        QanaryPipelineConfiguration configuration = new QanaryPipelineConfiguration(environment(properties));

        MissingArgumentException e = assertThrows(MissingArgumentException.class,
                configuration::validateAfterInitialization);
        assertTrue(e.getMessage().contains("server.port"), e.getMessage());
        assertNull(configuration.getPort());
    }

    @Test
    void triplestoreDefaultsToTheEmbeddedSparqlEndpoint() throws Exception {
        QanaryPipelineConfiguration configuration = new QanaryPipelineConfiguration(environment(required()));

        String expected = "http://127.0.0.1:8080/" + QanarySparqlProtocolController.SPARQL_ENDPOINT;
        assertEquals(expected, configuration.getTriplestore());
        assertEquals(expected, configuration.getProperty("qanary.triplestore"));
        assertEquals(URI.create(expected), configuration.getTriplestoreAsURI());
    }

    @Test
    void configuredTriplestoreIsUsedAsIs() throws Exception {
        Map<String, Object> properties = required();
        properties.put("qanary.triplestore", "http://127.0.0.1:8890/sparql");
        QanaryPipelineConfiguration configuration = new QanaryPipelineConfiguration(environment(properties));

        assertEquals(URI.create("http://127.0.0.1:8890/sparql"), configuration.getTriplestoreAsURI());
    }

    @Test
    void emptyTriplestoreIsRejected() {
        Map<String, Object> properties = required();
        properties.put("qanary.triplestore", "");
        QanaryPipelineConfiguration configuration = new QanaryPipelineConfiguration(environment(properties));

        assertThrows(MissingRequiredConfiguration.class, configuration::getTriplestore);
    }

    @Test
    void triplestoreThatIsNoUriIsRejected() {
        Map<String, Object> properties = required();
        properties.put("qanary.triplestore", "http://not a uri/sparql");
        QanaryPipelineConfiguration configuration = new QanaryPipelineConfiguration(environment(properties));

        TripleStoreNotProvided e = assertThrows(TripleStoreNotProvided.class, configuration::getTriplestoreAsURI);
        assertTrue(e.getMessage().contains("http://not a uri/sparql"), e.getMessage());
    }

    @Test
    void ontologyThatIsNoUriIsRejected() {
        Map<String, Object> properties = required();
        properties.put("qanary.ontology", "not a uri");
        QanaryPipelineConfiguration configuration = new QanaryPipelineConfiguration(environment(properties));

        assertThrows(MissingRequiredConfiguration.class, configuration::getQanaryOntologyAsURI);
    }

    @Test
    void questionsDirectoryDefaultsToTheWorkingDirectory() {
        QanaryPipelineConfiguration configuration = new QanaryPipelineConfiguration(environment(required()));
        assertEquals(System.getProperty("user.dir"), configuration.getProperty("qanary.questions.directory"));
        assertTrue(configuration.getPredefinedComponents().isEmpty());

        Map<String, Object> properties = required();
        properties.put("qanary.questions.directory", "/srv/questions");
        assertEquals("/srv/questions", new QanaryPipelineConfiguration(environment(properties)).getQuestionsDirectory());
    }

    @Test
    void baseUrlIsTakenFromTheCurrentRequest() {
        QanaryPipelineConfiguration configuration = new QanaryPipelineConfiguration(environment(required()));
        // outside of a request there is no base URL
        assertNull(configuration.getBaseUrl());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/components");
        request.setServerName("qanary.local");
        request.setServerPort(8443);
        request.setScheme("https");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertEquals("https://qanary.local:8443", configuration.getBaseUrl());
    }
}
