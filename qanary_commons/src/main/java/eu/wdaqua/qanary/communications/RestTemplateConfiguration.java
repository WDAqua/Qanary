package eu.wdaqua.qanary.communications;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Handles the creation of the correct RestTemplate depending on the corresponding (rest.template.setting)-property.
 * <p>
 * The properties qanary.rest.connect-timeout-ms and qanary.rest.read-timeout-ms limit how long a call (e.g., of a
 * Qanary component by the pipeline) may wait for a connection and for the data of the answer; 0 (the default) waits
 * without limit.
 */
@Configuration
public class RestTemplateConfiguration {

    @Autowired
    private CacheOfRestTemplateResponse cacheOfRestTemplateResponse;

    @Value("${qanary.rest.connect-timeout-ms:0}")
    private long connectTimeoutMs;

    @Value("${qanary.rest.read-timeout-ms:0}")
    private long readTimeoutMs;

    @Bean
    @ConditionalOnProperty(name = "rest.template.setting", havingValue = "both", matchIfMissing = false)
    public RestTemplateWithProcessId restTemplateWithProcessId() {
        return new RestTemplateWithProcessId("both", cacheOfRestTemplateResponse, connectTimeout(), readTimeout());
    }

    @Bean
    @ConditionalOnProperty(name = "rest.template.setting", havingValue = "logging", matchIfMissing = false)
    public RestTemplateWithProcessId cacheOfRestTemplateWithProcessId() {
        return new RestTemplateWithProcessId("logging", null, connectTimeout(), readTimeout());
    }

    @Bean
    @ConditionalOnProperty(name = "rest.template.setting", havingValue = "caching", matchIfMissing = true)
    public RestTemplateWithCaching cacheOfRestTemplateWithCaching() {
        return new RestTemplateWithCaching(
                cacheOfRestTemplateResponse,
                "caching",
                connectTimeout(),
                readTimeout()
        );
    }

    private Duration connectTimeout() {
        return Duration.ofMillis(Math.max(0, connectTimeoutMs));
    }

    private Duration readTimeout() {
        return Duration.ofMillis(Math.max(0, readTimeoutMs));
    }

}
