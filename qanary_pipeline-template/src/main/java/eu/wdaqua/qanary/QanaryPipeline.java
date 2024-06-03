package eu.wdaqua.qanary;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import eu.wdaqua.qanary.commons.QanaryMessage;
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
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.exceptions.TripleStoreNotProvided;
import eu.wdaqua.qanary.exceptions.TripleStoreNotWorking;
import eu.wdaqua.qanary.web.QanaryPipelineConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import eu.wdaqua.qanary.component.QanaryComponent;

@SpringBootApplication
@de.codecentric.boot.admin.server.config.EnableAdminServer
// @EnableDiscoveryClient // registers itself as client for the Spring Boot admin server,
// removable
@ComponentScan({ "eu.wdaqua.qanary" })
public class QanaryPipeline extends QanaryComponent{

	private static final Logger logger = LoggerFactory.getLogger(QanaryPipeline.class);

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
	
	/**
	 * send a test query to the triplestore 
	 * 
	 * @param myQanaryTripleStoreConnector
	 * @return
	 * @throws TripleStoreNotWorking 
	 */
	private void checkTripleStoreConnection(QanaryTripleStoreConnector myQanaryTripleStoreConnector) throws TripleStoreNotWorking {
		int numberOfTests = 1;
		int maxNumberOfTests = 10;
		/*// TODO: needs to be extracted and moved to abstract class QanaryTripleStoreConnector
		while(numberOfTests <= maxNumberOfTests) {
			try {
				ResultSet myResultSet = myQanaryTripleStoreConnector.select("SELECT ?g WHERE { GRAPH ?g { ?s ?p ?o } } LIMIT 1");
				while(myResultSet.hasNext()) {
					logger.debug("Test #{}/{}: Test query to {}: {}", numberOfTests, maxNumberOfTests, myQanaryTripleStoreConnector.getFullEndpointDescription(), myResultSet.next().toString()); 
				}
				logger.info("Test query to triplestore {} worked.", myQanaryTripleStoreConnector.getFullEndpointDescription());
				return;
			} catch (SparqlQueryFailed e) {
				e.printStackTrace();
			}
			numberOfTests--;
		}
		throw new TripleStoreNotWorking("Minimal request does not work after " + maxNumberOfTests + " tries.");
		*/ 
	}
	

	@Bean
	public QanaryConfigurator configurator( //
			QanaryComponentRegistrationChangeNotifier myQanaryComponentRegistrationChangeNotifier, //
			QanaryTripleStoreConnector myQanaryTripleStoreConnector
	) throws TripleStoreNotWorking, TripleStoreNotProvided {
		this.checkTripleStoreConnection(myQanaryTripleStoreConnector);
		
		return new QanaryConfigurator( //
				restTemplate(), //
				qanaryPipelineConfiguration.getPredefinedComponents(), // from config
				qanaryPipelineConfiguration.getHost(), // from config
				qanaryPipelineConfiguration.getPort(), // from config
				qanaryPipelineConfiguration.getQanaryOntologyAsURI(), // from config
				qanaryPipelineConfiguration.getTriplestoreAsURI(), // from config
				myQanaryTripleStoreConnector //
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

	@Bean
	public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion, @Value("${spring.application.name}") String title) { 
		return new OpenAPI().info(new Info() //
				.title(title) 
				.version(appVersion) //
				.description("Provides central functionality for each Qanary component (registration) "
						+ "and endpoints for users.")
				.termsOfService("http://swagger.io/terms/") //
				.license(new License().name("Apache 2.0").url("http://springdoc.org")) //
		);
	}

	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

		// This method is only executed when the Qanary pipeline component is called as component
		// It follows the standard of all process() implementations

		// 1. Get all information === Question
		// 2. Compute new information === Execute the own pipeline
		// 3. Store information === Response JSON

		// STEP 1:
		/*
		 * has access to current question via methods
		 * doesn't select any queries ==> We've to store input data somehow different, generalized
		 *
		 */



		// STEP 2:
		/*
		 * Calls its own components
		 * How and where are they defined? Are they somehow passed or determined?
		 * (Call for registered components possible -> API working?)
		 * ==> Extend /annotatequestion with componentListParam?
		 * ==> Component should serve one explicit function -> Pre-defined components
		 * Returns @see{QanaryQuestionAnsweringRun.class}
		 */



		// STEP 3:
		/*
		 * Store the response, e.g. the graph, questionURI, etc.
		 */



		return null;
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
