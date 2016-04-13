package eu.wdaqua.qanary.trivial.batmanquestion.annotator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * start the spring application
 * 
 * @author AnBo
 *
 */
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.component")
public class BatmanQuesrtionAnnotatorApplication {

	public static void main(String... args) {
		SpringApplication.run(BatmanQuesrtionAnnotatorApplication.class, args);
	}
}
