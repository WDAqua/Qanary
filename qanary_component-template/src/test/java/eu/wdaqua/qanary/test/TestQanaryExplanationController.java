package eu.wdaqua.qanary.test;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryComponentConfiguration;
import eu.wdaqua.qanary.component.QanaryExplanationController;
import eu.wdaqua.qanary.component.QanaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QanaryExplanationController.class)
@ContextConfiguration(classes = QanaryService.class)
public class TestQanaryExplanationController {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QanaryComponentConfiguration qanaryComponentConfiguration;
    @MockBean
    private QanaryComponent qanaryComponent;

    @Test
    public void testExplainEndpointWithoutGraph() throws Exception {
        mockMvc.perform(get("/explain")).andExpect(status().is4xxClientError());
    }

    @Test
    public void testExplainEndpointWithGraph() throws Exception {
        mockMvc.perform(get("/explain/examplegraph"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Not yet implemented"));
    }


}
