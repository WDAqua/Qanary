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

    @Test
    void theProtocolsOfProcessPartsAreAppendedInOrder() throws URISyntaxException {
        QanaryQuestionAnsweringFinished process = new QanaryQuestionAnsweringFinished(message());
        process.startQuestionAnswering();
        process.appendProtocol(new QanaryComponent("a", "http://localhost:1", true), HttpStatus.OK, 1L);
        QanaryQuestionAnsweringFinished stream = new QanaryQuestionAnsweringFinished(message());
        stream.startQuestionAnswering();
        stream.appendProtocol(new QanaryComponent("b", "http://localhost:2", true), HttpStatus.OK, 2L);
        stream.appendProtocol(new QanaryComponent("c", "http://localhost:3", true), HttpStatus.OK, 3L);

        process.appendProtocol(stream);
        // a part that was never started has no protocol
        process.appendProtocol(new QanaryQuestionAnsweringFinished(message()));

        assertEquals(3, process.getCompactProtocol().size());
        assertTrue(process.getCompactProtocol().get(1).startsWith("b "));
        assertEquals(java.util.List.of("a", "b", "c"), process.getExtendedProtocol().stream()
                .map(QanaryQuestionAnsweringFinished.ComponentExecutionLog::getComponentName).toList());
        assertEquals(2, stream.getCompactProtocol().size()); // the part is not changed
    }
}
