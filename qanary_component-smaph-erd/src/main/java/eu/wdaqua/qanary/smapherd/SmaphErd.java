package eu.wdaqua.qanary.smapherd;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

@Component
public class SmaphErd extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(SmaphErd.class);

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) {
		logger.info("process: {}", myQanaryMessage);
		// TODO: implement processing of question

		try {
			logger.info("store data in graph {}", myQanaryMessage.getValues().get(new URL(QanaryMessage.endpointKey)));
			// TODO: insert data in QanaryMessage.outgraph
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)

		return myQanaryMessage;
	}

}
