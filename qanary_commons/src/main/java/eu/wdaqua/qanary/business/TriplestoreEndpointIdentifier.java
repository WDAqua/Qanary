package eu.wdaqua.qanary.business;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * represents specific behavior for triplestore endpoints currently only while
 * covering more cases later this should be refactored to a factory or else
 * 
 * @author AnBo
 *
 */
@Component
public class TriplestoreEndpointIdentifier {
	private static final Logger logger = LoggerFactory.getLogger(TriplestoreEndpointIdentifier.class);

	@Value("${qanary.triplestore.stardog5}")
	private boolean stardog5;

	/**
	 * represents the specific behavior of Stardog, w.r.t. to this bug
	 * https://community.stardog.com/t/query-and-update-endpoint-stardog-5/530/2
	 * 
	 * @return
	 */
	private boolean isStardog5orHigher() {
		return this.stardog5;
	}

	/**
	 * if required by the configuration the endpoint URL is rewritten
	 * 
	 * @param url
	 * @return
	 * @throws URISyntaxException 
	 */
	public URI getSelectEndpoint(URI uri) throws URISyntaxException {
		if (this.isStardog5orHigher()) {
			uri = new URI(uri.toString() + "/query");
			logger.info("endpoint changed due to Stardog 5+: {}", uri);
		}

		return uri;
	}

	/**
	 * if required by the configuration the endpoint URL is rewritten
	 * 
	 * @param url
	 * @return
	 * @throws URISyntaxException 
	 */
	public URI getAskEndpoint(URI uri) throws URISyntaxException {
		if (this.isStardog5orHigher()) {
			uri = this.getSelectEndpoint(uri);
		}

		return uri;
	}

	/**
	 * if required by the configuration the endpoint URL is rewritten
	 * 
	 * @param url
	 * @return
	 * @throws URISyntaxException 
	 */
	public URI getUpdateEndpoint(URI uri) throws URISyntaxException {
		if (this.isStardog5orHigher()) {
			uri = new URI(uri.toString() + "/update");
			logger.info("endpoint changed due to Stardog 5+: {}", uri);
		}

		return uri;
	}

	/**
	 * if required by the configuration the endpoint URL is rewritten
	 * 
	 * @param url
	 * @return
	 * @throws URISyntaxException 
	 */
	public URI getCreateEndpoint(URI url) throws URISyntaxException {
		if (this.isStardog5orHigher()) {
			url = this.getUpdateEndpoint(url);
		}

		return url;
	}

	/**
	 * if required by the configuration the endpoint URL is rewritten
	 * 
	 * @param url
	 * @return
	 * @throws URISyntaxException 
	 */
	public URI getLoadEndpoint(URI uri) throws URISyntaxException {
		if (this.isStardog5orHigher()) {
			uri = this.getUpdateEndpoint(uri);
		}

		return uri;
	}

}
