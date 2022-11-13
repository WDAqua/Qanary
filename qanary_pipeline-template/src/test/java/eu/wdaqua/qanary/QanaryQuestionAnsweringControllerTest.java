package eu.wdaqua.qanary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import eu.wdaqua.qanary.web.QanaryStandardWebParameters;
import eu.wdaqua.qanary.web.messages.RequestQuestionAnsweringProcess;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;


@SpringBootTest(classes = QanaryPipeline.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(name = "contextWithFakeBean")
class QanaryQuestionAnsweringControllerTest {

    private final Logger logger = LoggerFactory.getLogger(QanaryQuestionAnsweringControllerTest.class);
	
	@Autowired
    private MockMvc mvc;

	@MockBean
	private QanaryTripleStoreConnector mockedQanaryTripleStoreConnector;
    
    @Test
    void testRequestQuestionAnsweringProcessToStringWithoutLanguage() throws URISyntaxException {
        RequestQuestionAnsweringProcess qaProcess = new RequestQuestionAnsweringProcess();

		// given a text question, two components and an existing priorConversation
		String question = "What is the real name of Batman";
		List<String> componentList = new ArrayList<>();
		componentList.add("NED-DBpediaSpotlight");
		componentList.add("QueryBuilderSimpleRealNameOfSuperHero");
		URI priorConversation = new URI("urn:graph:806261d9-4601-4c8c-8603-926eee707c38");

		qaProcess.setQuestion(question);
		qaProcess.setcomponentlist(componentList);
		qaProcess.setPriorConversation(priorConversation);

        String expected = "RequestQuestionAnsweringProcess " + // 
                " -- question: \"What is the real name of Batman\"" + // 
                " -- componentList: [NED-DBpediaSpotlight, QueryBuilderSimpleRealNameOfSuperHero]" + // 
                " -- priorConversation: urn:graph:806261d9-4601-4c8c-8603-926eee707c38" + // 
                " -- language: null";

        String actual = qaProcess.toString();

        assertEquals(expected, actual);
    }

    @Test
    void testRequestQuestionAnsweringProcessToStringWithLanguage() throws URISyntaxException {
        RequestQuestionAnsweringProcess qaProcess = new RequestQuestionAnsweringProcess();

        String question = "What is the real name of Batman";
        List<String> componentList = new ArrayList<>();
        componentList.add("NED-DBpediaSpotlight");
        componentList.add("QueryBuilderSimpleRealNameOfSuperHero");
        URI priorConversation = new URI("urn:graph:806261d9-4601-4c8c-9999-926eee707c38");

        qaProcess.setQuestion(question);
        qaProcess.setcomponentlist(componentList);
        qaProcess.setPriorConversation(priorConversation);
        qaProcess.setLanguage("es"); // Spanish

        String expected = "RequestQuestionAnsweringProcess " + // 
                " -- question: \"What is the real name of Batman\"" + // 
                " -- componentList: [NED-DBpediaSpotlight, QueryBuilderSimpleRealNameOfSuperHero]" + // 
                " -- priorConversation: urn:graph:806261d9-4601-4c8c-9999-926eee707c38" + // 
                " -- language: es";

        String actual = qaProcess.toString();

		assertEquals(expected, actual);
	}


    @Test
    void testQuestionAnsweringControllerPriorConversation() throws Exception {

		// mock update() of QanaryTripleStoreConnector to remove its internal logic 
		doNothing().when(mockedQanaryTripleStoreConnector).update(anyString()); 

		// given question with reference to a prior conversation
		URI priorConversation = new URI("urn:priorConversation");
		String json = "{\""+QanaryStandardWebParameters.QUESTION+"\": \"foo?\", \""+QanaryStandardWebParameters.PRIORCONVERSATION+"\": \""+ priorConversation.toASCIIString() + "\"}"; 

		// when a successful post request is made to /startquestionansweringwithtextquestion
		// to start the question answering process 
		mvc.perform(
				MockMvcRequestBuilders.post("/startquestionansweringwithtextquestion")
					.contentType("application/json")
					.content(json)
					.accept(MediaType.APPLICATION_JSON)
				).andExpect(status().is2xxSuccessful());

		// then the SPARQL UPDATE query is called and a graph URI is provided as 
		// prior conversation 
		String queryPart = ".*" //
				+ " qa:priorConversation " //
				+ "<" + priorConversation.toASCIIString() + ">" //
				+ " \\..*";

 		// update() 'does nothing' but we can still verify the passed string value
 		// is it called at least once?
		verify(mockedQanaryTripleStoreConnector, atLeast(1)).update(anyString());

 		// is is called at least once with the expected parameter value?
		verify(mockedQanaryTripleStoreConnector, atLeast(1)).update(matches(queryPart));
    }

}
