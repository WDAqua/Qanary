package qa.commons;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.business.TriplestoreEndpointIdentifier;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoException;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import org.mockito.MockedStatic;

class QanaryQuestionTest {

    @Test
    void testStatic() {
        try (MockedStatic<QanaryUtils> mockedStatic = Mockito.mockStatic(QanaryUtils.class)) {
            // mocked behaviour
            doNothing().when(QanaryUtils.class);

            // setup new Question
            URL questionUri = new URI("http://localhost/question").toURL();
            QanaryConfigurator qanaryConfigurator = new QanaryConfigurator(new RestTemplate(),
                    new ArrayList<>(),
                    "http://localhost",
                    8080,
                       new URI("http://localhost:8081"), new TriplestoreEndpointIdentifier());
            // prior conversation dummy for this test
            URI priorConversation = new URI("urn:priorConversation");

            // sut
            QanaryQuestion qanaryQuestion = new QanaryQuestion(questionUri, qanaryConfigurator, priorConversation);

            // verify that prior conversation dummy is annotated
            mockedStatic.verify(() -> QanaryUtils.loadTripleStore(matches(
                    ".*<http://localhost/question> qa:priorConversation <urn:priorConversation> \\..*"),
                    any(QanaryConfigurator.class)), times(1));

        } catch (URISyntaxException | SparqlQueryFailed | MalformedURLException | MockitoException e) {
            e.printStackTrace();
            fail();
        }
    }
}
