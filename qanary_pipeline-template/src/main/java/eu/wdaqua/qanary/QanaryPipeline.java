package eu.wdaqua.qanary;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Maps;

import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.business.TriplestoreEndpointIdentifier;
import eu.wdaqua.qanary.exceptions.TripleStoreNotProvided;
import eu.wdaqua.qanary.web.QanaryPipelineConfiguration;

@SpringBootApplication
@de.codecentric.boot.admin.server.config.EnableAdminServer
// @EnableDiscoveryClient // registers itself as client for the Spring Boot admin server,
// removable
@ComponentScan({ "eu.wdaqua.qanary.business", "eu.wdaqua.qanary.web" })
public class QanaryPipeline {

	private static final Logger logger = LoggerFactory.getLogger(QanaryPipeline.class);

	@Autowired
	public QanaryComponentRegistrationChangeNotifier myComponentRegistrationChangeNotifier;
	
	@Autowired
	public QanaryPipelineConfiguration qanaryPipelineConfiguration;  

	public static void main(final String[] args) {
		// define usage of configuration file 'application.local.properties'
		new SpringApplicationBuilder(QanaryPipeline.class).properties("spring.config.name:application,application.local").run(args);
	}

	@Bean
	ApplicationRunner applicationRunner() {
		return (args) -> {
			logger.info("host and port: {}:{}", this.qanaryPipelineConfiguration.getHost(), this.qanaryPipelineConfiguration.getPort());
			logger.info("triplestore endpoint: {}", this.qanaryPipelineConfiguration.getTriplestore());
			logger.info("questions directory: {}", this.qanaryPipelineConfiguration.getQuestionsDirectory());
		};
	}

	@Bean
	public QanaryComponentRegistrationChangeNotifier getComponentRegistrationChangeNotifier(
			InstanceRepository repository) {
		return new QanaryComponentRegistrationChangeNotifier(repository);
	}

	@Bean
	public QanaryConfigurator configurator( //
			TriplestoreEndpointIdentifier myTriplestoreEndpointIdentifier, //
			QanaryComponentRegistrationChangeNotifier myQanaryComponentRegistrationChangeNotifier //
	) throws TripleStoreNotProvided {
		return new QanaryConfigurator( //
				restTemplate(), //
				qanaryPipelineConfiguration.getPredefinedComponents(), // from config
				qanaryPipelineConfiguration.getHost(), // from config
				qanaryPipelineConfiguration.getPort(), // from config
				qanaryPipelineConfiguration.getTriplestoreAsURI(), // from config
				qanaryPipelineConfiguration.getQanaryOntologyAsURI(), // from config
				myTriplestoreEndpointIdentifier //
		);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public Map<String, Integer> componentsToIndexMap() {
		int i = 0;
		final Map<String, Integer> componentsToIndexMap = Maps.newHashMap();
		for (final String component : qanaryPipelineConfiguration.getPredefinedComponents()) {
			componentsToIndexMap.put(component, i);
			i++;
		}
		return componentsToIndexMap;
	}

	@Bean
	@ConditionalOnProperty(name = "spring.config.location", matchIfMissing = false)
	public PropertiesConfiguration propertiesConfiguration(
			@Value("${spring.config.location}") String path) throws Exception {

		Path localConfigPath = Paths.get(new ClassPathResource(path).getPath());
		//String filePath = new File(path).getCanonicalPath();

		logger.warn("new property source: {}", localConfigPath.toString());
		PropertiesConfiguration configuration = new PropertiesConfiguration(
				new File(localConfigPath.toString()));
		configuration.setReloadingStrategy(new FileChangedReloadingStrategy());
		return configuration;
	}

	/*
	 * @EventListener(ClientApplicationRegisteredEvent.class) public void
	 * addComponent(final ClientApplicationEvent event) {
	 * configurator.addComponent(event.getApplication()); }
	 * 
	 * @EventListener(ClientApplicationDeregisteredEvent.class) public void
	 * removeComponent(final ClientApplicationEvent event) {
	 * configurator.removeComponent(event.getApplication()); }
	 */
}
