package eu.wdaqua.qanary;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Maps;

import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.business.TriplestoreEndpointIdentifier;
import eu.wdaqua.qanary.exceptions.TripleStoreNotProvided;

@SpringBootApplication
@de.codecentric.boot.admin.server.config.EnableAdminServer
// @EnableDiscoveryClient // registers itself as client for the Spring Boot admin server,
// removable
@ComponentScan({ "eu.wdaqua.qanary.business", "eu.wdaqua.qanary.web" })
public class QanaryPipeline {

	@Autowired
	public QanaryConfigurator configurator;

	@Autowired
	public QanaryComponentRegistrationChangeNotifier myComponentRegistrationChangeNotifier;

	public static void main(final String[] args) {
		SpringApplication.run(QanaryPipeline.class, args);
	}

	@Bean
	public QanaryComponentRegistrationChangeNotifier getComponentRegistrationChangeNotifier(InstanceRepository repository) {
		return new QanaryComponentRegistrationChangeNotifier(repository);
	}

	@Bean
	public QanaryConfigurator configurator( //
			@Value("#{'${qanary.components}'.split(',')}") final List<String> availablecomponents, //
			@Value("${server.host}") @NotNull final String serverhost, //
			@Value("${server.port}") @NotNull final int serverport, //
			@Value("${qanary.triplestore}") @NotNull final URI triplestoreendpoint, //
			TriplestoreEndpointIdentifier myTriplestoreEndpointIdentifier, //
			QanaryComponentRegistrationChangeNotifier myQanaryComponentRegistrationChangeNotifier //
	) throws TripleStoreNotProvided {

		if (triplestoreendpoint == null || triplestoreendpoint.toASCIIString().compareTo("") == 0) {
			throw new TripleStoreNotProvided(triplestoreendpoint);
		}

		return new QanaryConfigurator( //
				restTemplate(), //
				availablecomponents, // from config
				serverhost, // from config
				serverport, // from config
				triplestoreendpoint, // from config
				myTriplestoreEndpointIdentifier //
		);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public Map<String, Integer> componentsToIndexMap(
			@Value("'${qanary.components}'.split(',')") final List<String> components) {
		int i = 0;
		final Map<String, Integer> componentsToIndexMap = Maps.newHashMap();
		for (final String component : components) {
			componentsToIndexMap.put(component, i);
			i++;
		}
		return componentsToIndexMap;
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
