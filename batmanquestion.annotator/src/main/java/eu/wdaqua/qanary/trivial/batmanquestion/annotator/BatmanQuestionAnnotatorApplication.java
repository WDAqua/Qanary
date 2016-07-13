package eu.wdaqua.qanary.trivial.batmanquestion.annotator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.wdaqua.qanary.component.QanaryComponent;

/**
 * start the spring application
 *
 * @author AnBo
 */
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
public class BatmanQuestionAnnotatorApplication {

    /**
     * this method is needed to make the QanaryComponent in this project known to the
     * QanaryServiceController in the qanary_component-template
     */
    @Bean
    public QanaryComponent qanaryComponent() {
        return new BatmanQuestionAnnotator();
    }

    public static void main(String... args) {
        SpringApplication.run(BatmanQuestionAnnotatorApplication.class, args);
    }
}
