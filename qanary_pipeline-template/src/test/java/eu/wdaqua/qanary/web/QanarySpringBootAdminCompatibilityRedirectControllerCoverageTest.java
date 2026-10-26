package eu.wdaqua.qanary.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;

import eu.wdaqua.qanary.CoverageHttpStub;

/**
 * Components using the Spring Boot Admin v1 client register at
 * /api/applications; the pipeline forwards the registration to the v2
 * endpoint /instances of itself and returns the new instance id.
 */
class QanarySpringBootAdminCompatibilityRedirectControllerCoverageTest {

    @Test
    void legacyRegistrationIsForwardedToTheInstancesEndpoint() throws Exception {
        try (CoverageHttpStub adminServer = new CoverageHttpStub()) {
            adminServer.respond("/instances", 201, "application/json", "{\"id\":\"4711abcd\"}");
            QanarySpringBootAdminCompatibilityRedirectController controller = new QanarySpringBootAdminCompatibilityRedirectController();
            controller.environment = new MockEnvironment() //
                    .withProperty("server.host", "http://127.0.0.1") //
                    .withProperty("server.port", String.valueOf(adminServer.port()));
            String registration = "{\"name\":\"NED-DBpediaSpotlight\",\"serviceUrl\":\"http://127.0.0.1:9001/\"}";

            ResponseEntity<QanarySpringBootAdminCompatibilityRedirectController.Result> response = controller
                    .registrationOfOldSpringBootAdminClient(registration);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertEquals("4711abcd", response.getBody().getId());
            CoverageHttpStub.RecordedRequest forwarded = adminServer.requests().get(0);
            assertEquals("POST", forwarded.method());
            assertEquals(registration, forwarded.body());
            assertEquals("application/json", forwarded.contentType());
        }
    }
}
