/*
package eu.wdaqua.qanary.test;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TestQanaryExplanationController {

    private MockMvc mockMvc;

    @Test
    public void testExplainEndpointWithoutGraph() throws Exception {
        mockMvc.perform(get("/explain")).andExpect(status().is4xxClientError());
    }

    @Test
    public void testExplainEndpointWithGraph() throws Exception {
        mockMvc.perform(get("/explain/examplegraph"))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string("Not yet implemented"));
    }
*/


}
