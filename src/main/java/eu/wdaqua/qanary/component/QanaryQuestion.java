package eu.wdaqua.qanary.component;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.component.config.QanaryConfiguration;

public class QanaryQuestion<T> {

    private static final Logger logger = LoggerFactory.getLogger(QanaryQuestion.class);

    private final URI uri;
    private T raw;

    public QanaryQuestion(URI questionUri) {
        this.uri = questionUri;
    }

    /**
     * returns the URI of the question
     */
    public URI getUri() {
        return this.uri;
    }

    /**
     * returns the raw data of the question fetched from the URI provided via the constructor, the
     * result is cached to prevent unnecessary calls to remote services
     *
     * TODO: replace with Spring rest client using the message class from the QanaryPipeline
     */
    public T getRawData() throws URISyntaxException {

        if (this.raw == null) {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(this.getUri(), String.class);
            logger.debug("while calling {} received response: {}", this.getUri(), response.getBody(), response);

            JsonObject body = JSON.parse(response.getBody());

            String uriString = body.get(QanaryConfiguration.questionRawDataProperyName).getAsString().toString().trim();
            if (uriString.startsWith("\"")) {
                uriString = uriString.substring(1);
            }
            if (uriString.endsWith("\"")) {
                uriString = uriString.substring(0, uriString.length() - 1);
            }

            // TODO catch exception here and throw an own one
            logger.debug("{}: <{}>", QanaryConfiguration.questionRawDataProperyName, uriString);
            URI rawUri = new URI(uriString);
            ResponseEntity<String> responseRaw = restTemplate.getForEntity(rawUri, String.class);
            logger.debug("raw data fetched from {} is \"{}\"", rawUri, responseRaw);

            this.raw = (T) responseRaw.getBody();

            if (this.raw == null) {
                logger.warn("fetched raw data from {}, result is null: {}", rawUri, responseRaw.getBody());
            }
        }
        return this.raw;
    }

}
