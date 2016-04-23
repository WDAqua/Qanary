package eu.wdaqua.qanary.component.config;

import java.net.URI;

import org.springframework.stereotype.Component;

@Component
public class QanaryConfiguration {
	public static final String description = "/description";

	// TODO should move to commons package
	public static final String annotatequestion = "/annotatequestion";
	public static final String sparql = "/sparql";
	public static final String questionRawDataUrlSuffix = "/rawdata";
	public static final String questionRawDataProperyName = "rawdata";

	public static URI serviceUri;

	/**
	 * set the URI of the endpoint
	 * 
	 * @param uri
	 */
	public static void setServiceUri(URI uri) {
		serviceUri = uri;
	}

	/**
	 * get the URI of the endpoint
	 * 
	 * @param uri
	 */
	public static URI getServiceUri() {
		return (serviceUri);
	}

}
