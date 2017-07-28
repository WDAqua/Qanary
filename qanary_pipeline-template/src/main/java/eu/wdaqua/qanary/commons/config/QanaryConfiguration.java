package eu.wdaqua.qanary.commons.config;

import java.net.URI;

import org.springframework.stereotype.Component;

@Component
public class QanaryConfiguration {
    public static final String description = "/description";
    public static final String annotatequestion = "/annotatequestion";
    public static final String sparql = "/sparql";
    public static final String questionRawDataUrlSuffix = "/raw";
    public static final String questionRawDataProperyName = "raw";

    public static final String endpointKey = "http://qanary/#endpoint";
    public static final String inGraphKey = "http://qanary/#inGraph";
    public static final String outGraphKey = "http://qanary/#outGraph";

    private static URI serviceUri;
    private static URI hostUri;

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

    /**
     * set the URI of the endpoint
     */
    public static void setHostUri(URI uri) {
        hostUri = uri;
    }

    /**
     * get the URI of the endpoint
     */
    public static URI getHostUri() {
        return (hostUri);
    }

}
