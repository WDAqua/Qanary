package eu.wdaqua.qanary.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The configuration endpoint shows the configurable pipeline properties
 * (never credentials, host, port or the component list) and writes changed
 * values to the local properties file.
 */
class QanaryPipelineConfigurationControllerCoverageTest {

    private static QanaryPipelineConfigurationController controller(Map<String, Object> extra) {
        Map<String, Object> properties = QanaryPipelineConfigurationCoverageTest.required();
        properties.putAll(extra);
        StandardEnvironment environment = QanaryPipelineConfigurationCoverageTest.environment(properties);
        return new QanaryPipelineConfigurationController(environment, new QanaryPipelineConfiguration(environment));
    }

    @Test
    void configurablePropertiesAreShownWithoutProtectedOnes() throws Exception {
        QanaryPipelineConfigurationController controller = controller(Map.of( //
                "qanary.triplestore", "http://127.0.0.1:8890/sparql", //
                "qanary.components", "NED-DBpediaSpotlight", //
                "spring.application.name", "MyQanaryPipeline", //
                "configuration.password", "secret"));

        ResponseEntity<String> response = controller.getConfigurablePipelineProperties();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = new ObjectMapper().readTree(response.getBody());
        assertEquals("http://127.0.0.1:8890/sparql", json.get("qanary.triplestore").asText());
        assertEquals("MyQanaryPipeline", json.get("spring.application.name").asText());
        for (String hidden : List.of("server.host", "server.port", "qanary.components", "configuration.password")) {
            assertFalse(json.has(hidden), hidden + " must not be configurable: " + json);
        }
    }

    @Test
    void changedPropertiesAreWrittenToTheLocalPropertiesFile(@TempDir Path directory) throws Exception {
        Path localProperties = directory.resolve("application.local.properties");
        Files.writeString(localProperties, "qanary.triplestore=http://old:8890/sparql\n");
        QanaryPipelineConfigurationController controller = controller(
                Map.of("spring.config.location", localProperties.toString()));

        controller.updateLocalPipelineProperties(
                "{\"qanary.triplestore\": \"http://127.0.0.1:8890/sparql\", \"qanary.process.allow-insert-queries\": true}");

        List<String> lines = Files.readAllLines(localProperties);
        assertEquals(2, lines.size(), lines.toString());
        assertTrue(lines.contains("qanary.triplestore=http://127.0.0.1:8890/sparql"), lines.toString());
        assertTrue(lines.contains("qanary.process.allow-insert-queries=true"), lines.toString());
    }

    @Test
    void localPropertiesFileIsCreatedIfMissing(@TempDir Path directory) throws Exception {
        Path localProperties = directory.resolve("application.local.properties");
        QanaryPipelineConfigurationController controller = controller(
                Map.of("spring.config.location", localProperties.toString()));

        controller.updateLocalPipelineProperties("{\"qanary.process.allow-additional-triples\": false}");

        assertEquals(List.of("qanary.process.allow-additional-triples=false"), Files.readAllLines(localProperties));
    }

    @Test
    void unwritableLocationLeavesNoFileBehind(@TempDir Path directory) {
        Path unreachable = directory.resolve("missing").resolve("sub").resolve("application.local.properties");
        QanaryPipelineConfigurationController controller = controller(
                Map.of("spring.config.location", unreachable.toString()));

        // errors are logged, the request itself does not fail
        controller.updateLocalPipelineProperties("{\"qanary.triplestore\": \"http://127.0.0.1:8890/sparql\"}");

        assertFalse(Files.exists(unreachable));
    }

    @Test
    void invalidJsonIsRejected(@TempDir Path directory) {
        QanaryPipelineConfigurationController controller = controller(
                Map.of("spring.config.location", directory.resolve("x.properties").toString()));

        assertThrows(JSONException.class, () -> controller.updateLocalPipelineProperties("not json"));
    }
}
