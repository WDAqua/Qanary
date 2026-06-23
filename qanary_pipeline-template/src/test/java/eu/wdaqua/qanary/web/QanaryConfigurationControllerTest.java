package eu.wdaqua.qanary.web;

import eu.wdaqua.qanary.QanaryComponentRegistrationChangeNotifier;
import eu.wdaqua.qanary.business.QanaryComponent;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * validates the JSON contract of the /components endpoint, i.e., a list of
 * objects providing the name and the relative URL of every registered Qanary
 * component (the format used by external clients and frontends)
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class QanaryConfigurationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private QanaryTripleStoreProxy mockedQanaryTripleStoreConnector;

    @MockitoBean
    private QanaryComponentRegistrationChangeNotifier mockedRegistrationChangeNotifier;

    @Test
    void testComponentsEndpointReturnsRegisteredComponentsAsJsonList() throws Exception {
        List<String> componentNames = Arrays.asList("NED-DBpediaSpotlight", "QB-SimpleRealNameOfSuperHero");
        QanaryComponent firstComponent = Mockito.mock(QanaryComponent.class);
        when(firstComponent.getName()).thenReturn("NED-DBpediaSpotlight");
        QanaryComponent secondComponent = Mockito.mock(QanaryComponent.class);
        when(secondComponent.getName()).thenReturn("QB-SimpleRealNameOfSuperHero");

        when(mockedRegistrationChangeNotifier.getAvailableComponentNames()).thenReturn(componentNames);
        when(mockedRegistrationChangeNotifier.getAvailableComponentsFromNames(componentNames))
                .thenReturn(Arrays.asList(firstComponent, secondComponent));

        mvc.perform(MockMvcRequestBuilders.get("/components").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("NED-DBpediaSpotlight"))
                .andExpect(jsonPath("$[0].url").value("/components/NED-DBpediaSpotlight"))
                .andExpect(jsonPath("$[1].name").value("QB-SimpleRealNameOfSuperHero"))
                .andExpect(jsonPath("$[1].url").value("/components/QB-SimpleRealNameOfSuperHero"));
    }

    @Test
    void testComponentsEndpointReturnsEmptyJsonListIfNoComponentsAreRegistered() throws Exception {
        when(mockedRegistrationChangeNotifier.getAvailableComponentNames()).thenReturn(List.of());
        when(mockedRegistrationChangeNotifier.getAvailableComponentsFromNames(List.of()))
                .thenReturn(List.of());

        mvc.perform(MockMvcRequestBuilders.get("/components").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
