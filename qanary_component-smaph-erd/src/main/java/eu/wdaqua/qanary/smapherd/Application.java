package eu.wdaqua.qanary.smapherd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
public class Application {

	/**
	* this method is needed to make the QanaryComponent in this project known
	* to the QanaryServiceController in the qanary_component-template
	* 
	* @return
	*/
	@Bean
	public QanaryComponent qanaryComponent() {
		return new SmaphErd();
	}
	
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
