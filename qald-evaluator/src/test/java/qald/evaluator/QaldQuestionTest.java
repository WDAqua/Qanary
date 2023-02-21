package qald.evaluator;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import eu.wdaqua.qanary.qald.evaluator.qaldreader.QaldQuestion;

class QaldQuestionTest {

	@Disabled
	@Test
	void test() {

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
