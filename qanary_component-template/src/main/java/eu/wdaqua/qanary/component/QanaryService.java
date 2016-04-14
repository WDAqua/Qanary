package eu.wdaqua.qanary.component;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
@RestController
public class QanaryService {

	/**
	 * default main, can be removed later
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Properties p = new Properties();
		new SpringApplicationBuilder(QanaryService.class).properties(p).run(args);
		//SpringApplication.run(QanaryService.class, args);
	}
}
