package eu.wdaqua.qanary.message;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.json.JSONObject;

public class QanaryMessage extends HashMap<URL, URL> {

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
	public static final String inGraph = "http://qanary/#inGraph";
	// the property URI (key) for inserting the output into the endpoint TODO:
	// move to QanaryConfiguration
	public static final String outGraph = "http://qanary/#outGraph";

	/**
	 * default constructor needed for post communication
	 */
	public QanaryMessage() {
	}

	public QanaryMessage(String jsonString) throws MalformedURLException {
		logger.info("construct QanaryMessage: {}", jsonString);
		JSONObject json = JSONObject.fromObject(jsonString);

		URL endpointKeyURL = new URL(endpointKey);
		URL inGraphKeyURL = new URL(inGraph);
		URL outGraphKeyURL = new URL(outGraph);

		// just for debugging
		URL endpointValueURL = new URL((String) json.get(endpointKeyURL.toString()));
		logger.info("construct endpoint: {}={}", endpointKeyURL, endpointValueURL);

		// assign endpoint value to internal map
		this.put(endpointKeyURL, new URL((String) json.get(endpointKeyURL.toString())));
		logger.info("construct put endpoint value: {}", this.get(endpointKeyURL));

		// assign inGraph value to internal map
		this.put(inGraphKeyURL, new URL((String) json.get(inGraphKeyURL.toString())));
		logger.info("construct put inGraph value: {}", this.get(inGraphKeyURL));

		// assign outGraph value to internal map
		this.put(outGraphKeyURL, new URL((String) json.get(outGraphKeyURL.toString())));
		logger.info("construct put outGraph value: {}", this.get(outGraphKeyURL));

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
