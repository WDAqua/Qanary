package eu.wdaqua.qanary.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.apache.commons.cli.MissingArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.exceptions.MissingRequiredConfiguration;
import eu.wdaqua.qanary.exceptions.TripleStoreNotProvided;

/**
 * component holding the environment configuration extra: checks required
 * parameters and sets default values
 * 
 */
@Component
public class QanaryPipelineConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(QanaryPipelineConfiguration.class);
	private Environment environment;
	private final String[] commonPropertiesToBeShownOnStartup = { //
			"spring.application.name", //
			"server.host", //
			"server.port", //
			"qanary.triplestore", //
			"qanary.questions.directory", //
			"qanary.components", //
			"qanary.ontology"};
	private final String[] debugPropertiesPrefixesToBeShowOnStartup = { //
			"qanary", //
			"server", //
			"spring" //
	};
	private final String[] requiredParameterNames = { "qanary.triplestore", "server.host", "server.port", "qanary.ontology"};

	public QanaryPipelineConfiguration(@Autowired Environment environment) {
		this.environment = environment;
	}

	@PostConstruct
	public void validateAfterInitialization() throws MissingArgumentException {
		for (int i = 0; i < requiredParameterNames.length; i++) {
			if (!this.hasProperty(requiredParameterNames[i])) {
				logger.error("Configuration parameter '" + requiredParameterNames[i] + "' was not provided.\n\n" //
						+ "\tRequired: Add parameter '" + requiredParameterNames[i] + "' to environment configuration\n" //
						+ "\t(c.f., https://www.tutorialspoint.com/spring_boot/spring_boot_application_properties.htm)\n\n");
				throw new MissingArgumentException(requiredParameterNames[i]);
			}
		}
		logger.info("Current Configuration: \n{}", this);

		// log all properties that are important (see
		// debugPropertiesPrefixesToBeShowOnStartup) to DEBUG log
		Map<String, Object> knowProperties = this.getAllKnownProperties(environment);
		Map<String, Object> sortedMap = new TreeMap<String, Object>(knowProperties);
		logSourceOfRelevantConfigurationProperties(sortedMap);
	}

	/**
	 * puts properties from the environment env into a map, only properties having a
	 * prefix from debugPropertiesPrefixesToBeShowOnStartup are considered
	 * 
	 * @param env
	 * @return
	 */
	public Map<String, Object> getAllKnownProperties(Environment env) {
		boolean flag;
		Map<String, Object> propertyMap = new HashMap<>();
		if (env instanceof ConfigurableEnvironment) {
			for (PropertySource<?> propertySource : ((ConfigurableEnvironment) env).getPropertySources()) {
				if (propertySource instanceof EnumerablePropertySource) {
					for (String key : ((EnumerablePropertySource<?>) propertySource).getPropertyNames()) {
						flag = false;
						for (String prefix : debugPropertiesPrefixesToBeShowOnStartup) {
							if (key.startsWith(prefix)) {
								flag = true;
							}
						}
						if (flag) {
							propertyMap.put(key, env.getProperty(key));
						}
					}
				}
			}
		}
		return propertyMap;
	}

	/**
	 * log properties and their values into DEBUG including their (possibly
	 * multiple) sources, only property values from the applicationConfig and
	 * commandLineArgs are considered
	 * 
	 * @param properties
	 */
	private void logSourceOfRelevantConfigurationProperties(Map<String, Object> properties) {
		String key;
		String sourceDescriptions;
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			key = entry.getKey();
			sourceDescriptions = "";
			for (PropertySource<?> propertySource : ((ConfigurableEnvironment) environment).getPropertySources()) {
				if (propertySource instanceof EnumerablePropertySource) {
					if ((propertySource.getName().startsWith("applicationConfig") // from the applicationConfig
							|| propertySource.getName().startsWith("commandLineArgs") // or from command line
					) && propertySource.getProperty(key) != null // skip null (undefined in propertySource)
					) {
						sourceDescriptions += "\n\tvalue '" + propertySource.getProperty(key) + "'" //
								+ " from source '" + propertySource.getName() + "'";
					}
				}
			}
			if (!sourceDescriptions.isEmpty()) {
				logger.debug("configuration property: {}={} -> sources: {}", entry.getKey(), entry.getValue(),
						sourceDescriptions);
			}
		}
	}

	public boolean hasProperty(String name) {
		return this.environment.containsProperty(name);
	}

	public String getProperty(String name) {
		if (name.compareTo("server.host") == 0) {
			return getHost();
		} else if (name.compareTo("qanary.triplestore") == 0) {
			return getTriplestore();
		} else if (name.compareTo("qanary.questions.directory") == 0) {
			return getQuestionsDirectory();
		} else {
			return this.environment.getProperty(name);
		}
	}

	public String getHost() {
		String name = "server.host";
		if (this.hasProperty(name) && !this.environment.getProperty(name).isEmpty()) {
			return this.environment.getProperty(name);
		} else {
			return null;
		}
	}

	public Integer getPort() {
		String name = "server.port";
		if (this.hasProperty(name)) {
			return Integer.valueOf(this.getProperty(name));
		} else {
			return null;
		}
	}

	/**
	 * required attribute
	 * 
	 * @return
	 */
	public String getTriplestore() {
		String triplestore = this.environment.getProperty("qanary.triplestore");
		if (triplestore == null) {
			throw new MissingRequiredConfiguration("qanary.triplestore");
		} else {
			return triplestore;
		}
	}

	/**
	 * required attribute
	 * 
	 * @return
	 */
	public URI getTriplestoreAsURI() throws TripleStoreNotProvided {
		try {
			return new URI(getTriplestore());
		} catch (Exception e) {
			throw new TripleStoreNotProvided(
					"triplestore value '" + getTriplestore() + "' was no URI.\n(error: " + e.getMessage() + ")");
		}
	}

	public URI getQanaryOntologyAsURI() {
		try {
			return new URI(this.getProperty("qanary.ontology"));
		} catch (Exception e) {
			throw new MissingRequiredConfiguration("qanary.ontology");
		}
	}


	/**
	 * default value is System.getProperty("user.dir") if configuration is not
	 * user-defined
	 * 
	 * @return
	 */
	public String getQuestionsDirectory() {
		String questionsDirectory = this.environment.getProperty("qanary.questions.directory");
		if (questionsDirectory == null) {
			return System.getProperty("user.dir");
		} else {
			return questionsDirectory;
		}
	}

	/**
	 * create list of components from configuration (comma-separated) parameter
	 * 
	 * @return
	 */
	public List<String> getPredefinedComponents() {
		if (this.hasProperty("qanary.components")) {
			return Arrays.asList(this.getProperty("qanary.components").split("\\s*,\\s*"));
		} else {
			return new ArrayList<>();
		}
	}

	/**
	 * specific toString method
	 */
	public String toString() {
		String result = "";
		for (int i = 0; i < commonPropertiesToBeShownOnStartup.length; i++) {
			result += String.format("%-40s = %s\n", //
					commonPropertiesToBeShownOnStartup[i], //
					this.getProperty(commonPropertiesToBeShownOnStartup[i]));
		}
		return result;
	}
}
