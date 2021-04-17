package qa.pipeline;

import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import eu.wdaqua.qanary.commons.QanaryUtils;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class QanaryQuestionAnsweringControllerPriorConversationTest {
	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private MockMvc mvc;

	public String getQueryUrl() {
		return "http://localhost:" + port + "/startquestionansweringwithtextquestion";
	}

	@Test
	void testTestEnvironment() {
		assertNotEquals(null, restTemplate, "Test enviroment initialization failed.");
	}

	@Test
	void testQuestionAnsweringControllerPriorConversation() throws Exception {
		String json = "{\"question\": \"foo?\"}";
		try (MockedStatic<QanaryUtils> mockedStatic = Mockito.mockStatic(QanaryUtils.class)) {
			mvc.perform(MockMvcRequestBuilders.post("/startquestionansweringwithtextquestion")
					.contentType("application/json").content(json).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().is2xxSuccessful()); // returns 201
		}
	}

}
