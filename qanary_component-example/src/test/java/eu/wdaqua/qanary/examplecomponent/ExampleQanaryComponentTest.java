package eu.wdaqua.qanary.examplecomponent;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import eu.wdaqua.qanary.QanaryMessage;
import eu.wdaqua.qanary.component.QanaryServiceController;
import eu.wdaqua.qanary.config.QanaryConfiguration;
import net.minidev.json.JSONObject;

public class ExampleQanaryComponentTest {

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
		jsonObject.put(QanaryMessage.inGraph, testInGraph);
		jsonObject.put(QanaryMessage.outGraph, testOutGraph);

		// create message from json string
		QanaryMessage requestMessage;
		try {
			requestMessage = new QanaryMessage(jsonObject.toJSONString());
		} catch (MalformedURLException e) {
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

		for (Entry<URL, URL> entry : requestMessage.entrySet()) {
			URL key = entry.getKey();
			int compareResult = entry.getValue().toString().compareTo(resultMessage.get(key).toString());
			assertTrue("check result vs. request: " + key, compareResult == 0);
		}

	}

}
