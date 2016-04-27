package qald.evaluator;

import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.gson.JsonObject;

import eu.wdaqua.qanary.qald.evaluator.qaldreader.QaldQuestion;

public class QaldQuestionTest {

	@Test
	public void test() {

		JsonObject rawQuestion = new JsonObject();
		rawQuestion.addProperty("id", 42);
		rawQuestion.addProperty("question", "Can you cry underwater?");

		QaldQuestion qaldQuestion = new QaldQuestion(rawQuestion);

		qaldQuestion.getUris();

		// TODO: encapsulate with Mockito

		// TODO: check recognized URIs

		fail("Not yet implemented");
	}

}
