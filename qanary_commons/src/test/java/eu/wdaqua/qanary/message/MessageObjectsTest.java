package eu.wdaqua.qanary.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessageObjectsTest {

    @Test
    void questionCreatedExposesUriAndId() {
        URI uri = URI.create("http://localhost/question/42");
        QanaryQuestionCreated created = new QanaryQuestionCreated("42", uri);
        assertEquals(uri, created.getQuestionURI());
        assertEquals("42", created.getQuestionID());
    }

    @Test
    void questionInformationBuildsRawDataUrl() throws Exception {
        QanaryQuestionInformation info = new QanaryQuestionInformation("abc", "http://localhost:8080");
        assertEquals("abc", info.questionID);
        assertEquals("http://localhost:8080/question/abc/raw", info.rawdata.toString());
    }

    @Test
    void componentNotAvailableExceptionKeepsMessage() {
        QanaryComponentNotAvailableException ex = new QanaryComponentNotAvailableException("down");
        assertEquals("down", ex.getMessage());
    }

    @Test
    void questionNotProvidedExceptionHasDefaultMessage() {
        QanaryExceptionQuestionNotProvided ex = new QanaryExceptionQuestionNotProvided();
        assertTrue(ex.getMessage().contains("question"));
    }

    @Test
    void questionAnsweringRunExposesGraphsAndQuestion() throws Exception {
        URI question = URI.create("http://localhost/question/1");
        URI endpoint = URI.create("http://localhost/sparql");
        URI inGraph = URI.create("urn:graph:in");
        URI outGraph = URI.create("urn:graph:out");

        QanaryQuestionAnsweringRun run =
                new QanaryQuestionAnsweringRun(question, endpoint, inGraph, outGraph, null);
        assertEquals(question, run.getQuestion());
        assertEquals(endpoint, run.getEndpoint());
        assertEquals(inGraph, run.getInGraph());
        assertEquals(outGraph, run.getOutGraph());
    }

    @Test
    void availableQuestionsListsOnlyStoredQuestionFiles(@TempDir Path dir) throws IOException {
        new File(dir.toFile(), "stored-question-1").createNewFile();
        new File(dir.toFile(), "stored-question-2").createNewFile();
        new File(dir.toFile(), "other-file.txt").createNewFile();

        QanaryAvailableQuestions available =
                new QanaryAvailableQuestions(dir.toString(), "http://localhost:8080");

        assertEquals(2, available.getAvailableQuestions().size());
        available.getAvailableQuestions()
                .forEach(url -> assertTrue(url.toString().contains("/question/stored-question")));
    }

    @Test
    void availableQuestionsRejectsNonExistingDirectory() {
        assertThrows(IOException.class, () -> new QanaryAvailableQuestions(
                "/this/path/should/not/exist/qanary", "http://localhost"));
    }

    @Test
    void availableQuestionsFallsBackToCurrentDirectoryWhenBlank() throws IOException {
        // a blank directory configuration falls back to "." (current working dir);
        // it must not throw and yields the stored-question files found there (if any)
        QanaryAvailableQuestions available = new QanaryAvailableQuestions("", "http://localhost:8080");
        assertNotNull(available.getAvailableQuestions());
    }

    @Test
    void availableQuestionsThrowsOnMalformedHost(@TempDir Path dir) throws IOException {
        new File(dir.toFile(), "stored-question-1").createNewFile();
        // a host without a protocol produces a MalformedURLException (an IOException)
        assertThrows(IOException.class,
                () -> new QanaryAvailableQuestions(dir.toString(), "no-protocol-host"));
    }
}
