package qa.commons;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

class PriorConversationTest {

	@Test
	void testCreationOfQuestionInTriplestoreWithPriorConversation()
			throws SparqlQueryFailed, URISyntaxException, MalformedURLException {
		QanaryTripleStoreConnectorQanaryInternal mockedQanaryTripleStoreConnectorQanary = mock(
				QanaryTripleStoreConnectorQanaryInternal.class);
		doNothing().when(mockedQanaryTripleStoreConnectorQanary).update(anyString());

		QanaryConfigurator qanaryConfigurator = new QanaryConfigurator( //
				null, //
				null, //
				null, //
				0, //
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
