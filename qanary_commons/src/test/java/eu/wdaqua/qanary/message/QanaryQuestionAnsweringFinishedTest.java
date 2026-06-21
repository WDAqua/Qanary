package eu.wdaqua.qanary.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import eu.wdaqua.qanary.business.QanaryComponent;
import eu.wdaqua.qanary.commons.QanaryMessage;

class QanaryQuestionAnsweringFinishedTest {

    private QanaryMessage message() throws URISyntaxException {
        return new QanaryMessage(URI.create("http://localhost/sparql"), URI.create("urn:graph"));
    }

    @Test
    void keepsTheOriginatingMessage() throws URISyntaxException {
        QanaryMessage message = message();
        QanaryQuestionAnsweringFinished finished = new QanaryQuestionAnsweringFinished(message);
        assertSame(message, finished.getQanaryMessage());
    }

    @Test
    void protocolCapturesComponentExecutions() throws URISyntaxException, InterruptedException {
        QanaryQuestionAnsweringFinished finished = new QanaryQuestionAnsweringFinished(message());
        finished.startQuestionAnswering();

        QanaryComponent component = new QanaryComponent("NER", "http://localhost:8080", true);
        finished.appendProtocol(component, HttpStatus.OK, 55L);
        finished.endQuestionAnswering();

        // compact protocol
        assertEquals(1, finished.getCompactProtocol().size());
        assertTrue(finished.getCompactProtocol().get(0).contains("NER"));
        assertTrue(finished.getCompactProtocol().get(0).contains("200"));

        // extended protocol (covers the inner ComponentExecutionLog accessors)
        var extended = finished.getExtendedProtocol();
        assertEquals(1, extended.size());
        assertEquals("NER", extended.get(0).getComponentName());
        assertEquals("http://localhost:8080", extended.get(0).getComponentUri());
        assertEquals(55L, extended.get(0).getDurationInMilliseconds());
        assertEquals(200, extended.get(0).getHttpResponseCode());

        // duration / readable dates are derived from start & end timestamps
        assertTrue(finished.getDurationInMilliseconds() >= 0);
        assertEquals(19, finished.getStartOfQuestionAnswering().length()); // yyyy-MM-dd HH:mm:ss
        assertEquals(19, finished.getEndOfQuestionAnswering().length());
        assertTrue(finished.toString().contains("question answering"));
    }
}
