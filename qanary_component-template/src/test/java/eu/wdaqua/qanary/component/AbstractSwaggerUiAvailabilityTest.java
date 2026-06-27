package eu.wdaqua.qanary.component;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.ResponseEntity;

/**
 * Shared test that verifies the Swagger UI / OpenAPI description is reachable through
 * all expected paths of a Qanary component:
 * <ul>
 *   <li>{@code /swagger-ui}, {@code /swagger}, {@code /openapi}, {@code /docs} → the Swagger UI</li>
 *   <li>{@code /api-docs} → the OpenAPI JSON description</li>
 * </ul>
 * Provided by the {@code qa.component} framework (in its test-jar). A component enables it with:
 * <pre>
 * &#64;SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * class SwaggerUiAvailabilityTest extends eu.wdaqua.qanary.component.AbstractSwaggerUiAvailabilityTest {}
 * </pre>
 */
// Spring Boot 4 no longer provides a TestRestTemplate bean from @SpringBootTest alone.
@AutoConfigureTestRestTemplate
public abstract class AbstractSwaggerUiAvailabilityTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Test
    void swaggerUiReachableViaAllAliasPaths() {
        for (String path : new String[] {"/swagger-ui", "/swagger", "/openapi", "/docs"}) {
            ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);
            if (response.getStatusCode().is3xxRedirection()) {
                assertNotNull(response.getHeaders().getLocation(), path + " should redirect to the Swagger UI");
                assertTrue(response.getHeaders().getLocation().toString().contains("swagger-ui"),
                        path + " should redirect to the Swagger UI but went to " + response.getHeaders().getLocation());
            } else {
                assertTrue(response.getStatusCode().is2xxSuccessful(),
                        path + " should serve / redirect to the Swagger UI but returned " + response.getStatusCode());
                assertTrue(response.getBody() != null && response.getBody().toLowerCase().contains("swagger"),
                        path + " should serve the Swagger UI HTML");
            }
        }
    }

    @Test
    void openApiDescriptionReachableAtApiDocs() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api-docs", String.class);
        assertTrue(response.getStatusCode().is2xxSuccessful(),
                "/api-docs should serve the OpenAPI description but returned " + response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("openapi"),
                "/api-docs should serve the OpenAPI JSON description");
    }
}
