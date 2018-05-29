package eu.wdaqua.qanary.commons;

import java.net.URISyntaxException;
import java.net.URL;

import eu.wdaqua.qanary.business.QanaryConfigurator;

public class QanaryQuestionTextual extends QanaryQuestion<Object> {

	public QanaryQuestionTextual(QanaryMessage qanaryMessage) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException {
		super(qanaryMessage);
		// ensure that the question is marked as textual question
		this.putAnnotationOfTextRepresentation();
	}

	public QanaryQuestionTextual(URL questionUrl, QanaryConfigurator qanaryConfigurator) throws URISyntaxException, QanaryExceptionNoOrMultipleQuestions {
		super(questionUrl, qanaryConfigurator);
		// ensure that the question is marked as textual question
		this.putAnnotationOfTextRepresentation();
	}

}
