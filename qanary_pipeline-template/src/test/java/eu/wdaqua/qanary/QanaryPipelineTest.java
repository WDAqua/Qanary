package eu.wdaqua.qanary;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import eu.wdaqua.qanary.explainability.QanaryExplanationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static com.google.inject.matcher.Matchers.any;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class QanaryPipelineTest {

    @MockBean
    private PipelineExplanationHelper pipelineExplanationHelper;
    @MockBean
    private QanaryTripleStoreProxy qanaryTripleStoreProxy;
    @MockBean
    private QanaryComponentRegistrationChangeNotifier qanaryComponentRegistrationChangeNotifier;
    @Autowired
    private QanaryPipeline qanaryPipeline;

    @BeforeEach
    public void setup() throws IOException, URISyntaxException, SparqlQueryFailed {
        List<String> components = new ArrayList<>() {{add("component1");add("component2");}};
        Mockito.when(pipelineExplanationHelper.getUsedComponents("testGraph")).thenReturn(components);
        //Mockito.when(pipelineExplanationHelper.getQanaryComponentRegistrationChangeNotifier().getAvailableComponents().get(any()).getRegistration().getServiceUrl()).thenReturn("serviceUrl");
        Mockito.when(qanaryComponentRegistrationChangeNotifier.getAvailableComponents()).thenReturn(null);
        List<String> explanations = new ArrayList<>() {{add("explanation1"); add("explanation2");}};
        Mockito.when(pipelineExplanationHelper.fetchSubComponentExplanations(Mockito.anyList()).collectList().block()).thenReturn(explanations);
    }

    @Test
    public void explainTest() throws IOException, URISyntaxException, SparqlQueryFailed {
        QanaryExplanationData testData = new QanaryExplanationData();
        testData.setGraph("testGraph");
        testData.setQuestionId("testQuestionId");
        String explanation = this.qanaryPipeline.explain(testData);
    }

}
