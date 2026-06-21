package eu.wdaqua.qanary.component;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Provides framework-wide springdoc defaults so the OpenAPI description is served at
 * {@code /api-docs} (and the Swagger UI at {@code /swagger-ui/**}) for <em>every</em>
 * Qanary component, even those that do not set these properties themselves. Registered
 * via {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports}.
 *
 * <p>The property source is added with lowest precedence ({@code addLast}), so a
 * component's own {@code application.properties} can still override it.
 */
public class QanarySpringdocDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("springdoc.api-docs.path", "/api-docs");
        defaults.put("springdoc.swagger-ui.path", "/swagger-ui.html");
        environment.getPropertySources().addLast(new MapPropertySource("qanarySpringdocDefaults", defaults));
    }
}
