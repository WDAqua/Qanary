package eu.wdaqua.qanary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.reactive.ReactiveHttpClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;

import reactor.netty.http.client.HttpClient;

/**
 * the reactive HTTP client of Spring Boot, used by Spring Boot Admin for the
 * health checks of the components, must not cache IP addresses of components as
 * long as the DNS answer allows (600 seconds in Docker networks)
 */
class QanaryComponentHealthCheckConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ReactiveHttpClientAutoConfiguration.class))
            .withUserConfiguration(QanaryComponentHealthCheckConfiguration.class);

    private static Duration maxTimeToLiveOf(HttpClient httpClient) {
        return httpClient.configuration().getNameResolverProvider().cacheMaxTimeToLive();
    }

    private static HttpClient httpClientOf(ClientHttpConnector connector) {
        return (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
    }

    @Test
    void limitDnsCacheSetsTheMaximalTimeToLiveOfTheResolver() {
        HttpClient httpClient = QanaryComponentHealthCheckConfiguration.limitDnsCache(HttpClient.create(),
                Duration.ofSeconds(7));
        assertEquals(Duration.ofSeconds(7), maxTimeToLiveOf(httpClient));
    }

    @Test
    void connectorOfTheReactiveHttpClientCachesAddressesForTenSecondsByDefault() {
        contextRunner.run(context -> assertEquals(Duration.ofSeconds(10),
                maxTimeToLiveOf(httpClientOf(context.getBean(ClientHttpConnector.class)))));
    }

    @Test
    void maximalTimeToLiveIsConfigurable() {
        contextRunner.withPropertyValues(QanaryComponentHealthCheckConfiguration.DNS_CACHE_MAX_TIME_TO_LIVE + "=1m")
                .run(context -> assertEquals(Duration.ofMinutes(1),
                        maxTimeToLiveOf(httpClientOf(context.getBean(ClientHttpConnector.class)))));
    }
}
