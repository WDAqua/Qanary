package eu.wdaqua.qanary.LuceneLinker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.component.QanaryComponent;

/**
 * Created by didier on 27.03.16.
 */
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
public class Application {
    /**
     * this method is needed to make the QanaryComponent in this project known to the
     * QanaryServiceController in the qanary_component-template
     */
    @Bean
    public QanaryComponent qanaryComponent() {
        return new LuceneLinker();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
