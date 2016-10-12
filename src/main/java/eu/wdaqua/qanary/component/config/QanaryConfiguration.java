package eu.wdaqua.qanary.component.config;

import java.net.URI;

import org.springframework.stereotype.Component;

@Component
public class QanaryConfiguration {
    public static final String description = "/description";

    // TODO should move to commons package
    public static final String annotatequestion = "/annotatequestion";
    public static final String sparql = "/sparql";
    public static final String questionRawDataUrlSuffix = "/raw";
    public static final String questionRawDataProperyName = "raw";

    private static URI serviceUri;

    /**
     * set the URI of the endpoint
     */
    public static void setServiceUri(URI uri) {
        serviceUri = uri;
    }

    /**
     * get the URI of the endpoint
     */
    public static URI getServiceUri() {
        return (serviceUri);
    }

}
