package eu.wdaqua.qanary.trivial.batmanquestion.annotator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;
import eu.wdaqua.qanary.component.QanaryQuestion;
import eu.wdaqua.qanary.component.QanaryUtils;

@Component
public class BatmanQuestionAnnotator extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(BatmanQuestionAnnotator.class);

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * 
	 * @throws Exception
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

		logger.info("process: {}", myQanaryMessage);
		logger.info("received: sparql Endpoint: {}, inGraph: {}, outGraph: {}", myQanaryMessage.getEndpoint(),
				myQanaryMessage.getInGraph(), myQanaryMessage.getOutGraph());

		// TODO: implement processing of question
		// TODO: implement this (custom for every component)

		// TODO: wait for the fully working implementation in QanaryComponent
		QanaryUtils utils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> question = utils.getQuestion();
		logger.info("question: {} from {}", question.getUri(), question.getRawData());

		logger.info("apply vocabulary alignment on outgraph for the Batman question");

		return myQanaryMessage;
	}

}