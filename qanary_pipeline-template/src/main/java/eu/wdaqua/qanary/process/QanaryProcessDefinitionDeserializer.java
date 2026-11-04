package eu.wdaqua.qanary.process;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * reads the {@code componentlist} of a JSON request into a
 * {@link QanaryProcessDefinition}
 * <p>
 * The JSON value is read into lists, maps, and strings first, which are then
 * validated by {@link QanaryProcessDefinition#fromJson(Object)}. Reading it
 * here (instead of into a map) makes a stream name that is used twice in the
 * same parallel block visible: a map would silently keep only the last stream.
 */
public class QanaryProcessDefinitionDeserializer extends ValueDeserializer<QanaryProcessDefinition> {

	@Override
	public QanaryProcessDefinition deserialize(JsonParser parser, DeserializationContext context) {
		return QanaryProcessDefinition.fromJson(read(parser));
	}

	/**
	 * {@code "componentlist": null} is the empty process definition
	 */
	@Override
	public QanaryProcessDefinition getNullValue(DeserializationContext context) {
		return QanaryProcessDefinition.EMPTY;
	}

	/**
	 * read the JSON value starting at the current token; afterwards, the parser is
	 * at the last token of the value
	 */
	private static Object read(JsonParser parser) {
		JsonToken token = parser.currentToken();
		if (token == JsonToken.START_ARRAY) {
			List<Object> elements = new ArrayList<>();
			while (parser.nextToken() != JsonToken.END_ARRAY) {
				elements.add(read(parser));
			}
			return elements;
		} else if (token == JsonToken.START_OBJECT) {
			Map<String, Object> properties = new LinkedHashMap<>();
			for (String name = parser.nextName(); name != null; name = parser.nextName()) {
				parser.nextToken();
				if (properties.containsKey(name)) {
					throw QanaryProcessDefinition.duplicateStreamName(name);
				}
				properties.put(name, read(parser));
			}
			return properties;
		} else if (token == JsonToken.VALUE_STRING) {
			return parser.getString();
		} else if (token == JsonToken.VALUE_NULL) {
			return null;
		} else if (token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
			return parser.getBooleanValue();
		} else {
			// the only other values are numbers; like booleans, they are kept for the error
			// message (they are not strings, so they are not taken as component names)
			return parser.getNumberValue();
		}
	}
}
