package eu.wdaqua.qanary.component;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
public class QanaryService {

	/**
	 * default main, can be removed later
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(QanaryService.class, args);
	}

}
