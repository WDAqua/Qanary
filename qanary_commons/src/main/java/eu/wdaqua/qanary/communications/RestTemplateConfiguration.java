package eu.wdaqua.qanary.communications;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Handles the creation of the correct RestTemplate depending on the corresponding (rest.template.setting)-property.
 */
@Configuration
public class RestTemplateConfiguration {

    @Bean
    @ConditionalOnProperty(name = "rest.template.setting", havingValue = "both", matchIfMissing = false)
    public RestTemplateWithProcessId restTemplateWithProcessId() {
        return new RestTemplateWithProcessId("both");
    }

    @Bean
    @ConditionalOnProperty(name = "rest.template.setting", havingValue = "logging", matchIfMissing = false)
    public RestTemplateWithProcessId cacheOfRestTemplateWithProcessId() {
        return new RestTemplateWithProcessId("logging");
    }

    @Bean
    @ConditionalOnProperty(name = "rest.template.setting", havingValue = "caching", matchIfMissing = true)
    public RestTemplateWithCaching cacheOfRestTemplateWithCaching() {
        return new RestTemplateWithCaching(
                new CacheOfRestTemplateResponse(),
                "caching"
        );
    }


}
