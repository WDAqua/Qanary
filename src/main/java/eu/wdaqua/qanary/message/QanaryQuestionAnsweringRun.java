package eu.wdaqua.qanary.message;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.web.QanaryQuestionAnsweringController;

/**
 * represents the message send back to the client after starting a QA process
 * 
 * @author AnBo
 *
 */
public class QanaryQuestionAnsweringRun {

	final URI questionAnsweringRunUri;

	public QanaryQuestionAnsweringRun(UUID runId, QanaryConfigurator configurator) throws URISyntaxException {
		this.questionAnsweringRunUri = new URI(configurator.getHost() + ":" + configurator.getPort()
				+ QanaryQuestionAnsweringController.QUESTIONANSWERING + "/" + runId.toString());
	}

	public String toString() {
		return this.getQuestionAnsweringRunUri().toASCIIString();
	}

	public URI getQuestionAnsweringRunUri() {
		return this.questionAnsweringRunUri;
	}
}
