package eu.wdaqua.qanary.component;

import javax.annotation.PostConstruct;

import org.apache.commons.cli.MissingArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.config.CacheConfig;
import eu.wdaqua.qanary.commons.config.RestClientConfig;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;

@Component
@Configuration
@EnableCaching
public class QanaryComponentConfiguration {
	private final Logger logger = LoggerFactory.getLogger(QanaryComponentConfiguration.class);
	private Environment environment;
	private final String[] propertiesToDisplayOnStartup = { //
			"server.port", //
			"spring.application.name", //
			"spring.application.description", //
			"spring.boot.admin.url", //
			"spring.boot.admin.client.url" };
	// add any required parameters
	private final String[] requiredParameters = { "server.port", "spring.application.name", "spring.boot.admin.url" };

	public QanaryComponentConfiguration(@Autowired Environment environment) {
		this.environment = environment;
	}

	/**
	 * the following four beans are required to make these components in the package
	 * eu.wdaqua.qanary.component visible
	 * 
	 * @return
	 */
	@Bean
	public RestClientConfig myRestClientConfig() {
		return new RestClientConfig();
	}

	@Bean
	public RestTemplateWithCaching myComponentRestClient(RestClientConfig myRestClientConfig)
			throws NoSuchMethodException, SecurityException {
		return myRestClientConfig.restTemplateWithCaching(myRestClientConfig.cacheResponse());
	}
	
	@Bean
	public CacheOfRestTemplateResponse myCacheOfRestTemplateResponse(RestClientConfig myRestClientConfig)
			throws NoSuchMethodException, SecurityException{
		return myRestClientConfig.cacheResponse();
	}
	
	@Bean
	public CacheConfig myCacheConfig() {
		return new CacheConfig();
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
		return this.environment.getProperty(property);
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
