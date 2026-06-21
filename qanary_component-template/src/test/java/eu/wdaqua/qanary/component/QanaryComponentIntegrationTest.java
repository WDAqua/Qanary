package eu.wdaqua.qanary.component;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import eu.wdaqua.qanary.commons.QanaryMessage;

/**
 * Integration test that boots a real Qanary component Spring context (the
 * {@code qa.component} framework controllers + a concrete {@link QanaryComponent})
 * and drives its HTTP endpoints through MockMvc. Exercises the component web layer
 * end to end (root description page, the {@code /annotatequestion} processing
 * endpoint, the RDF component description and the Swagger UI redirects) without an
 * external triple store or Spring Boot Admin server.
 */
@SpringBootTest(classes = QanaryComponentIntegrationTest.TestComponentApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.application.name=qanary-integration-test-component",
        "spring.application.description=integration test component",
        "server.port=0",
        "spring.boot.admin.url=http://localhost:65535",
        "spring.boot.admin.client.url=http://localhost:65535",
        "spring.boot.admin.client.enabled=false",
        "spring.boot.admin.client.auto-registration=false",
        "pipeline.as.component=true",
        "springdoc.api-docs.path=/api-docs"
})
class QanaryComponentIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void rootServesTheComponentDescriptionPage() throws Exception {
        // QanaryServiceControllerRoot: renders description.html, detecting the
        // concrete QanaryComponent implementation via classpath reflection
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void annotatequestionGetServesTheOnlyPostHint() throws Exception {
        // QanaryServiceController GET fallback -> only-post-is-allowed.html
        mvc.perform(get("/annotatequestion")).andExpect(status().isOk());
    }

    @Test
    void descriptionPageRenders() throws Exception {
        mvc.perform(get("/description")).andExpect(status().isOk());
    }

    @Test
    void rdfComponentDescriptionIsServed() throws Exception {
        // QanaryComponentDescriptionController: builds and serialises the RDF
        // service description of the component
        mvc.perform(get("/component-description").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUiAliasRedirects() throws Exception {
        mvc.perform(get("/swagger-ui")).andExpect(status().is3xxRedirection());
    }

    @Test
    void annotatequestionPostRunsTheComponentProcess() throws Exception {
        // a valid Qanary message; the test component's process() echoes it back
        String message = "{\"values\":{"
                + "\"urn:qanary#endpoint\":\"http://localhost:65535/sparql\","
                + "\"urn:qanary#inGraph\":\"urn:graph:in\","
                + "\"urn:qanary#outGraph\":\"urn:graph:out\""
                + "}}";

        mvc.perform(post("/annotatequestion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(message)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /**
     * Minimal Qanary component implementation so the framework controllers have a
     * concrete component to wire and reflect on.
     */
    static class TestQanaryComponent extends QanaryComponent {
        @Override
        public QanaryMessage process(QanaryMessage myQanaryMessage) {
            return myQanaryMessage;
        }
    }

    @SpringBootApplication
    static class TestComponentApplication {
        @Bean
        QanaryComponent testQanaryComponent() {
            return new TestQanaryComponent();
        }
    }
}
