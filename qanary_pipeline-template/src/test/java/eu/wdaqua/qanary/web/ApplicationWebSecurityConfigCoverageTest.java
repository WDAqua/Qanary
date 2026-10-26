package eu.wdaqua.qanary.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Access modes of the pipeline's web security that the full application tests
 * do not use: "disallow" closes the configuration pages, unknown modes stop
 * the start of the pipeline.
 */
class ApplicationWebSecurityConfigCoverageTest {

    private static AnnotationConfigWebApplicationContext securityContext(String access) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("test", Map.of("configuration.access", access)));
        context.register(ApplicationWebSecurityConfig.class);
        return context;
    }

    @Test
    void disallowedAccessClosesConfigurationPagesButNotTheRest() throws Exception {
        try (AnnotationConfigWebApplicationContext context = securityContext("disallow")) {
            context.refresh();
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

            mvc.perform(get("/")).andExpect(status().isForbidden());
            mvc.perform(get("/configuration")).andExpect(status().isForbidden());
            mvc.perform(get("/applications")).andExpect(status().isForbidden());
            // everything else passes security (no handler exists in this test context)
            mvc.perform(get("/qa")).andExpect(status().isNotFound());
        }
    }

    @Test
    void unknownAccessModePreventsTheStart() {
        try (AnnotationConfigWebApplicationContext context = securityContext("open-for-everyone")) {
            BeanCreationException e = assertThrows(BeanCreationException.class, context::refresh);
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            assertEquals("undefined access type", cause.getMessage());
        }
    }
}
