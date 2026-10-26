package eu.wdaqua.qanary.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import eu.wdaqua.qanary.message.QanaryAvailableQuestions;
import eu.wdaqua.qanary.message.QanaryQuestionCreated;
import eu.wdaqua.qanary.message.QanaryQuestionInformation;

/**
 * Questions are stored as files in the configured questions directory and
 * addressed by URLs built from the current request.
 */
class QanaryQuestionControllerCoverageTest {

    @TempDir
    Path temp;

    private static QanaryQuestionController controller(Path questionsDirectory) {
        Map<String, Object> properties = QanaryPipelineConfigurationCoverageTest.required();
        properties.put("qanary.questions.directory", questionsDirectory.toString());
        return new QanaryQuestionController(null,
                new QanaryPipelineConfiguration(QanaryPipelineConfigurationCoverageTest.environment(properties)));
    }

    @BeforeEach
    void inRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/question");
        request.setServerName("qanary.local");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void resetRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void textQuestionIsStoredInANewlyCreatedDirectory() throws Exception {
        Path directory = temp.resolve("questions").resolve("text");
        QanaryQuestionController controller = controller(directory);

        ResponseEntity<?> response = controller.createQuestion("Wo liegt Leipzig?");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        QanaryQuestionCreated created = (QanaryQuestionCreated) response.getBody();
        assertEquals("Wo liegt Leipzig?",
                Files.readString(directory.resolve(created.getQuestionID()), StandardCharsets.UTF_8));
        assertEquals("http://qanary.local:8080/question/" + created.getQuestionID(),
                created.getQuestionURI().toString());
    }

    @Test
    void textQuestionThatCannotBeStoredIsAServerError() throws Exception {
        Path notADirectory = Files.writeString(temp.resolve("a-file"), "occupied");

        ResponseEntity<?> response = controller(notADirectory).createQuestion("Who wrote Faust?");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void audioQuestionIsStoredInANewlyCreatedDirectory() throws Exception {
        Path directory = temp.resolve("audio");
        byte[] audio = {1, 2, 3, 4, 5};

        ResponseEntity<?> response = controller(directory)
                .createAudioQuestion(new MockMultipartFile("question", "q.mp3", "audio/mpeg", audio));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        QanaryQuestionCreated created = (QanaryQuestionCreated) response.getBody();
        assertTrue(created.getQuestionID().contains("_audio_"), created.getQuestionID());
        assertEquals(5, Files.readAllBytes(directory.resolve(created.getQuestionID())).length);
    }

    @Test
    void emptyAudioQuestionIsRejected() {
        ResponseEntity<?> response = controller(temp)
                .createAudioQuestion(new MockMultipartFile("question", "silence.mp3", "audio/mpeg", new byte[0]));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("silence.mp3"), response.getBody().toString());
    }

    @Test
    void storedQuestionsAreListedButOtherFilesAreNot() throws Exception {
        QanaryQuestionController controller = controller(temp);
        String first = ((QanaryQuestionCreated) controller.createQuestion("one?").getBody()).getQuestionID();
        String second = ((QanaryQuestionCreated) controller.createQuestion("two?").getBody()).getQuestionID();
        Files.writeString(temp.resolve("notes.txt"), "not a question");

        QanaryAvailableQuestions questions = controller.getQuestions();

        List<String> urls = questions.getAvailableQuestions().stream().map(URL::toString).sorted()
                .collect(Collectors.toList());
        assertEquals(List.of("http://qanary.local:8080/question/" + first + "/",
                "http://qanary.local:8080/question/" + second + "/").stream().sorted().collect(Collectors.toList()),
                urls);
    }

    @Test
    void questionInformationLinksToTheRawData() throws Exception {
        QanaryQuestionInformation information = controller(temp).getQuestion("stored-question__text_1");

        assertEquals("stored-question__text_1", information.questionID);
        assertEquals("http://qanary.local:8080/question/stored-question__text_1/raw", information.rawdata.toString());
    }

    @Test
    void notImplementedOperationsSaySo() throws Exception {
        QanaryQuestionController controller = controller(temp);

        assertThrows(HttpRequestMethodNotSupportedException.class,
                () -> controller.deleteQuestion("stored-question__text_1"));
        assertNull(controller.getStatusOfQuestion(new URL("http://qanary.local:8080/question/1")));
    }

    @Test
    void thereIsNoCurrentRequestOutsideOfRequestHandling() {
        RequestContextHolder.resetRequestAttributes();

        assertNull(QanaryQuestionController.getCurrentHttpServletRequest());
    }
}
