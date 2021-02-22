package eu.wdaqua.qanary.web;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

@Configuration
public class ReloadablePropertySourceConfig {

    private ConfigurableEnvironment environment;
    private final Logger logger = LoggerFactory.getLogger(ReloadablePropertySourceConfig.class);

    public ReloadablePropertySourceConfig(@Autowired ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.config.location", matchIfMissing = false)
    public ReloadablePropertySource reloadablePropertySource(PropertiesConfiguration propertiesConfiguration) {
        ReloadablePropertySource source = new ReloadablePropertySource("dynamic", propertiesConfiguration);
        MutablePropertySources mutableSources = environment.getPropertySources();
        mutableSources.addFirst(source);
        logger.info("added new reloadable property source");
        return source;
    }
}
