package qa.pipeline;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
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
	
}
