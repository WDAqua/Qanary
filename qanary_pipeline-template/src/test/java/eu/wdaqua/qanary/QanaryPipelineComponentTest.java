package eu.wdaqua.qanary;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorPipelineComponent;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWireMock(port = 8080) //
public class QanaryPipelineComponentTest {

    private final String questionID = "http://localhost:8080";
    private final String EXAMPLE_QUESTION = "Example question";

    @MockBean
    private QanaryTripleStoreConnector qanaryTripleStoreConnector;

    @Autowired
    private QanaryPipelineComponent qanaryPipelineComponent;

    @Before
    public void setup() {
        WebClient webClient = WebClient.builder().build();
        ReflectionTestUtils.setField(qanaryPipelineComponent, "webClient", webClient);
    }

    private String convertResponseToString(HttpResponse response) throws IOException {
        InputStream responseStream = response.getEntity().getContent();
        Scanner scanner = new Scanner(responseStream, "UTF-8");
        String responseString = scanner.useDelimiter("\\Z").next();
        scanner.close();
        return responseString;
    }

    @Test
    public void getQuestionWithQuestionIdTest() {
        WireMock.stubFor(WireMock.get(WireMock.urlPathMatching(".*\\/raw$"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody(EXAMPLE_QUESTION)));
        String result = qanaryPipelineComponent.getQuestionWithQuestionId(questionID);
        Assert.assertEquals(EXAMPLE_QUESTION, result);
    }

    public void transferGraphDataTest() {

    }



}
