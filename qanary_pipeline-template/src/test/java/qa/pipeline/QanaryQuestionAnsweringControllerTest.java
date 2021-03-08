package qa.pipeline;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;
import eu.wdaqua.qanary.web.QanaryQuestionAnsweringController;
import eu.wdaqua.qanary.web.messages.RequestQuestionAnsweringProcess;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@RunWith(SpringRunner.class)
@WebAppConfiguration
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
        try (MockedStatic<QanaryUtils> mockedStatic = Mockito.mockStatic(QanaryUtils.class)) {
            mvc.perform(
                    MockMvcRequestBuilders.post("/startquestionansweringwithtextquestion")
                            .contentType("application/json")
                            .param("question", "foo?")
                            .accept(MediaType.APPLICATION_JSON)
            ).andExpect(status().isOk()); // fails with status 404

            // todo: assert prior conversation stored
            mockedStatic.verify(() -> QanaryUtils.loadTripleStore(matches(".*"), any(QanaryConfigurator.class)),
                    times(1));
        }
    }

    @Test
    @Ignore
    void testPriorConversationController() throws Exception {
        QanaryQuestionAnsweringController controller = new QanaryQuestionAnsweringController(
                null,
                null,
                null,
                null,
                null);
        String testQuestion = "foo?";
        URI priorConversation = new URI("urn:priorConversation");

        try (MockedStatic<QanaryUtils> mockedStatic = Mockito.mockStatic(QanaryUtils.class)) {
            ResponseEntity<?> response = controller.startquestionansweringwithtextquestion(
                    testQuestion,
                    null,
                    null,
                    null,
                    priorConversation);

            // todo: assert prior conversation stored
            mockedStatic.verify(() -> QanaryUtils.loadTripleStore(matches(".*"), any(QanaryConfigurator.class)),
                    times(1));
        }
    }
}
