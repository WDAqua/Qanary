package qald.evaluator;

import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.JsonObject;

import eu.wdaqua.qanary.qald.evaluator.qaldreader.QaldQuestion;

public class QaldQuestionTest {

	@Ignore
	@Test
	public void test() {

		JsonObject rawQuestion = new JsonObject();
		rawQuestion.addProperty("id", 42);
		rawQuestion.addProperty("question", "How many goals did Pelé score?");

		QaldQuestion qaldQuestion = new QaldQuestion(rawQuestion);

		qaldQuestion.getUris();

		// TODO: encapsulate with Mockito

		// TODO: check recognized URIs

		fail("Not yet implemented");
	}

}
