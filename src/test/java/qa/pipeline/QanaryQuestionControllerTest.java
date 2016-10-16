package qa.pipeline;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import eu.wdaqua.qanary.message.QanaryQuestionCreated;
import eu.wdaqua.qanary.web.QanaryQuestionController;

public class QanaryQuestionControllerTest {

	/**
	 * test if a text question (string) is written correctly
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCreateTextQuestion() throws IOException {

		QanaryQuestionController qanaryQuestionController = new QanaryQuestionController(null);

		String testquestion = "foo bar?";

		@SuppressWarnings("unchecked")
		ResponseEntity<QanaryQuestionCreated> response = (ResponseEntity<QanaryQuestionCreated>) qanaryQuestionController
				.createQuestion(testquestion);

		FileSystemResource returnedquestion = qanaryQuestionController.getQuestionRawData(response.getBody().getQuestionID());
		assertTrue(returnedquestion.contentLength() == testquestion.length());
	}

	@Test
	public void testCreateAudioQuestion() throws IOException {

		QanaryQuestionController qanaryQuestionController = new QanaryQuestionController(null);

        //FileSystemResource testquestion = new FileSystemResource("src/test/resources/bill_gates-TED.mp3");

        File file = new File("src/test/resources/bill_gates-TED.mp3");
        FileInputStream input = new FileInputStream(file);
        MultipartFile testquestion = new MockMultipartFile("file",
                file.getName(), "text/plain", IOUtils.toByteArray(input));


        //MultipartFile testquestion = new MockMultipartFile("src/test/resources/bill_gates-TED.mp3");

		@SuppressWarnings("unchecked")
		ResponseEntity<QanaryQuestionCreated> response = (ResponseEntity<QanaryQuestionCreated>) qanaryQuestionController
				.createAudioQuestion(testquestion);

		FileSystemResource returnedquestion = qanaryQuestionController.getQuestionRawData(response.getBody().getQuestionID());
		assertTrue(returnedquestion.contentLength() == file.length());
	}
	
}
