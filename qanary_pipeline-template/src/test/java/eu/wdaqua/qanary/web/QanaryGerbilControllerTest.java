package eu.wdaqua.qanary.web;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.inject.Inject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.web.messages.GerbilExecuteResponse;

/**
 * Test the compatibility of the Gerbil response class
 * 
 * @author AnBo
 *
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(name = "contextWithFakeBean")
class QanaryGerbilControllerTest {

	private static final Logger logger = LoggerFactory.getLogger(QanaryGerbilControllerTest.class);

	private MockMvc mockMvc;

	@Inject
	QanaryGerbilController controller;

	@MockBean
	private QanaryTripleStoreConnector mockedQanaryTripleStoreConnector;

	/**
	 * test the compatibility of the gold standard JSON object and the implemented
	 * message class
	 * 
	 * @throws JSONException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	@Test
	void testGerbilEndpointMessageObject() throws JSONException, JsonMappingException, JsonProcessingException {

		String questionText = "Who ...";
		String language = "en";
		String sparqlQueryString = "SELECT * FROM ...";
		String jsonAnswerString = "{\"aaa\":\"42\"}";
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

		// this object is acceptable by Gerbil
		JSONObject goldObj = this.getGoldStandardMessageObject(questionText, language, sparqlQueryString,
				jsonAnswerString);

		GerbilExecuteResponse newObj = new GerbilExecuteResponse(objectMapper, questionText, language,
				sparqlQueryString, jsonAnswerString); // ObjectMapper objectMapper

		String jsonInString = objectMapper.writeValueAsString(newObj);
		JSONTokener tokener = new JSONTokener(jsonInString);

		// make sure everything can be parsed
		JSONObject newObjParsed = new JSONObject(tokener);

		logger.debug("goldObj:\n{}\nnewObj:\n{}", goldObj, newObjParsed);

		// compare objects
		assertEquals(goldObj.toString(), newObjParsed.toString());
	}

	/**
	 * creates a JSON object that was already accepted by the Gerbil interface
	 * 
	 * @param questionText
	 * @param language
	 * @param sparqlQueryString
	 * @param jsonAnswerString
	 * @return
	 * @throws JSONException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	private JSONObject getGoldStandardMessageObject(String questionText, String language, String sparqlQueryString,
			String jsonAnswerString) throws JSONException, JsonMappingException, JsonProcessingException {
		// create the question array and object and add the content
		JSONObject questionData = new JSONObject();
		questionData.put("language", language);
		questionData.put("string", questionText);

		JSONArray questionDataArray = new JSONArray();
		questionDataArray.put(questionData);

		// create the query object and add the content
		JSONObject queryObj = new JSONObject();
		queryObj.put("sparql", sparqlQueryString);

		// transform the answer String to JSON, if an answer was found
		JSONObject answersObj = new JSONObject();
		if (jsonAnswerString != null && jsonAnswerString.length() > 0) {
			JSONTokener tokener = new JSONTokener(jsonAnswerString);
			answersObj = new JSONObject(tokener);
		}

		// create the answers array and add the content
		JSONArray answersArray = new JSONArray();
		answersArray.put(answersObj);

		// create the wrapper object and the array and add all JSON to it
		JSONArray questionsArray = new JSONArray();
		JSONObject questionObject = new JSONObject();
		questionObject.put("question", questionDataArray);
		questionObject.put("query", queryObj);
		questionObject.put("answers", answersArray);
		questionsArray.put(questionObject);

		// add all to the wrapper object
		JSONObject obj = new JSONObject();
		obj.put("questions", questionsArray);

		return obj;
	}

	/**
	 * TODO: need to mock /startquestionansweringwithtextquestion 
	 * 
	 * @throws JSONException
	 */
	void testEndpoint() throws JSONException {
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
		viewResolver.setPrefix("/WEB-INF/jsp/view/");
		viewResolver.setSuffix(".jsp");
		mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();

		String endpoint = QanaryGerbilController.GERBILEXECUTE + "COMPONENTNAME1";
		
		try {
			MvcResult res = mockMvc.perform( //
					post(endpoint) //
							.param("query", "What is the capital of France?") //
							.contentType(MediaType.APPLICATION_JSON) //
					) //
					.andExpect(status().isOk()) // ok
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)) //
					.andReturn();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}

	}
}
