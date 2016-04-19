package TestQanaryServiceController;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.springframework.util.Assert;

import eu.wdaqua.qanary.LuceneSpotter.LuceneSpotter;
import eu.wdaqua.qanary.component.QanaryMessage;
import net.minidev.json.JSONObject;

public class TestMain {

	@Test
	public void test() throws URISyntaxException {
		LuceneSpotter l = new  LuceneSpotter();
		String testEndPoint = "http://qanary.test/endpoint";
		String testInGraph = "http://qanary.test/graph/in";
		String testOutGraph = "http://qanary.test/graph/out";

		// create a JSON object with required properties
		JSONObject jsonObject = new JSONObject();
		// TODO: replace key by URLs of the qa vocabulary
		jsonObject.put(QanaryMessage.endpointKey, testEndPoint);
		jsonObject.put(QanaryMessage.inGraphKey, testInGraph);
		jsonObject.put(QanaryMessage.outGraphKey, testOutGraph);

		// create message from json string
		QanaryMessage message;
		try {
			message = new QanaryMessage(jsonObject.toJSONString());
			l.process(message);
			
			URI endpointKeyUrlFromMessage = message.getValues().get(new URI(QanaryMessage.endpointKey));
			Assert.notNull(endpointKeyUrlFromMessage);

			URI endpointKeyUrlFromHere = new URI(testEndPoint);

			// TODO: more tests to ensure mechanism
			assertTrue(endpointKeyUrlFromHere.toString().compareTo(endpointKeyUrlFromMessage.toString()) == 0);
			

		} catch (URISyntaxException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		fail("Not yet implemented");
	}

}
