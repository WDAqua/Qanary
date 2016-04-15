package eu.wdaqua.qanary.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import eu.wdaqua.qanary.component.QanaryMessage;
import eu.wdaqua.qanary.component.QanaryService;
import eu.wdaqua.qanary.component.QanaryServiceController;
import eu.wdaqua.qanary.component.config.QanaryConfiguration;
import net.minidev.json.JSONObject;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { QanaryService.class }, loader = SpringApplicationContextLoader.class)
@WebAppConfiguration
public class TestQanaryServiceController {

	private static final Logger logger = LoggerFactory.getLogger(TestQanaryServiceController.class);

	@Inject
	QanaryServiceController controller;

	private MockMvc mockMvc;

	/**
	 * initialize local controller enabled for tests
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
		viewResolver.setPrefix("/WEB-INF/jsp/view/");
		viewResolver.setSuffix(".jsp");

		mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();
	}

	/**
	 * test description interface
	 */
	@Test
	public void testDescriptionAvailable() {

		MvcResult result;
		try {
			result = mockMvc.perform(get(QanaryConfiguration.description)) // fetch
					.andExpect(status().isOk()) // HTTP 200
					.andExpect(content().contentType(MediaType.TEXT_HTML)) //
					.andReturn(); // HTML
			assert (result.getRequest().getContentLength() > 0);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * send and receive message a JSON message to
	 * QanaryConfiguration.annotatequestion, check if the values are the same
	 */
	@Test
	public void testMessageReceiveAndSend() {
		String testEndPoint = "http://qanary.test/endpoint";
		String testInGraph = "http://qanary.test/graph/in";
		String testOutGraph = "http://qanary.test/graph/out";

		// create a JSON object with required properties
		JSONObject jsonObject = new JSONObject();
		// TODO: replace key by URLs of the qa vocabulary
		jsonObject.put(QanaryMessage.endpointKey, testEndPoint);
		jsonObject.put("ingraph", testInGraph);
		jsonObject.put("outgraph", testOutGraph);

		// create message from json string
		QanaryMessage requestMessage;
		try {
			requestMessage = new QanaryMessage(jsonObject.toJSONString());
		} catch (URISyntaxException e) {
			fail(e.getMessage());
			return;
		}

		// check the response
		MvcResult res;
		try {
			res = mockMvc
					.perform( //
							post(QanaryConfiguration.annotatequestion) //
									.content(requestMessage.asJsonString()) //
									.contentType(MediaType.APPLICATION_JSON))
					.andExpect(status().is2xxSuccessful()) //
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)) //
					.andReturn();
		} catch (Exception e) {
			fail(e.getMessage());
			return;
		}

		// check the values of all messages, should be equal if dummy
		// implementation is used
		QanaryMessage resultMessage;
		try {
			resultMessage = new QanaryMessage(res.getResponse().getContentAsString());
		} catch (Exception e) {
			fail(e.getMessage());
			return;
		}

		for (Entry<URI, URI> entry : requestMessage.getValues().entrySet()) {
			URI key = entry.getKey();
			int compareResult = entry.getValue().toString().compareTo(resultMessage.getValues().get(key).toString());
			assertTrue("check result vs. request: " + key, compareResult == 0);
		}

	}

	/**
	 * test correct message format
	 */
	@Test
	public void testMessageFromJson() {

		String testEndPoint = "http://qanary.test/endpoint";
		String testInGraph = "http://qanary.test/graph/in";
		String testOutGraph = "http://qanary.test/graph/out";

		// create a JSON object with required properties
		JSONObject jsonObject = new JSONObject();
		// TODO: replace key by URLs of the qa vocabulary
		jsonObject.put(QanaryMessage.endpointKey, testEndPoint);
		jsonObject.put("ingraph", testInGraph);
		jsonObject.put("outgraph", testOutGraph);

		// create message from json string
		QanaryMessage message;
		try {
			message = new QanaryMessage(jsonObject.toJSONString());

			URI endpointKeyUrlFromMessage = message.getValues().get(new URI(QanaryMessage.endpointKey));
			Assert.notNull(endpointKeyUrlFromMessage);

			URI endpointKeyUrlFromHere = new URI(testEndPoint);

			// TODO: more tests to ensure mechanism
			assertTrue(endpointKeyUrlFromHere.toString().compareTo(endpointKeyUrlFromMessage.toString()) == 0);

		} catch (URISyntaxException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testAnnotation() {

		// QanaryMessage message = new QanaryMessage()
		// mockMvc.perform
	}

}
