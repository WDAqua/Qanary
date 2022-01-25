package eu.wdaqua.qanary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import eu.wdaqua.qanary.web.messages.RequestQuestionAnsweringProcess;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(name = "contextWithFakeBean")
class QanaryQuestionAnsweringControllerTest {
	
	static {
		// TODO: needs to be replaced by mocked version
		System.setProperty("stardog.url", "https://webengineering.ins.hs-anhalt.de:40159"); 
		System.setProperty("stardog.username", "admin");
		System.setProperty("stardog.password", "admin");
	}
	
	@Autowired
	@Qualifier("QanaryTripleStoreConnectorQanaryInternal")
    private MockMvc mvc;

	@MockBean
	private QanaryTripleStoreConnectorQanaryInternal mockedQanaryTripleStoreConnector;
    
    @Test
	void testRequestQuestionAnsweringProcessToString() throws URISyntaxException {
		RequestQuestionAnsweringProcess qaProcess = new RequestQuestionAnsweringProcess();

		String question = "What is the real name of Batman";
		List<String> componentList = new ArrayList<>();
		componentList.add("NED-DBpediaSpotlight");
		componentList.add("QueryBuilderSimpleRealNameOfSuperHero");
		URI priorConversation = new URI("urn:graph:806261d9-4601-4c8c-8603-926eee707c38");

		qaProcess.setQuestion(question);
		qaProcess.setcomponentlist(componentList);
		qaProcess.setPriorConversation(priorConversation);

		String expected = "RequestQuestionAnsweringProcess " //
				+ " -- question: \"What is the real name of Batman\"" //
				+ " -- componentList: [NED-DBpediaSpotlight, QueryBuilderSimpleRealNameOfSuperHero]" //
				+ " -- priorConversation: urn:graph:806261d9-4601-4c8c-8603-926eee707c38";

		String actual = qaProcess.toString();

		assertEquals(expected, actual);
	}


    @Test
    void testQuestionAnsweringControllerPriorConversation() throws Exception {
        String json = "{\"question\": \"foo?\"}";

//		mockedQanaryTripleStoreConnector = 
//			mock(QanaryTripleStoreConnectorQanaryInternal.class);
		doNothing().when(mockedQanaryTripleStoreConnector).update(anyString());

		URI priorConversation = new URI("urn:priorConversation");
		URL questionUri = new URI("http://localhost/question").toURL();


		// when post is made to endpoint (and question is created)
		mvc.perform(
				MockMvcRequestBuilders.post("/startquestionansweringwithtextquestion")
					.contentType("application/json")
					.content(json)
					.accept(MediaType.APPLICATION_JSON)
				).andExpect(status().is2xxSuccessful());


		// update() 'does nothing' but we can still verify the passed string value
		// ensure that a SPARQL UPDATE query is called and a graph URI is provided as
		// prior conversation
		String queryPart = ".*" //
				+ "<" + questionUri.toString() + ">" //
				+ " qa:priorConversation " //
				+ "<" + priorConversation.toASCIIString() + ">" //
				+ " \\..*";
		verify(mockedQanaryTripleStoreConnector, atLeast(1)).update(matches(queryPart));
    }
}
