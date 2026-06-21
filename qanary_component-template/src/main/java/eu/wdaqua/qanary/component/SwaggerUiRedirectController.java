package eu.wdaqua.qanary.component;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Makes the springdoc Swagger UI reachable through a set of convenient aliases.
 * Every Qanary component (which depends on this {@code qa.component} framework and
 * component-scans {@code eu.wdaqua.qanary}) therefore exposes the Swagger UI at:
 * <ul>
 *   <li>{@code /swagger-ui} (springdoc serves the UI itself under {@code /swagger-ui/**})</li>
 *   <li>{@code /swagger}</li>
 *   <li>{@code /openapi}</li>
 *   <li>{@code /docs}</li>
 * </ul>
 * all of which redirect to the actual Swagger UI page. The machine-readable OpenAPI
 * description stays at {@code /api-docs} (served by springdoc; see
 * {@link QanarySpringdocDefaultsEnvironmentPostProcessor}).
 */
@Controller
public class SwaggerUiRedirectController {

    /** the page springdoc serves the Swagger UI from */
    public static final String SWAGGER_UI_PAGE = "/swagger-ui/index.html";

    @GetMapping({"/swagger-ui", "/swagger", "/openapi", "/docs"})
    public RedirectView redirectToSwaggerUi() {
        return new RedirectView(SWAGGER_UI_PAGE);
    }
}
