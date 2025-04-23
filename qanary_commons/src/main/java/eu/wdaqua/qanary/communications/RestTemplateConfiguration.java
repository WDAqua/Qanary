package eu.wdaqua.qanary.communications;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestTemplateConfiguration {

    @Bean
    @ConditionalOnProperty(name = "rest.template.setting", havingValue = "A", matchIfMissing = false)
    public RestTemplateWithProcessId restTemplateWithProcessId() {
        return new RestTemplateWithProcessId("A");
    }

    @Bean
    @ConditionalOnProperty(name = "rest.template.setting", havingValue = "C", matchIfMissing = false)
    public RestTemplateWithProcessId cacheOfRestTemplateWithProcessId() {
        return new RestTemplateWithProcessId("C");
    }

    @Bean
    @ConditionalOnProperty(name = "rest.template.setting", havingValue = "B", matchIfMissing = true)
    public RestTemplateWithCaching cacheOfRestTemplateWithCaching() {
        return new RestTemplateWithCaching(
                new CacheOfRestTemplateResponse(),
                "B"
        );
    }


}
