package eu.wdaqua.qanary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.atLeast;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.web.messages.RequestQuestionAnsweringProcess;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class QanaryQuestionAnsweringControllerTest {
    @Autowired
    private MockMvc mvc;

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

        String expected = "RequestQuestionAnsweringProcess " +
                " -- question: \"What is the real name of Batman\"" +
                " -- componentList: [NED-DBpediaSpotlight, QueryBuilderSimpleRealNameOfSuperHero]" +
                " -- priorConversation: urn:graph:806261d9-4601-4c8c-8603-926eee707c38";

        String actual = qaProcess.toString();

        assertEquals(expected, actual);
    }

    @Test
    void testQuestionAnsweringControllerPriorConversation() throws Exception {
        String json = "{\"question\": \"foo?\"}";
        try (MockedStatic<QanaryUtils> mockedStatic = Mockito.mockStatic(QanaryUtils.class)) {
            mvc.perform(
                    MockMvcRequestBuilders.post("/startquestionansweringwithtextquestion")
                            .contentType("application/json")
                            .content(json)
                            .accept(MediaType.APPLICATION_JSON)
            ).andExpect(status().is2xxSuccessful()); 

            // todo: assert prior conversation stored
            mockedStatic.verify(() -> QanaryUtils.loadTripleStore(matches(".*"), any(QanaryConfigurator.class)),
            		atLeast(1));
        }
    }
}
