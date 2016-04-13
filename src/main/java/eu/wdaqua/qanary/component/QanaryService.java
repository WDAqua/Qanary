package eu.wdaqua.qanary.component;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//import eu.wdaqua.qanary.QanaryPipeline;
//import eu.wdaqua.qanary.business.QanaryConfigurator;

@SpringBootApplication
@EnableAutoConfiguration
// @RestController
public class QanaryService {

	// @Autowired
	// public QanaryConfigurator configurator;

	/**
	 * default main, can be removed later
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// Properties p = new Properties();
		// new
		// SpringApplicationBuilder(QanaryService.class).properties(p);//.run(args);
		SpringApplication.run(QanaryService.class, args);
	}

}
