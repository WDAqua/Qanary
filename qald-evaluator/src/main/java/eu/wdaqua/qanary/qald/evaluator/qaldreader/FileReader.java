package eu.wdaqua.qanary.qald.evaluator.qaldreader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class FileReader {

	private HashMap<Integer, QaldQuestion> questions = new HashMap<>();

	public FileReader() throws UnsupportedEncodingException, IOException {

		Reader reader = new InputStreamReader(FileReader.class.getResourceAsStream("/qald-6-test-multilingual.json"),
				"UTF-8");
		Gson gson = new GsonBuilder().create();
		JsonObject json = gson.fromJson(reader, JsonObject.class);

		JsonArray questions = json.get("questions").getAsJsonArray();
		// System.out.println(questions);
		System.out.println("size: " + questions.size());

		for (int i = 0; i < questions.size(); i++) {
			this.addQuestion(new QaldQuestion(questions.get(i).getAsJsonObject()));
		}
	}

	/**
	 * register a question
	 * 
	 * @param qaldQuestion
	 */
	private void addQuestion(QaldQuestion qaldQuestion) {
		this.questions.put(qaldQuestion.getQaldId(), qaldQuestion);
	}

	/**
	 * retrieve a QaldQuestion via the QALD question id
	 * 
	 * @param qaldId
	 * @return
	 */
	public QaldQuestion getQuestion(int qaldId) {
		return this.questions.get(qaldId);
	}
}
