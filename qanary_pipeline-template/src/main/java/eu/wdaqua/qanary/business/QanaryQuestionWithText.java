package eu.wdaqua.qanary.business;

import java.net.URI;

import eu.wdaqua.qanary.web.QanaryQuestionAnsweringController;

/**
 * represents a textual question
 * 
 * @author AnBo
 *
 */
public class QanaryQuestionWithText extends QanaryQuestion {

	public QanaryQuestionWithText(URI endpoint, URI graph, URI questionUri,
			QanaryQuestionAnsweringController sparqlOperator) {
		super(endpoint, graph, questionUri, sparqlOperator);
	}

	@Override
	public String[] getAnnotationName() {
		return new String[] { "qa:AnnotationOfTextRepresentation" };
	}

}
