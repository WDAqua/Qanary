package eu.wdaqua.qanary.examplecomponent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.QanaryMessage;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryService;

@Component
public class ExampleQanaryComponent implements QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(ExampleQanaryComponent.class);

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 */
	public QanaryMessage process(QanaryMessage myQanaryMessage) {

		logger.info("process: {}", myQanaryMessage);
		// TODO: implement processing of question

		try {
			logger.info("store data in graph {}", myQanaryMessage.get(new URL(QanaryMessage.endpointKey)));
			// TODO: insert data in QanaryMessage.outgraph
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)

		return myQanaryMessage;
	}

	/**
	 * main method will start the web server
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		Properties p = new Properties();
		new SpringApplicationBuilder(QanaryService.class).properties(p).run(args);

	}

}
