package eu.wdaqua.qanary.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import eu.wdaqua.qanary.component.SwaggerUiRedirectController;

/**
 * Verifies the pipeline exposes the Swagger UI via the /swagger-ui, /swagger, /openapi
 * and /docs aliases (each redirects to the Swagger UI page).
 * <p>
 * The pipeline depends on the {@code qa.component} framework and component-scans
 * {@code eu.wdaqua.qanary}, so the aliases are provided by the framework's
 * {@link SwaggerUiRedirectController} (there is intentionally no pipeline-specific copy,
 * which would clash with it). This standalone MockMvc test exercises that very controller
 * so it needs neither a triplestore nor the full pipeline context. The /api-docs (OpenAPI
 * JSON) and /swagger-ui/** (UI) endpoints are provided by springdoc in the running pipeline.
 */
class SwaggerUiRedirectControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SwaggerUiRedirectController()).build();

    @Test
    void aliasesRedirectToSwaggerUi() throws Exception {
        for (String path : new String[] {"/swagger-ui", "/swagger", "/openapi", "/docs"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(SwaggerUiRedirectController.SWAGGER_UI_PAGE));
        }
    }
}
