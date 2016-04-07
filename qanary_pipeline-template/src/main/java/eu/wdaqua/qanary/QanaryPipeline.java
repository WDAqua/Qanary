package eu.wdaqua.qanary;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Maps;

import de.codecentric.boot.admin.config.EnableAdminServer;
import de.codecentric.boot.admin.event.ClientApplicationDeregisteredEvent;
import de.codecentric.boot.admin.event.ClientApplicationEvent;
import de.codecentric.boot.admin.event.ClientApplicationRegisteredEvent;
import eu.wdaqua.qanary.business.QanaryConfigurator;

@SpringBootApplication
@EnableAdminServer
@EnableDiscoveryClient // registers itself as client for the admin server,
// removeable
@ComponentScan({ "eu.wdaqua.qanary.business", "eu.wdaqua.qanary.web" })
public class QanaryPipeline {

	public static void main(String[] args) {
		SpringApplication.run(QanaryPipeline.class, args);
	}

	@Autowired
	public QanaryConfigurator configurator;

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public Map<String, Integer> componentsToIndexMap(
			@Value("'${qanary.components}'.split(',')") List<String> components) {
		int i = 0;
		Map<String, Integer> componentsToIndexMap = Maps.newHashMap();
		for (String component : components) {
			componentsToIndexMap.put(component, i);
			i++;
		}
		return componentsToIndexMap;
	}

	@Bean
	public QanaryConfigurator configurator(@Value("'${qanary.components}'.split(',')") List<String> components,
			@Value("${server.host}") @NotNull String host, @Value("${server.port}") @NotNull int port,
			@Value("${qanary.triplestore}") @NotNull URL endpoint) {
		return new QanaryConfigurator(restTemplate(), componentsToIndexMap(components), host, port, endpoint);
	}

	@EventListener(ClientApplicationRegisteredEvent.class)
	public void addComponent(ClientApplicationEvent event) {
		configurator.addComponent(event.getApplication());
	}

	@EventListener(ClientApplicationDeregisteredEvent.class)
	public void removeComponent(ClientApplicationEvent event) {
		configurator.removeComponent(event.getApplication());
	}
}
