#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.inGraphKey;
import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.outGraphKey;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import eu.wdaqua.qanary.component.QanaryServiceController;

@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class QanaryServiceControllerTest {

	private static final Logger logger = LoggerFactory.getLogger(QanaryServiceControllerTest.class);

    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext applicationContext;

    /**
     * initialize local controller enabled for tests
     */
    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.applicationContext).build();
    }

    /**
     * test description interface
     *
     * @throws Exception
     */
    @Test
    void testDescriptionAvailable() throws Exception {
        mockMvc.perform(get(QanaryConfiguration.description)) // fetch
                .andExpect(status().isOk()) // HTTP 200
                .andReturn(); //
    }

    /**
     * test correct message format
     */
    @Test
    void testMessageFromJson() {
        // create message from JSON string
        QanaryMessage message;
        try {
            message = new QanaryMessage(new URI(endpointKey), new URI(inGraphKey), new URI(outGraphKey));

            URI endpointKeyUrlFromMessage = message.getEndpoint();
            assertNotNull(endpointKeyUrlFromMessage);

            URI endpointKeyUrlFromHere = new URI(endpointKey);

            // TODO: more tests to ensure mechanism
            assertEquals(endpointKeyUrlFromMessage.toString(), endpointKeyUrlFromHere.toString());

        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}