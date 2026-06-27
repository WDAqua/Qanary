package eu.wdaqua.qanary.web;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ContextConfiguration(name = "contextWithFakeBean")
@TestPropertySource(properties = {"configuration.username=qanary", "configuration.password=mySecret"})
class QanaryPipelineConfigurationAccessTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private QanaryTripleStoreProxy mockedQanaryTripleStoreConnector;

//    static {
//        System.setProperty("configuration.username", "qanary");
//        System.setProperty("configuration.password", "mySecret");
//    }

    @Test
    void testQanaryConfigurationAccessControl() throws Exception {
        mvc.perform(
                        MockMvcRequestBuilders.get("/configuration").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(302))
                .andExpect(status().is3xxRedirection())
                // Spring Security 7 issues a relative redirect to the login page
                .andExpect(redirectedUrl("/login"));

        mvc.perform(
                        MockMvcRequestBuilders.post("/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .content("username=qanary&password=mySecret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();
    }
}
