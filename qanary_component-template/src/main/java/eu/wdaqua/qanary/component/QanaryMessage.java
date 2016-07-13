package eu.wdaqua.qanary.component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

// TODO: needs to be moved to pipeline project
public class QanaryMessage {

    /**
     * The serialization runtime associates with each serializable class a version number, called a
     * serialVersionUID, which is used during deserialization to verify that the sender and receiver
     * of a serialized object have loaded classes for that object that are compatible with respect
     * to serialization
     */
    private static final long serialVersionUID = 42L;

    private static final Logger logger = LoggerFactory.getLogger(QanaryMessage.class);

    // the property URI (key) for accessing the endpoint, TODO: move to
    // QanaryConfiguration
    public static final String endpointKey = "http://qanary/#endpoint";
    // the property URI (key) for accessing the input data at the endpoint TODO:
    // move to QanaryConfiguration
    public static final String inGraphKey = "http://qanary/#inGraph";
    // the property URI (key) for inserting the output into the endpoint TODO:
    // move to QanaryConfiguration
    public static final String outGraphKey = "http://qanary/#outGraph";

    private Map<URI, URI> values;

    /**
     * constructor fulfilling the communication requirements
     */
    public QanaryMessage(URI endpoint, URI inGraph) throws URISyntaxException {
        // reuse inGraph as outGraph (just for convenience)
        this.setValues(endpoint, inGraph, inGraph);
    }

    /**
     * constructor fulfilling the communication requirements
     */
    public QanaryMessage(URI endpoint, URI inGraph, URI outGraph) throws URISyntaxException {
        this.setValues(endpoint, inGraph, outGraph);
    }

    /**
     * set all values for valid message
     */
    public void setValues(URI endpoint, URI inGraph, URI outGraph) throws URISyntaxException {
        this.values = new HashMap<>();

        URI keyEndpoint = new URI(endpointKey);
        this.values.put(keyEndpoint, endpoint);

        URI keyInGraph = new URI(inGraphKey);
        this.values.put(keyInGraph, inGraph);

        URI keyOutGraph = new URI(outGraphKey);
        this.values.put(keyOutGraph, outGraph);
    }

    public URI getEndpoint() {
        return this.getValue(endpointKey);
    }

    public URI getInGraph() {
        return this.getValue(inGraphKey);
    }

    public URI getOutGraph() {
        return this.getValue(outGraphKey);
    }

    /**
     * returns a URI for the given key from the map
     */
    private URI getValue(String key) {
        try {
            return this.values.get(new URI(key));
        } catch (URISyntaxException e) {
            // should never ever happen or the whole Qanary pipeline is broken
            e.printStackTrace();
            return null;
        }
    }

    /**
     * parses a jsonString and sets correct values for valid Qanary message
     */
    public QanaryMessage(String jsonString) throws URISyntaxException {
        logger.info("construct QanaryMessage: {}", jsonString);
        org.json.JSONObject json = new org.json.JSONObject(jsonString);
        logger.info("constructed json: {}", json.toString());

        URI endpointValue = new URI((String) json.getJSONObject("values").get(endpointKey));
        URI inGraphValue = new URI((String) json.getJSONObject("values").get(inGraphKey));
        URI outGraphValue = new URI((String) json.getJSONObject("values").get(outGraphKey));

        this.setValues(endpointValue, inGraphValue, outGraphValue);
    }

    private QanaryMessage(URI endpoint, String namedGraph) throws URISyntaxException {
        this.setValues(endpoint, new URI(namedGraph), new URI(namedGraph));
    }

    // public QanaryMessage(Object x) throws URISyntaxException {
    // this.setValues(new URI("urn:x"), new URI("urn:y"), new URI("urn:z"));
    // }

    public String asJsonString() {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<URI, URI> getValues() {
        return this.values;
    }

}