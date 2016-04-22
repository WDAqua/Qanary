package eu.wdaqua.qanary.component;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.atlas.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class QanaryQuestion<T> {

	private static final Logger logger = LoggerFactory.getLogger(QanaryQuestion.class);

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
	 * @throws URISyntaxException
	 */
	public T getRawData() throws URISyntaxException {

		if (this.raw == null) {
			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<JsonObject> response = restTemplate.getForEntity(this.getUri(), JsonObject.class);

			// TODO catch exception here and throw an own one
			URI rawUri = new URI(response.getBody().get("raw").toString());

			// TODO
		}
		return this.raw;
	}

}
