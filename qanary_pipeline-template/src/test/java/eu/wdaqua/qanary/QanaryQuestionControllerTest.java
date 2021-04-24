package eu.wdaqua.qanary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.multipart.MultipartFile;

import eu.wdaqua.qanary.message.QanaryQuestionCreated;
import eu.wdaqua.qanary.web.QanaryPipelineConfiguration;
import eu.wdaqua.qanary.web.QanaryQuestionController;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
class QanaryQuestionControllerTest {
	@Autowired
	public Environment environment;

	/**
	 * test if a text question (string) is written correctly
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCreateTextQuestion() throws IOException {

		QanaryQuestionController qanaryQuestionController = new QanaryQuestionController(null,
				new QanaryPipelineConfiguration(this.environment));

		testCreateTextQuestion(qanaryQuestionController, "foo bar?");
		testCreateTextQuestion(qanaryQuestionController, "Wie viele Länder gibt es?");
		testCreateTextQuestion(qanaryQuestionController, "La reunión se celebrará a las 16:00 horas en la biblioteca.");
	}

	private void testCreateTextQuestion(QanaryQuestionController qanaryQuestionController, String testquestion) throws IOException {
		@SuppressWarnings("unchecked")
		ResponseEntity<QanaryQuestionCreated> response = (ResponseEntity<QanaryQuestionCreated>) qanaryQuestionController
				.createQuestion(testquestion);

		FileSystemResource returnedquestion = qanaryQuestionController
				.getQuestionRawData(response.getBody().getQuestionID());
		String returnedquestionContent = Files.readString(Path.of(returnedquestion.getURI()));
		assertEquals(testquestion.trim().length(), returnedquestionContent.trim().length(), //
				"expected: '" + testquestion + "' != '" + returnedquestionContent + "'");
		assertEquals(testquestion, returnedquestionContent, //
				"expected: '" + testquestion + "' != '" + returnedquestionContent + "'");
	}

	@Test
	public void testCreateAudioQuestion() throws IOException {

		QanaryQuestionController qanaryQuestionController = new QanaryQuestionController(null,
				new QanaryPipelineConfiguration(this.environment));
		// FileSystemResource testquestion = new
		// FileSystemResource("src/test/resources/bill_gates-TED.mp3");

		File file = new File("src/test/resources/bill_gates-TED.mp3");
		FileInputStream input = new FileInputStream(file);
		MultipartFile testquestion = new MockMultipartFile("file", file.getName(), "text/plain",
				IOUtils.toByteArray(input));

		// MultipartFile testquestion = new
		// MockMultipartFile("src/test/resources/bill_gates-TED.mp3");

		@SuppressWarnings("unchecked")
		ResponseEntity<QanaryQuestionCreated> response = (ResponseEntity<QanaryQuestionCreated>) qanaryQuestionController
				.createAudioQuestion(testquestion);

		FileSystemResource returnedquestion = qanaryQuestionController
				.getQuestionRawData(response.getBody().getQuestionID());
		assertTrue(returnedquestion.contentLength() == file.length());
	}
}
