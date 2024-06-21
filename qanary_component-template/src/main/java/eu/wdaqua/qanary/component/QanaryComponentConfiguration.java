package eu.wdaqua.qanary.component;

import org.apache.commons.cli.MissingArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.annotation.PostConstruct;

@Component
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "pipeline.as.component", matchIfMissing = true, havingValue = "true")
public class QanaryComponentConfiguration {
    private final Logger logger = LoggerFactory.getLogger(QanaryComponentConfiguration.class);
    private final String[] propertiesToDisplayOnStartup = { //
            "server.host", //
            "server.port", //
            "spring.application.name", //
            "spring.application.description", //
            "spring.boot.admin.url", //
            "spring.boot.admin.client.url"};
    // add any required parameters
    private final String[] requiredParameters = {"server.port", "spring.application.name", "spring.boot.admin.url"};
    private Environment environment;

    public QanaryComponentConfiguration(@Autowired Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateRequiredArguments() throws MissingArgumentException {
        for (int i = 0; i < requiredParameters.length; i++) {
            if (!this.propertyIsValid(requiredParameters[i])) {
                logger.error("Configuration parameter '" + requiredParameters[i] + "' was not provided.\n\n" //
                        + "\tRequired: Add parameter '" + requiredParameters[i] + "' to environment configuration\n" //
                        + "\t(c.f., https://www.tutorialspoint.com/spring_boot/spring_boot_application_properties.htm)\n\n");
                throw new MissingArgumentException(requiredParameters[i]);
            }
        }
        logger.info("Current Component Configuration: \n{}", this);
    }

    private boolean propertyIsValid(String property) {
        if (this.propertyExists(property)) {
            if (!this.getPropertyValue(property).equals("")) {
                return true;
            }
        }
        return false;
    }

    private boolean propertyExists(String requiredParameter) {
        return this.environment.containsProperty(requiredParameter);
    }

    public String getPropertyValue(String property) {
        if (property.compareTo("server.host") == 0) {
            return getHost();
        } else {
            return this.environment.getProperty(property);
        }
    }

    public String getBaseUrl() {
        try {
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .toUriString();
            return baseUrl;
        } catch (Exception e) {
            logger.warn("could not get base url: {}", e.getMessage());
            return null;
        }
    }

    // TODO: return the host on startup
    // currently the context is only available after the application started
    public String getHost() {
        String baseUrl = this.getBaseUrl();
        if (baseUrl != null) {
            return baseUrl.substring(0, baseUrl.lastIndexOf(":"));
        }
        return null;
    }

    public String getApplicationName() {
        return this.getPropertyValue("spring.application.name");
    }


    public String toString() {
        String output = "";
        for (int i = 0; i < propertiesToDisplayOnStartup.length; i++) {
            output += String.format("%-40s = %s\n", propertiesToDisplayOnStartup[i],
                    this.getPropertyValue(propertiesToDisplayOnStartup[i]));
        }
        return output;
    }
}
