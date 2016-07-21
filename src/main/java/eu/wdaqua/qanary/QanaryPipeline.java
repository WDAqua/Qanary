package eu.wdaqua.qanary;

import com.google.common.collect.Maps;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.sparql.modify.GraphStoreBasic;
import com.hp.hpl.jena.update.GraphStore;

import de.codecentric.boot.admin.config.EnableAdminServer;
import de.codecentric.boot.admin.event.ClientApplicationDeregisteredEvent;
import de.codecentric.boot.admin.event.ClientApplicationEvent;
import de.codecentric.boot.admin.event.ClientApplicationRegisteredEvent;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.sparql.SparqlConnector;
import eu.wdaqua.qanary.sparql.SparqlInMemoryStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.validation.constraints.NotNull;

import java.net.URI;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@EnableAdminServer
@EnableDiscoveryClient // registers itself as client for the admin server,
// removeable
@ComponentScan({"eu.wdaqua.qanary.business", "eu.wdaqua.qanary.web"})
public class QanaryPipeline {

    @Autowired
    public QanaryConfigurator configurator;

    public static void main(final String[] args) {
        SpringApplication.run(QanaryPipeline.class, args);
    }    
    
    @Bean
    public QanaryConfigurator configurator(@Value("'${qanary.components}'.split(',')") final List<String> components,
                                           @Value("${server.host}") @NotNull final String host, @Value("${server.port}") @NotNull final int port,
                                           @Value("${qanary.triplestore}") @NotNull final URI endpoint) {
        return new QanaryConfigurator(restTemplate(), componentsToIndexMap(components), host, port, endpoint);
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

    @Bean
    public SparqlConnector sparqlConnector() {
        return new SparqlInMemoryStore(graphStore());
    }

    @Bean
    public GraphStore graphStore() {
        return new GraphStoreBasic(new DatasetImpl(ModelFactory.createDefaultModel()));
    }

    @EventListener(ClientApplicationRegisteredEvent.class)
    public void addComponent(final ClientApplicationEvent event) {
        configurator.addComponent(event.getApplication());
    }

    @EventListener(ClientApplicationDeregisteredEvent.class)
    public void removeComponent(final ClientApplicationEvent event) {
        configurator.removeComponent(event.getApplication());
    }
}
