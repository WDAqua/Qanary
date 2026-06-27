package eu.wdaqua.qanary.health;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import eu.wdaqua.qanary.QanaryComponentRegistrationChangeNotifier;
import eu.wdaqua.qanary.business.QanaryConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extended Qanary health information exposed under {@code /actuator/health}
 * (and mirrored at {@code /health} by {@link QanaryHealthWebController}), per
 * issue #405. Contributes, under the {@code qanary} key:
 * <ul>
 *   <li>{@code version} – the running Qanary pipeline version (from build-info / the pom)</li>
 *   <li>{@code frontend} – {@code {availability, url}} of the embedded /qa frontend</li>
 *   <li>{@code components} – the currently registered Qanary components and their info</li>
 *   <li>{@code triplestore} – {@code {availability}} verified via a trivial SPARQL ASK</li>
 * </ul>
 * The overall status is reported DOWN when the triplestore (essential to the
 * pipeline) cannot be reached.
 */
@Component("qanary")
public class QanaryHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(QanaryHealthIndicator.class);

    private final QanaryComponentRegistrationChangeNotifier componentRegistry;
    private final QanaryConfigurator qanaryConfigurator;
    private final ObjectProvider<BuildProperties> buildProperties;
    private final String frontendUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    public QanaryHealthIndicator( //
            QanaryComponentRegistrationChangeNotifier componentRegistry, //
            QanaryConfigurator qanaryConfigurator, //
            ObjectProvider<BuildProperties> buildProperties, //
            @Value("${server.host:http://localhost}") String serverHost, //
            @Value("${server.port:8080}") String serverPort) {
        this.componentRegistry = componentRegistry;
        this.qanaryConfigurator = qanaryConfigurator;
        this.buildProperties = buildProperties;
        String host = serverHost.endsWith("/") ? serverHost.substring(0, serverHost.length() - 1) : serverHost;
        this.frontendUrl = host + ":" + serverPort + "/qa";
    }

    @Override
    public Health health() {
        Health.Builder health = Health.up();
        health.withDetail("version", version());

        boolean frontendAvailable = isReachable(frontendUrl);
        Map<String, Object> frontend = new LinkedHashMap<>();
        frontend.put("availability", frontendAvailable);
        frontend.put("url", frontendUrl);
        health.withDetail("frontend", frontend);

        health.withDetail("components", components());

        boolean triplestoreAvailable = isTriplestoreAvailable();
        health.withDetail("triplestore", Map.of("availability", triplestoreAvailable));

        // the triplestore is essential; surface DOWN when it is unreachable
        if (!triplestoreAvailable) {
            health.down();
        }
        return health.build();
    }

    private String version() {
        BuildProperties bp = buildProperties.getIfAvailable();
        if (bp != null && bp.getVersion() != null) {
            return bp.getVersion();
        }
        String implementationVersion = getClass().getPackage().getImplementationVersion();
        return implementationVersion != null ? implementationVersion : "unknown";
    }

    private List<Map<String, Object>> components() {
        List<Map<String, Object>> components = new ArrayList<>();
        for (Map.Entry<String, Instance> entry : componentRegistry.getAvailableComponents().entrySet()) {
            Instance instance = entry.getValue();
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", entry.getKey());
            info.put("status", instance.getStatusInfo().getStatus());
            if (instance.getRegistration() != null) {
                info.put("serviceUrl", instance.getRegistration().getServiceUrl());
                info.put("managementUrl", instance.getRegistration().getManagementUrl());
                info.put("healthUrl", instance.getRegistration().getHealthUrl());
            }
            components.add(info);
        }
        return components;
    }

    private boolean isTriplestoreAvailable() {
        try {
            // trivial, side-effect-free query: a reachable, responsive triplestore answers true
            return qanaryConfigurator.getQanaryTripleStoreConnector().ask("ASK {}");
        } catch (Exception e) {
            logger.warn("triplestore health check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isReachable(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 400;
        } catch (Exception e) {
            logger.warn("frontend health check failed for {}: {}", url, e.getMessage());
            return false;
        }
    }
}
