package eu.wdaqua.qanary.spotlightNED;

import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RestController;

import eu.wdaqua.qanary.component.QanaryComponent;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
public class Application {

	
	@Bean
	public QanaryComponent qanaryComponent() {
		return new DBpediaSpotlightNED();
	}
	/**
	 * default main, can be removed later
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		//Properties p = new Properties();
		//new SpringApplicationBuilder(Application.class).properties(p).run(args);
		SpringApplication.run(Application.class, args);
	}
}
