package eu.wdaqua.qanary.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.values.InstanceId;
import de.codecentric.boot.admin.server.domain.values.Registration;
import de.codecentric.boot.admin.server.domain.values.StatusInfo;
import eu.wdaqua.qanary.CoverageHttpStub;
import eu.wdaqua.qanary.QanaryComponentRegistrationChangeNotifier;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorInMemory;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;

/**
 * Web endpoints around registered components, run through the full pipeline
 * application (same context configuration as
 * {@link QanaryPipelineWebEndpointsIntegrationTest}, so the cached context is
 * reused). Components are served by a local HTTP stub, process data comes from
 * an in-memory triple store behind the mocked proxy.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RegisteredComponentsWebCoverageTest {

    static final String TURTLE = "<urn:qanary:NED-Comp> a <http://www.wdaqua.eu/qa#QanaryComponent> .";
    static final String GRAPH = "urn:graph:run-1";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private QanaryTripleStoreProxy mockedQanaryTripleStoreConnector;

    @Autowired
    private QanaryComponentRegistrationChangeNotifier notifier;

    private CoverageHttpStub component;
    private Map<String, Instance> registeredBefore;

    static Instance instance(String name, String serviceUrl, StatusInfo status) {
        Registration registration = Registration.create(name, "http://127.0.0.1:1/actuator/health")
                .serviceUrl(serviceUrl).build();
        return Instance.create(InstanceId.of(name + "-id")).register(registration).withStatusInfo(status);
    }

    @BeforeEach
    void registerComponents() throws Exception {
        component = new CoverageHttpStub().respond("/ned/component-description", 200, "text/turtle", TURTLE);
        registeredBefore = new HashMap<>(notifier.getAvailableComponents());
        notifier.getAvailableComponents().clear();
        notifier.getAvailableComponents().put("NED-Comp", instance("NED-Comp", component.url() + "/ned/", StatusInfo.ofUp()));
        notifier.getAvailableComponents().put("QB-Down", instance("QB-Down", "http://qb-host/", StatusInfo.ofDown()));
    }

    @AfterEach
    void restoreRegistry() {
        notifier.getAvailableComponents().clear();
        notifier.getAvailableComponents().putAll(registeredBefore);
        component.close();
    }

    @Test
    void componentDescriptionIsFetchedFromTheComponentInTheRequestedFormat() throws Exception {
        mvc.perform(get("/components/NED-Comp").accept("text/turtle"))
                .andExpect(status().isOk())
                .andExpect(content().string(TURTLE));

        CoverageHttpStub.RecordedRequest forwarded = component.requests().get(0);
        assertEquals("/ned/component-description", forwarded.path());
        assertEquals("text/turtle", forwarded.accept());
    }

    @Test
    void unknownComponentHasNoDescription() throws Exception {
        mvc.perform(get("/components/NED-Unknown").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void annotationsOfOneComponentInAGraphAreReturnedAsSparqlJson() throws Exception {
        QanaryTripleStoreConnectorInMemory processData = new QanaryTripleStoreConnectorInMemory();
        processData.update("PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
                + "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
                + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
                + "INSERT DATA { GRAPH <" + GRAPH + "> { " //
                + "  <urn:annotation:ned> a qa:AnnotationOfInstance ; oa:annotatedBy <urn:qanary:NED-Comp> ; " //
                + "    oa:annotatedAt \"2026-10-26T10:00:00Z\"^^xsd:dateTime ; oa:hasTarget <urn:question:1> ; " //
                + "    oa:hasBody <http://dbpedia.org/resource/Leipzig> ; qa:score 0.9 . " //
                + "  <urn:annotation:qb> a qa:AnnotationOfAnswerSPARQL ; oa:annotatedBy <urn:qanary:QB-Comp> ; " //
                + "    oa:annotatedAt \"2026-10-26T10:00:01Z\"^^xsd:dateTime ; oa:hasTarget <urn:question:1> ; " //
                + "    oa:hasBody <urn:query:1> . " //
                + "} }");
        when(mockedQanaryTripleStoreConnector.select(anyString()))
                .thenAnswer(call -> processData.select(call.getArgument(0)));

        mvc.perform(get("/components/" + GRAPH + "/NED-Comp").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.bindings.length()").value(1))
                .andExpect(jsonPath("$.results.bindings[0].annotatedBy.value").value("urn:qanary:NED-Comp"))
                .andExpect(jsonPath("$.results.bindings[0].hasBody.value")
                        .value("http://dbpedia.org/resource/Leipzig"));
    }

    @Test
    void availabilityListsRegisteredComponentsWithTheirStatus() throws Exception {
        // a component whose registration carries a service URL that is no URI
        Registration unparseable = mock(Registration.class);
        when(unparseable.getServiceUrl()).thenReturn("http://bad host/");
        Instance withUnparseableUrl = mock(Instance.class);
        when(withUnparseableUrl.getRegistration()).thenReturn(unparseable);
        when(withUnparseableUrl.getStatusInfo()).thenReturn(StatusInfo.ofUp());
        notifier.getAvailableComponents().put("REL-BadUrl", withUnparseableUrl);
        // an instance without registration data
        Instance unregistered = mock(Instance.class);
        when(unregistered.getRegistration()).thenReturn(null);
        when(unregistered.getStatusInfo()).thenReturn(StatusInfo.ofUnknown());
        notifier.getAvailableComponents().put("REL-Unregistered", unregistered);

        mvc.perform(get("/components/availability").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[?(@.name=='NED-Comp')].status", contains("UP")))
                .andExpect(jsonPath("$[?(@.name=='NED-Comp')].accessible", contains(true)))
                .andExpect(jsonPath("$[?(@.name=='NED-Comp')].host", contains("127.0.0.1")))
                .andExpect(jsonPath("$[?(@.name=='NED-Comp')].port", contains(component.port())))
                .andExpect(jsonPath("$[?(@.name=='NED-Comp')].url", contains("/components/NED-Comp")))
                .andExpect(jsonPath("$[?(@.name=='QB-Down')].accessible", contains(false)))
                .andExpect(jsonPath("$[?(@.name=='QB-Down')].status", contains("DOWN")))
                .andExpect(jsonPath("$[?(@.name=='QB-Down')].host", contains("qb-host")))
                .andExpect(jsonPath("$[?(@.name=='QB-Down')].port", contains((Object) null)))
                .andExpect(jsonPath("$[?(@.name=='REL-BadUrl')].serviceUrl", contains("http://bad host/")))
                .andExpect(jsonPath("$[?(@.name=='REL-BadUrl')].host").isEmpty())
                .andExpect(jsonPath("$[?(@.name=='REL-Unregistered')].status", contains("UNKNOWN")))
                .andExpect(jsonPath("$[?(@.name=='REL-Unregistered')].serviceUrl").isEmpty());

        // only accessible components can be used in a run
        mvc.perform(get("/components").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("NED-Comp", "REL-BadUrl")));
    }

    @Test
    void feedbackIsAcceptedOnlyWithAllRequiredFields() throws Exception {
        mvc.perform(post("/feedback").with(csrf()) //
                        .param("question", "What is the capital of Saxony?") //
                        .param("sparql", "SELECT ?c WHERE { dbr:Saxony dbo:capital ?c }") //
                        .param("correct", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));

        mvc.perform(post("/feedback").with(csrf()).param("question", "What?").param("correct", "false"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void questionsCanBeLookedUpButNotDeleted() throws Exception {
        mvc.perform(get("/question/stored-question__text_1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(jsonPath("$.questionID").value("stored-question__text_1"))
                .andExpect(jsonPath("$.rawdata").value("http://localhost/question/stored-question__text_1/raw"));

        mvc.perform(delete("/question/stored-question__text_1").with(csrf()))
                .andExpect(status().isMethodNotAllowed());
    }
}
