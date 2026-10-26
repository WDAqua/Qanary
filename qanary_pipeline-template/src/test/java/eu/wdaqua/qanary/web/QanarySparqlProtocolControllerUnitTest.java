package eu.wdaqua.qanary.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;

import org.apache.commons.lang3.NotImplementedException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

/**
 * Tests the SPARQL protocol proxy of the pipeline against a real in-memory
 * triplestore: SELECT and ASK results, UPDATE requests in all supported request
 * forms, the connection check and the error cases.
 */
class QanarySparqlProtocolControllerUnitTest {

    private static final String SPARQL_JSON = "application/sparql-results+json";
    private static final String GRAPH = "urn:graph:sparql-protocol-test";

    private InMemoryQanaryTripleStore triplestore;
    private QanarySparqlProtocolController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        triplestore = new InMemoryQanaryTripleStore();
        controller = new QanarySparqlProtocolController(triplestore.proxy());
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private void insertExampleTriples() throws SparqlQueryFailed {
        triplestore.update("INSERT DATA { GRAPH <" + GRAPH + "> { "
                + "<urn:s1> <urn:p> \"one\" . <urn:s2> <urn:p> \"two\" . } }");
    }

    // ---- connection check ------------------------------------------------------

    @Test
    void connectionCheckReportsAnEmptyTriplestore() throws Exception {
        mvc.perform(get("/" + QanarySparqlProtocolController.SPARQL_ENDPOINT_TESTER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NO triples are returned")));
    }

    @Test
    void connectionCheckReportsATriplestoreWithData() throws Exception {
        insertExampleTriples();
        mvc.perform(get("/" + QanarySparqlProtocolController.SPARQL_ENDPOINT_TESTER))
                .andExpect(status().isOk())
                .andExpect(content().string("Triplestore is accessible and returns triples."));
    }

    @Test
    void connectionCheckReportsAFailingTriplestoreAsServerError() throws Exception {
        triplestore.failSelects();
        ResponseEntity<String> response = controller.checkTriplestoreConnection();
        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody().startsWith("SPARQL query for connection check failed"), response.getBody());
    }

    // ---- GET queries -------------------------------------------------------------

    @Test
    void selectQueryReturnsSparqlJsonResults() throws Exception {
        insertExampleTriples();
        MvcResult result = mvc.perform(get("/" + QanarySparqlProtocolController.SPARQL_ENDPOINT)
                        .accept(SPARQL_JSON)
                        .param("query", "SELECT ?s ?o WHERE { GRAPH <" + GRAPH + "> { ?s <urn:p> ?o } } ORDER BY ?s"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString(SPARQL_JSON)))
                .andReturn();

        JSONObject json = new JSONObject(result.getResponse().getContentAsString());
        JSONArray bindings = json.getJSONObject("results").getJSONArray("bindings");
        assertEquals(2, bindings.length());
        assertEquals("urn:s1", bindings.getJSONObject(0).getJSONObject("s").getString("value"));
        assertEquals("one", bindings.getJSONObject(0).getJSONObject("o").getString("value"));
        assertEquals("two", bindings.getJSONObject(1).getJSONObject("o").getString("value"));
    }

    @Test
    void askQueryReturnsBooleanResult() throws Exception {
        insertExampleTriples();
        MvcResult yes = mvc.perform(get("/" + QanarySparqlProtocolController.SPARQL_ENDPOINT)
                        .accept(SPARQL_JSON)
                        .param("query", "ASK { GRAPH <" + GRAPH + "> { <urn:s1> <urn:p> \"one\" } }"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(new JSONObject(yes.getResponse().getContentAsString()).getBoolean("boolean"));

        ResponseEntity<String> no = controller.getSparqlAsJSON(SPARQL_JSON,
                "ASK { GRAPH <" + GRAPH + "> { <urn:s1> <urn:p> \"nope\" } }");
        JSONObject noJson = new JSONObject(no.getBody());
        assertEquals(false, noJson.getBoolean("boolean"));
        assertTrue(noJson.has("head"));
    }

    @Test
    void invalidQueryIsRejected() {
        SparqlQueryFailed e = assertThrows(SparqlQueryFailed.class,
                () -> controller.getSparqlAsJSON(SPARQL_JSON, "SELECT WHERE {"));
        assertTrue(e.getMessage().contains("SELECT WHERE {"), e.getMessage());
    }

    @Test
    void constructQueryIsRejectedAsNeitherAskNorSelect() {
        SparqlQueryFailed e = assertThrows(SparqlQueryFailed.class,
                () -> controller.getSparqlAsJSON(SPARQL_JSON, "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }"));
        assertTrue(e.getMessage().contains("CONSTRUCT"), e.getMessage());
    }

    @Test
    void xmlResultsAreNotImplemented() {
        assertThrows(NotImplementedException.class,
                () -> controller.getSparqlAsXML("application/sparql-results+xml", "SELECT * {}"));
    }

    @Test
    void htmlRequestReturnsTheQueryPage() {
        assertEquals("sparql", controller.getSparqlAsHTML());
    }

    @Test
    void unknownSparqlPathsAnswerNotFound() throws Exception {
        mvc.perform(get("/sparqlx").header("X-Test", "1").requestAttr("someAttribute", "value"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{}"));
    }

    // ---- POST requests -------------------------------------------------------------

    @Test
    void formEncodedUpdateIsExecuted() throws Exception {
        mvc.perform(post("/" + QanarySparqlProtocolController.SPARQL_ENDPOINT)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(SPARQL_JSON)
                        .param("update", "INSERT DATA { GRAPH <" + GRAPH + "> { <urn:a> <urn:b> <urn:c> } }"))
                .andExpect(status().isNoContent());

        assertTrue(triplestore.contains(GRAPH, "<urn:a> <urn:b> <urn:c>"));
    }

    @Test
    void rawSparqlUpdateBodyIsExecuted() throws Exception {
        mvc.perform(post("/" + QanarySparqlProtocolController.SPARQL_ENDPOINT)
                        .contentType("application/sparql-update")
                        .accept(SPARQL_JSON)
                        .content("INSERT DATA { GRAPH <" + GRAPH + "> { <urn:x> <urn:y> \"z\" } }"))
                .andExpect(status().isNoContent());

        assertTrue(triplestore.contains(GRAPH, "<urn:x> <urn:y> \"z\""));
    }

    @Test
    void formEncodedSelectIsAnsweredAsJson() throws Exception {
        insertExampleTriples();
        MvcResult result = mvc.perform(post("/" + QanarySparqlProtocolController.SPARQL_ENDPOINT)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(SPARQL_JSON)
                        .param("query", "SELECT (COUNT(*) AS ?n) WHERE { GRAPH <" + GRAPH + "> { ?s ?p ?o } }"))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject json = new JSONObject(result.getResponse().getContentAsString());
        assertEquals("2", json.getJSONObject("results").getJSONArray("bindings")
                .getJSONObject(0).getJSONObject("n").getString("value"));
    }

    @Test
    void failingUpdateIsPassedOn() {
        triplestore.failUpdatesMatching(query -> query.contains("INSERT"));
        ServletException e = assertThrows(ServletException.class,
                () -> mvc.perform(post("/" + QanarySparqlProtocolController.SPARQL_ENDPOINT)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("update", "INSERT DATA { <urn:a> <urn:b> <urn:c> }")));
        assertInstanceOf(SparqlQueryFailed.class, e.getCause());
    }

    @Test
    void postWithoutAnyQueryFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sparql");
        request.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        SparqlQueryFailed e = assertThrows(SparqlQueryFailed.class,
                () -> controller.postSparql(request, Map.of(), SPARQL_JSON, new LinkedMultiValueMap<>()));
        assertTrue(e.getMessage().contains("no query extractable"), e.getMessage());
    }

    @Test
    void unreadableRequestBodyIsReported() {
        MockHttpServletRequest brokenRequest = new MockHttpServletRequest("POST", "/sparql") {
            @Override
            public ServletInputStream getInputStream() throws IllegalStateException {
                return new ServletInputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("connection reset");
                    }

                    @Override
                    public boolean isFinished() {
                        return false;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(jakarta.servlet.ReadListener readListener) {
                    }
                };
            }
        };

        SparqlQueryFailed e = assertThrows(SparqlQueryFailed.class,
                () -> controller.postSparql(brokenRequest, Map.of(), SPARQL_JSON, new LinkedMultiValueMap<>()));
        assertTrue(e.getMessage().contains("no query could be extracted"), e.getMessage());
    }
}
