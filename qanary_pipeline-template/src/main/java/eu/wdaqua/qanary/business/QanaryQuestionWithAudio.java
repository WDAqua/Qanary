package eu.wdaqua.qanary.business;

import java.net.URI;

import eu.wdaqua.qanary.web.QanaryQuestionAnsweringController;

/**
 * question referring to an audio file
 * 
 * @author AnBo
 *
 */
public class QanaryQuestionWithAudio extends QanaryQuestion {

	public QanaryQuestionWithAudio(URI endpoint, URI graph, URI questionUri,
			QanaryQuestionAnsweringController sparqlOperator) {
		super(endpoint, graph, questionUri, sparqlOperator);
	}

	@Override
	public String[] getAnnotationName() {
		return new String[]{"qa:AnnotationOfAudioRepresentation"};
	}
	
}
