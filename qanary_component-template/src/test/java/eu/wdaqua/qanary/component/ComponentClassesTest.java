package eu.wdaqua.qanary.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.cli.MissingArgumentException;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.servlet.view.RedirectView;

import eu.wdaqua.qanary.component.exceptions.AmbiguousExtendingComponentClass;
import eu.wdaqua.qanary.component.exceptions.NoExtendingComponentClass;

class ComponentClassesTest {

    @Test
    void ambiguousExtendingComponentClassMentionsType() {
        AmbiguousExtendingComponentClass ex = new AmbiguousExtendingComponentClass(QanaryComponent.class);
        assertTrue(ex.getMessage().contains(QanaryComponent.class.getName()));
        assertTrue(ex.getMessage().contains("multiple"));
    }

    @Test
    void noExtendingComponentClassMentionsType() {
        NoExtendingComponentClass ex = new NoExtendingComponentClass(QanaryComponent.class);
        assertTrue(ex.getMessage().contains(QanaryComponent.class.getName()));
        assertTrue(ex.getMessage().contains("Could not find"));
    }

    @Test
    void swaggerUiRedirectControllerRedirectsToSwaggerUiPage() {
        SwaggerUiRedirectController controller = new SwaggerUiRedirectController();
        RedirectView view = controller.redirectToSwaggerUi();
        assertEquals(SwaggerUiRedirectController.SWAGGER_UI_PAGE, view.getUrl());
    }

    @Test
    void springdocDefaultsArePlacedWithLowestPrecedence() {
        QanarySpringdocDefaultsEnvironmentPostProcessor processor =
                new QanarySpringdocDefaultsEnvironmentPostProcessor();
        ConfigurableEnvironment environment = new StandardEnvironment();
        // application parameter is not used by the implementation
        processor.postProcessEnvironment(environment, null);
        assertEquals("/api-docs", environment.getProperty("springdoc.api-docs.path"));
        assertEquals("/swagger-ui.html", environment.getProperty("springdoc.swagger-ui.path"));
    }

    // ---- QanaryComponentConfiguration ---------------------------------------

    private Environment environmentWithRequiredProperties() {
        Environment env = mock(Environment.class);
        for (String key : new String[] {"server.port", "spring.application.name", "spring.boot.admin.url"}) {
            when(env.containsProperty(key)).thenReturn(true);
            when(env.getProperty(key)).thenReturn("value-of-" + key);
        }
        return env;
    }

    @Test
    void componentConfigurationValidatesWhenRequiredPropertiesPresent() {
        QanaryComponentConfiguration config =
                new QanaryComponentConfiguration(environmentWithRequiredProperties());
        // all required properties present -> no exception, toString() is logged
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(config::validateRequiredArguments);
        assertEquals("value-of-spring.application.name", config.getApplicationName());
        assertTrue(config.toString().contains("spring.application.name"));
    }

    @Test
    void componentConfigurationThrowsWhenRequiredPropertyMissing() {
        Environment env = mock(Environment.class);
        // server.port missing -> validation must fail
        when(env.containsProperty("server.port")).thenReturn(false);
        when(env.containsProperty("spring.application.name")).thenReturn(true);
        when(env.getProperty("spring.application.name")).thenReturn("name");
        when(env.containsProperty("spring.boot.admin.url")).thenReturn(true);
        when(env.getProperty("spring.boot.admin.url")).thenReturn("url");

        QanaryComponentConfiguration config = new QanaryComponentConfiguration(env);
        assertThrows(MissingArgumentException.class, config::validateRequiredArguments);
    }

    @Test
    void componentConfigurationHostIsNullWithoutServletContext() {
        QanaryComponentConfiguration config =
                new QanaryComponentConfiguration(environmentWithRequiredProperties());
        // no active request -> ServletUriComponentsBuilder fails -> getBaseUrl()/getHost() return null
        assertNull(config.getBaseUrl());
        assertNull(config.getHost());
        assertNull(config.getPropertyValue("server.host"));
    }
}
