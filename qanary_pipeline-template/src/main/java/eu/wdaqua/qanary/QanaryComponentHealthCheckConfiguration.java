package eu.wdaqua.qanary;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.boot.http.client.autoconfigure.reactive.ClientHttpConnectorBuilderCustomizer;
import org.springframework.boot.http.client.reactive.ReactorClientHttpConnectorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.netty.http.client.HttpClient;

/**
 * limits how long the health checks of the registered components (done by Spring
 * Boot Admin) cache the IP addresses of the components
 * <p>
 * The health checks use the reactive HTTP client of Spring Boot, whose DNS
 * resolver (Reactor Netty) caches an address as long as the DNS answer allows,
 * e.g., 600 seconds in Docker networks. If a component's container is re-created,
 * it might get another IP address, while its old address might be used by another
 * container. The health check then still calls the old address, and the component
 * is shown as OFFLINE ("Connection refused") for up to 10 minutes, although it is
 * running and has registered again.
 * <p>
 * configuration: {@value #DNS_CACHE_MAX_TIME_TO_LIVE} (default:
 * {@value #DEFAULT_DNS_CACHE_MAX_TIME_TO_LIVE}, e.g., 10s, 1m or PT10S)
 */
@Configuration
public class QanaryComponentHealthCheckConfiguration {
    public static final String DNS_CACHE_MAX_TIME_TO_LIVE = "qanary.health-check.dns-cache-max-time-to-live";
    public static final String DEFAULT_DNS_CACHE_MAX_TIME_TO_LIVE = "10s";
    private static final Logger logger = LoggerFactory.getLogger(QanaryComponentHealthCheckConfiguration.class);

    @Bean
    public ClientHttpConnectorBuilderCustomizer<ReactorClientHttpConnectorBuilder> limitDnsCacheOfHealthChecks(
            @Value("${" + DNS_CACHE_MAX_TIME_TO_LIVE + ":" + DEFAULT_DNS_CACHE_MAX_TIME_TO_LIVE + "}") String maxTimeToLive) {
        Duration duration = DurationStyle.detectAndParse(maxTimeToLive);
        logger.info("IP addresses of components are cached for at most {} by the health checks", duration);
        return builder -> builder.withHttpClientCustomizer(httpClient -> limitDnsCache(httpClient, duration));
    }

    /**
     * returns the given HTTP client with a DNS resolver that caches addresses for at
     * most the given duration
     */
    static HttpClient limitDnsCache(HttpClient httpClient, Duration maxTimeToLive) {
        return httpClient.resolver(spec -> spec.cacheMaxTimeToLive(maxTimeToLive));
    }
}
