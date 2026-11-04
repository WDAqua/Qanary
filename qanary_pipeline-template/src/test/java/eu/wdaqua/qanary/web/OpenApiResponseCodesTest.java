package eu.wdaqua.qanary.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import eu.wdaqua.qanary.QanaryPipeline;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;

/**
 * the OpenAPI documentation states the HTTP status codes the endpoints actually
 * answer with: the endpoints that create a question or a question answering
 * process answer 201 (Created), not 200
 */
@SpringBootTest(classes = QanaryPipeline.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OpenApiResponseCodesTest {

    static {
        System.setProperty("springdoc.api-docs.path", "/api-docs");
    }

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private QanaryTripleStoreProxy mockedQanaryTripleStoreConnector;

    private Set<String> documentedResponseCodes(String path, String method) throws Exception {
        String apiDocs = mvc.perform(MockMvcRequestBuilders.get("/api-docs")).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        JSONObject responses = new JSONObject(apiDocs).getJSONObject("paths").getJSONObject(path)
                .getJSONObject(method).getJSONObject("responses");
        return new TreeSet<>(responses.keySet());
    }

    @ParameterizedTest
    @CsvSource({ //
            "/startquestionansweringwithtextquestion, post", //
            "/startquestionansweringwithtextquestionThroughJson, post", //
            "/startquestionansweringwithaudioquestion, post", //
            "/questionanswering, post", //
            "/question, post", //
            "/question_audio, post" //
    })
    void creatingEndpointsAreDocumentedWithCreated(String path, String method) throws Exception {
        assertEquals(Set.of("201"), documentedResponseCodes(path, method));
    }

    @Test
    void startingAProcessAnswersCreatedAsDocumented() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/startquestionansweringwithtextquestion") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(new JSONObject().put("question", "What is the capital of Germany?").toString()) //
                .with(csrf()) //
                .accept(MediaType.APPLICATION_JSON) //
        ).andExpect(status().isCreated());
    }

    @Test
    void storingAQuestionAnswersCreatedAsDocumented() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/question") //
                .param(QanaryStandardWebParameters.QUESTION, "What is the capital of Germany?") //
                .with(csrf()) //
        ).andExpect(status().isCreated());
    }
}
