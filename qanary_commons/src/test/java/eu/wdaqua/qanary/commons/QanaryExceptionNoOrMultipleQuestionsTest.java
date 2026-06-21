package eu.wdaqua.qanary.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class QanaryExceptionNoOrMultipleQuestionsTest {

    @Test
    void keepsProvidedMessage() {
        QanaryExceptionNoOrMultipleQuestions ex =
                new QanaryExceptionNoOrMultipleQuestions("no question found");
        assertEquals("no question found", ex.getMessage());
    }
}
