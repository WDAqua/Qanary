package eu.wdaqua.qanary.component;

import java.net.URI;

public class QanaryQuestion<T> {

	private final URI uri;
	private T raw;

	public QanaryQuestion(URI questionUri) {
		this.uri = questionUri;
	}

	/**
	 * returns the URI of the question
	 * 
	 * @return
	 */
	public URI getUri() {
		return this.uri;
	}

	/**
	 * returns the raw data of the question fetched from the URI provided via
	 * the constructor, the result is cached to prevent unnecessary calls to
	 * remote services
	 *
	 * @return
	 */
	public T getRawData() {
		return this.raw;
	}

}
