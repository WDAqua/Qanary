package eu.wdaqua.qanary;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

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
        //doNothing().when(qanaryTripleStoreConnector).connect();
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


}