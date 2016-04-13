package eu.wdaqua.qanary.component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.json.JSONObject;

public class QanaryMessage extends HashMap<URI, URI> {

	/**
	 * The serialization runtime associates with each serializable class a
	 * version number, called a serialVersionUID, which is used during
	 * deserialization to verify that the sender and receiver of a serialized
	 * object have loaded classes for that object that are compatible with
	 * respect to serialization
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

	/**
	 * constructor fulfilling the communication requirements
	 * 
	 * @throws URISyntaxException
	 */
	public QanaryMessage(URI endpoint, URI inGraph) throws URISyntaxException {
		// reuse inGraph as outGraph (just for convenience)
		this.setValues(endpoint, inGraph, inGraph);
	}

	/**
	 * constructor fulfilling the communication requirements
	 * 
	 * @param endpoint
	 * @param inGraph
	 * @param outGraph
	 * @throws URISyntaxException
	 */
	public QanaryMessage(URI endpoint, URI inGraph, URI outGraph) throws URISyntaxException {
		this.setValues(endpoint, inGraph, outGraph);
	}

	/**
	 * set all values for valid message
	 * 
	 * @param endpoint
	 * @param inGraph
	 * @param outGraph
	 * @throws URISyntaxException
	 */
	public void setValues(URI endpoint, URI inGraph, URI outGraph) throws URISyntaxException {
		URI keyEndpoint = new URI(endpointKey);
		this.put(keyEndpoint, endpoint);

		URI keyInGraph = new URI(inGraphKey);
		this.put(keyInGraph, inGraph);

		URI keyOutGraph = new URI(outGraphKey);
		this.put(keyOutGraph, outGraph);
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
	 * 
	 * @param key
	 * @return
	 */
	private URI getValue(String key) {
		try {
			return this.get(new URI(key));
		} catch (URISyntaxException e) {
			// should never ever happen or the whole Qanary pipeline is broken
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * parses a jsonString and sets correct values for valid Qanary message
	 * 
	 * @param jsonString
	 * @throws URISyntaxException
	 */
	public QanaryMessage(String jsonString) throws URISyntaxException {
		logger.info("construct QanaryMessage: {}", jsonString);
		JSONObject json = JSONObject.fromObject(jsonString);

		URI endpointValue = new URI((String) json.get(endpointKey));
		URI inGraphValue = new URI((String) json.get(inGraphKey));
		URI outGraphValue = new URI((String) json.get(outGraphKey));

		this.setValues(endpointValue, inGraphValue, outGraphValue);
	}

	public QanaryMessage(URI endpoint, String namedGraph) {
		// TODO Auto-generated constructor stub
	}

	public String asJsonString() {
		try {
			final ObjectMapper mapper = new ObjectMapper();
			final String jsonContent = mapper.writeValueAsString(this);
			return jsonContent;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}