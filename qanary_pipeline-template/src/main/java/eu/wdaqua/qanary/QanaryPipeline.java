package eu.wdaqua.qanary;

import com.google.common.collect.Maps;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.exceptions.TripleStoreNotProvided;
import eu.wdaqua.qanary.exceptions.TripleStoreNotWorking;
import eu.wdaqua.qanary.explainability.QanaryExplanation;
import eu.wdaqua.qanary.explainability.QanaryExplanationData;
import eu.wdaqua.qanary.web.QanaryPipelineConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@de.codecentric.boot.admin.server.config.EnableAdminServer
// @EnableDiscoveryClient // registers itself as client for the Spring Boot admin server,
// removable
@ComponentScan({"eu.wdaqua.qanary"})
public class QanaryPipeline implements QanaryExplanation {

    private static final Logger logger = LoggerFactory.getLogger(QanaryPipeline.class);
    @Autowired
    public QanaryPipelineConfiguration qanaryPipelineConfiguration;
    @Value("${spring.application.name}")
    private String applicationName;
    @Autowired
    private PipelineExplanationHelper pipelineExplanationHelper;
    @Autowired
    private RestTemplate restTemplate;

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
    public QanaryTripleStoreProxy qanaryTripleStoreProxy() {
        return new QanaryTripleStoreProxy();
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
    private void checkTripleStoreConnection(QanaryTripleStoreProxy myQanaryTripleStoreConnector) throws TripleStoreNotWorking {
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
				logger.warn("triplestore test query failed; will retry if attempts remain", e);
			}
			numberOfTests--;
		}
		throw new TripleStoreNotWorking("Minimal request does not work after " + maxNumberOfTests + " tries.");
		*/
    }


    @Bean
    public QanaryConfigurator configurator( //
                                            QanaryComponentRegistrationChangeNotifier myQanaryComponentRegistrationChangeNotifier, //
                                            QanaryTripleStoreProxy myQanaryTripleStoreConnector
    ) throws TripleStoreNotWorking, TripleStoreNotProvided {
        this.checkTripleStoreConnection(myQanaryTripleStoreConnector);
        return new QanaryConfigurator( //
                restTemplate, //
                qanaryPipelineConfiguration.getPredefinedComponents(), // from config
                qanaryPipelineConfiguration.getHost(), // from config
                qanaryPipelineConfiguration.getPort(), // from config
                qanaryPipelineConfiguration.getQanaryOntologyAsURI(), // from config
                qanaryPipelineConfiguration.getTriplestoreAsURI(), // from config
                myQanaryTripleStoreConnector //
        );
    }
    
    @ConditionalOnMissingBean(RestTemplate.class)
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
    public ReloadingFileBasedConfigurationBuilder<PropertiesConfiguration> propertiesConfiguration(
            @Value("${spring.config.location}") String path) throws Exception {

        logger.warn("new property source: {}", path);
        // commons-configuration2 replaces the removed FileChangedReloadingStrategy
        // with a reloading builder; callers trigger the reload check via its
        // ReloadingController before reading a property. The file is located by
        // cc2's default strategy (classpath + filesystem) and is OPTIONAL: a
        // missing file is tolerated in ReloadablePropertySource#getProperty
        // (matching the lenient behaviour of commons-configuration 1.x).
        ReloadingFileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                new ReloadingFileBasedConfigurationBuilder<>(PropertiesConfiguration.class);
        builder.configure(new Parameters().properties().setFileName(path));
        return builder;
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

    /**
     * returns an explanation for the post-hoc behavior of a specific execution
     */
    @Override
    public String explain(QanaryExplanationData qanaryExplanationData) throws IOException, URISyntaxException, SparqlQueryFailed {
        // Fetch subcomponent explanations == All annotations made on the original KG
        List<String> components = pipelineExplanationHelper.getUsedComponents(qanaryExplanationData.getGraph());
        List<QanaryExplanationData> dataList = new ArrayList<>();
        for (String qanaryComponent : components) {
            dataList.add(new QanaryExplanationData(
                    qanaryExplanationData.getGraph(),
                    qanaryExplanationData.getQuestionId(),
                    pipelineExplanationHelper.getQanaryComponentRegistrationChangeNotifier().getAvailableComponents().get(qanaryComponent).getRegistration().getServiceUrl()
            ));
        }
        List<String> subComponentExplanations = pipelineExplanationHelper.fetchSubComponentExplanations(dataList).collectList().block();
        Map<String, String> componentAndExplanation = new HashMap<>();
        for (int i = 0; i < dataList.size(); i++) {
            componentAndExplanation.put(components.get(i), subComponentExplanations.get(i));  // TODO?: Prove, that the explanations correspond to the correct component
        }
        qanaryExplanationData.setComponent(this.applicationName);
        qanaryExplanationData.setExplanations(componentAndExplanation);
        return pipelineExplanationHelper.requestPipelineExplanation(qanaryExplanationData);
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
