package eu.wdaqua.qanary.message;

import java.net.URI;

import org.springframework.boot.orm.jpa.EntityScan;

/**
 * message object send back to client by /question endpoint in
 * {@link QanaryPipelineController}
 * 
 * @author AnBo
 *
 */
@EntityScan
public class QanaryQuestionCreated {
	private final URI questionURI;

	public QanaryQuestionCreated(URI questionURI) {
		this.questionURI = questionURI;
	}

	public URI getQuestionURI() {
		return this.questionURI;
	}
}
