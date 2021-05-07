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

class PriorConversationTest {

    @Test
    void testStatic() {
        try (MockedStatic<QanaryUtils> mockedStatic = Mockito.mockStatic(QanaryUtils.class)) {
            // given a question with a prior conversation
            URI priorConversation = new URI("urn:priorConversation");
            URL questionUri = new URI("http://localhost/question").toURL();
            QanaryConfigurator qanaryConfigurator = new QanaryConfigurator(
                    null,
                    null,
                    null,
                    0,
                    null,
                    null,
                    null);

            // when QanaryQuestion is instantiated
            new QanaryQuestion(questionUri, qanaryConfigurator, priorConversation);

            // then prior conversation should be included in the Question annotation
            mockedStatic.verify(() -> QanaryUtils.loadTripleStore(matches(
                    ".*<http://localhost/question> qa:priorConversation <urn:priorConversation> \\..*"),
                    any(QanaryConfigurator.class)), times(1));

        } catch (URISyntaxException | SparqlQueryFailed | MalformedURLException | MockitoException e) {
            e.printStackTrace();
            fail();
        }
    }
}
