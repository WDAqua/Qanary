package qald.evaluator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import eu.wdaqua.qanary.qald.evaluator.qaldreader.FileReader;
import eu.wdaqua.qanary.qald.evaluator.qaldreader.QaldQuestion;

class FileReaderTest {

    /**
     * the bundled QALD-6 training benchmark is parsed into QaldQuestion objects
     */
    @Test
    void readsBundledQaldBenchmark() throws UnsupportedEncodingException, IOException {
        FileReader fileReader = new FileReader();

        Collection<QaldQuestion> questions = fileReader.getQuestions();
        assertNotNull(questions);
        assertFalse(questions.isEmpty(), "the QALD-6 benchmark should contain questions");

        // every parsed question is retrievable by its QALD id and carries a question string
        for (QaldQuestion q : questions) {
            assertNotNull(fileReader.getQuestion(q.getQaldId()));
            assertNotNull(q.getQuestion());
        }

        // at least one question annotates DBpedia resource URIs
        boolean anyResourceUris = questions.stream()
                .anyMatch(q -> !q.getResourceUrisAsString().isEmpty());
        assertTrue(anyResourceUris, "expected at least one question with DBpedia resource URIs");
    }
}
