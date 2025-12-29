package eu.wdaqua.qanary;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.web.QanaryStandardWebParameters;
import eu.wdaqua.qanary.web.messages.RequestQuestionAnsweringProcess;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.NestedServletException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(classes = QanaryPipeline.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(name = "contextWithFakeBean")
class QanaryQuestionAnsweringControllerTest {

    static {
        System.setProperty("spring.profiles.active", "test");
        System.setProperty("springdoc.api-docs.path", "/api-docs");
        System.setProperty("qanary.process.allow-additional-triples", "true");
    }

    private final Logger logger = LoggerFactory.getLogger(QanaryQuestionAnsweringControllerTest.class);
    @Autowired
    private MockMvc mvc;
    @MockBean
    private QanaryTripleStoreProxy mockedQanaryTripleStoreConnector;

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

        assertEquals(expected, actual, "\nexpected:\n" + expected + " \n != \nactual:\n" + actual);
    }


    @Test
    void testQuestionAnsweringControllerPriorConversation() throws Exception {

        // mock update() of QanaryTripleStoreConnector to remove its internal logic
        doNothing().when(mockedQanaryTripleStoreConnector).update(anyString());

        // given question with reference to a prior conversation
        URI priorConversation = new URI("urn:priorConversation");
        String json = "{\"" + QanaryStandardWebParameters.QUESTION + "\": \"foo?\", \"" + QanaryStandardWebParameters.PRIORCONVERSATION + "\": \"" + priorConversation.toASCIIString() + "\"}";

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

    /**
     * IMPORTANT: for some reason this test does not work in the GitHub action
     * <p>
     * test availability of api-docs via Web service (required to ensure OpenAPI/Swagger docs availability)
     * <p>
     * problem might be discovered when springdoc-openapi package has a conflict (e.g., v1.6.14)
     *
     * @throws Exception
     */
    @Test
    void testOpenApiDefinitionAvailability() throws Exception {
        String openApiPath = System.getProperty("springdoc.api-docs.path") + ".yaml";
        logger.info("test availability of OpenAPI YAML file at {}", openApiPath);
        MvcResult x = mvc.perform(MockMvcRequestBuilders.get(openApiPath)).andReturn();
        int httpCode = x.getResponse().getStatus();
        if (httpCode >= 200 && httpCode < 400) {
            logger.info("Access to {} results in HTTP {}.", openApiPath, httpCode);
        } else {
            logger.error("Access to {} results in HTTP {}. Check springdoc-openapi!", openApiPath, httpCode);
        }
    }

    @Test
    void testWithoutAdditionalTriples() throws Exception {
        String url = "/startquestionansweringwithtextquestion";

        JSONArray componentList = new org.json.JSONArray();
        componentList.put("OpenTapiocaNED");
        componentList.put("BirthDataQueryBuilderWikidata");

        JSONObject json = new JSONObject();
        json.put("question", "When was Albert Einstein born?");
        json.put("componentlist", componentList);
        json.put("language", "en");

        mvc.perform(MockMvcRequestBuilders.post(url) //
                .contentType("application/json")
                .content(json.toString())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().is2xxSuccessful());
        ;

    }

    @Test
    void testWithEmptyAdditionalTriples() throws Exception {
        String url = "/startquestionansweringwithtextquestion";

        JSONArray componentList = new org.json.JSONArray();
        componentList.put("OpenTapiocaNED");
        componentList.put("BirthDataQueryBuilderWikidata");

        JSONObject json = new JSONObject();
        json.put("question", "When was Albert Einstein born?");
        json.put("componentlist", componentList);
        json.put("language", "en");
        json.put("additionalTriples", "");

        mvc.perform(MockMvcRequestBuilders.post(url) //
                .contentType("application/json")
                .content(json.toString())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().is2xxSuccessful());
        ;

    }

    @Test
    void testWithAdditionalTriple() throws Exception {
        String url = "/startquestionansweringwithtextquestion";

        JSONArray componentList = new org.json.JSONArray();
        componentList.put("OpenTapiocaNED");
        componentList.put("BirthDataQueryBuilderWikidata");

        JSONObject json = new JSONObject();
        json.put("question", "When was Albert Einstein born?");
        json.put("componentlist", componentList);
        json.put("language", "en");
        json.put("additionalTriples", "<urn:s> <urn:p> <urn:o>  .");

        mvc.perform(MockMvcRequestBuilders.post(url) //
                .contentType("application/json")
                .content(json.toString())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().is2xxSuccessful());
        ;

    }

    @Test
    void testWithAdditionalTriples() throws Exception {
        String url = "/startquestionansweringwithtextquestion";

        JSONArray componentList = new org.json.JSONArray();
        componentList.put("OpenTapiocaNED");
        componentList.put("BirthDataQueryBuilderWikidata");

        JSONObject json = new JSONObject();
        json.put("question", "When was Albert Einstein born?");
        json.put("componentlist", componentList);
        json.put("language", "en");
        json.put("additionalTriples", "" //
                + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
                + "PREFIX xyz: <urn:xyz:> " //
                + "<urn:s0> xyz:p0 <urn:o0> ." //
                + "<urn:s1>   <urn:p1> <urn:o1> .     " //
                + "<urn:s2> <urn:p2> \"1\"^^xsd:integer    . " //
                + "<urn:s3> <urn:p3>    \"string\" . " //
                + "<urn:s4> <urn:p4> \"explicit string\"^^xsd:string . " //
                + "<urn:s5>   <urn:p5> <urn:o5>,\"string\" . " //
                + "");

        mvc.perform(MockMvcRequestBuilders.post(url) //
                .contentType("application/json")
                .content(json.toString())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().is2xxSuccessful());
        ;

    }

    @Test
    void testWithBrokenAdditionalTriplesShouldFail() throws Exception {
        String url = "/startquestionansweringwithtextquestion";

        JSONArray componentList = new org.json.JSONArray();
        componentList.put("OpenTapiocaNED");
        componentList.put("BirthDataQueryBuilderWikidata");

        JSONObject json = new JSONObject();
        json.put("question", "When was Albert Einstein born?");
        json.put("componentlist", componentList);
        json.put("language", "en");
        json.put("additionalTriples", "<urn:s <urn:p> <urn:o>  .");

        Assertions.assertThrows(NestedServletException.class, () -> {
            mvc.perform(MockMvcRequestBuilders.post(url) //
                    .contentType("application/json")
                    .content(json.toString())
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
            ).andReturn();
            fail("Request should have failed");
        });

    }

}
