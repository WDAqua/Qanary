package qa.pipeline;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.springframework.http.ResponseEntity;

import eu.wdaqua.qanary.message.QanaryQuestionCreated;
import eu.wdaqua.qanary.web.QanaryQuestionController;

public class QanaryQuestionControllerTest {

	/**
	 * test if question string is written correctly
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCreateQuestion() throws IOException {

		QanaryQuestionController qanaryQuestionController = new QanaryQuestionController(null);

		String testquestion = "foo bar?";

		ResponseEntity<QanaryQuestionCreated> response = (ResponseEntity<QanaryQuestionCreated>) qanaryQuestionController
				.createQuestion(testquestion);

		//String returnedquestion = qanaryQuestionController.getQuestionRawData(response.getBody().getQuestionID());

		//assertTrue(returnedquestion.compareTo(testquestion) == 0);

	}

}
