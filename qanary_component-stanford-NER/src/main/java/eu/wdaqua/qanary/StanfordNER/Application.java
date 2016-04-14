package eu.wdaqua.qanary.StanfordNER;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by didier on 27.03.16.
 */
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
