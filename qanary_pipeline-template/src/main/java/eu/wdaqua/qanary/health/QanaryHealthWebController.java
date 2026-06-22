package eu.wdaqua.qanary.health;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the same information as {@code /actuator/health} at the convenience
 * path {@code /health} (issue #405), so users get the extended Qanary status
 * (version, frontend, components, triplestore — see {@link QanaryHealthIndicator})
 * without knowing the actuator base path.
 */
@RestController
public class QanaryHealthWebController {

    private final HealthEndpoint healthEndpoint;

    public QanaryHealthWebController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/health")
    public HealthComponent health() {
        return healthEndpoint.health();
    }
}
