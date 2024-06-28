package qa.commons;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.*;

class PriorConversationTest {

    @Test
    void testCreationOfQuestionInTriplestoreWithPriorConversation()
            throws SparqlQueryFailed, URISyntaxException, MalformedURLException {
        QanaryTripleStoreProxy mockedQanaryTripleStoreConnectorQanary = mock(
                QanaryTripleStoreProxy.class);
        doNothing().when(mockedQanaryTripleStoreConnectorQanary).update(anyString());

        QanaryConfigurator qanaryConfigurator = new QanaryConfigurator( //
                null, //
                null, //
                null, //
                0, //
                null, //
                null, //
                mockedQanaryTripleStoreConnectorQanary //
        );

        // when QanaryQuestion is instantiated with a valid priorConversation
        URI priorConversation = new URI("urn:priorConversation");
        URL questionUri = new URI("http://localhost/question").toURL();
        new QanaryQuestion<String>(questionUri, qanaryConfigurator, priorConversation);

        // ensure that a SPARQL UPDATE query is called and a graph URI is provided as
        // prior conversation
        String queryPart = ".*" //
                + "<" + questionUri.toString() + ">" //
                + " qa:priorConversation " //
                + "<" + priorConversation.toASCIIString() + ">" //
                + " \\..*";
        verify(mockedQanaryTripleStoreConnectorQanary, atLeast(1)).update(matches(queryPart));
    }

}
