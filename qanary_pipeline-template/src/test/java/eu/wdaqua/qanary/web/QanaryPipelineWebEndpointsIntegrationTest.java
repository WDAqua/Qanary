package eu.wdaqua.qanary.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;

/**
 * Integration test that boots the full {@link eu.wdaqua.qanary.QanaryPipeline}
 * Spring context (random port, real web/security/Thymeleaf/springdoc stack) and
 * drives the read-only / view web endpoints through MockMvc. The triple store is
 * mocked ({@link QanaryTripleStoreProxy}) so no external store is required; the
 * focus is the controller + Spring MVC wiring, not query results.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class QanaryPipelineWebEndpointsIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private QanaryTripleStoreProxy mockedQanaryTripleStoreConnector;

    // ---- embedded /qa frontend (QanaryEmbeddedQaWebFrontendController) -------

    @Test
    void qaFrontendForwardsToBundledSinglePageApp() throws Exception {
        mvc.perform(get("/qa"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/qanary-ui/index.html"));
    }

    // ---- web helper views (QanaryWebHelperController) ------------------------

    @Test
    void webHelperViewsRender() throws Exception {
        mvc.perform(get("/description")).andExpect(status().isOk());
        mvc.perform(get("/inputtextquestion")).andExpect(status().isOk());
        mvc.perform(get("/startquestionanswering")).andExpect(status().isOk());
    }

    // ---- Gerbil endpoint generation (QanaryGerbilController) ------------------

    @Test
    void gerbilFormRenders() throws Exception {
        mvc.perform(get("/gerbil")).andExpect(status().isOk());
    }

    @Test
    void gerbilGeneratorWithoutComponentsAsksForComponents() throws Exception {
        mvc.perform(post("/gerbil").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void gerbilGeneratorWithComponentsBuildsEndpointUrl() throws Exception {
        mvc.perform(post("/gerbil")
                .param("componentlist", "NED-DBpediaSpotlight", "QB-SimpleRealNameOfSuperHero")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    // ---- SPARQL protocol controller (QanarySparqlProtocolController) ----------

    @Test
    void sparqlEndpointTesterReportsConnectivity() throws Exception {
        // mocked triple store returns no rows -> "accessible but no triples" (HTTP 200)
        mvc.perform(get("/checktriplestoreconnection"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("accessible")));
    }

    @Test
    void sparqlHtmlPageRenders() throws Exception {
        mvc.perform(get("/sparql").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }
}
