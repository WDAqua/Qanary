package eu.wdaqua.qanary.web.messages;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Message class for communicating with the Gerbil web service
 * 
 * @author AnBo
 *
 */
public class GerbilExecuteResponse {

	class QuestionData {

		private final String string; // question
		private final String language; //

		public QuestionData(String questionText, String language) {
			this.string = questionText;
			this.language = language;
		}

		public String getString() {
			return string;
		}

		public String getLanguage() {
			return language;
		}

	}

	class QueryObj {

		private final String sparql;

		public QueryObj(String sparqlQueryString) {
			this.sparql = sparqlQueryString;
		}

		public String getSparql() {
			return sparql;
		}

	}

	class Question {

		private final List<QuestionData> question;
		private final QueryObj query;
		private final List<JsonNode> answers;

		public Question(QueryObj queryObj, List<JsonNode> answersArray, List<QuestionData> questionDataArray) {
			this.question = questionDataArray;
			this.query = queryObj;
			this.answers = answersArray;
		}

		public List<QuestionData> getQuestion() {
			return question;
		}

		public QueryObj getQuery() {
			return query;
		}

		public List<JsonNode> getAnswers() {
			return answers;
		}
	}

	private final LinkedList<Question> questions;

	public GerbilExecuteResponse(ObjectMapper objectMapper, String questionText, String language, String sparqlQueryString,
			String jsonAnswerString) throws JSONException, JsonMappingException, JsonProcessingException {

		QuestionData questionData = new QuestionData(questionText, language);
		List<QuestionData> questionDataArray = new LinkedList<>();
		questionDataArray.add(questionData);

		QueryObj queryObj = new QueryObj(sparqlQueryString);

		List<JsonNode> answersArray = new LinkedList<>();
		//JsonNode answersObj2 = null;
		if (jsonAnswerString != null && jsonAnswerString.length() > 0) {
			JsonNode answersObj2 = objectMapper.readTree(jsonAnswerString);
			answersArray.add(answersObj2);
		}

		LinkedList<Question> questionsArray = new LinkedList<>();
		Question question = new Question(queryObj, answersArray, questionDataArray);
		questionsArray.add(question);

		this.questions = questionsArray;
	}

	public LinkedList<Question> getQuestions() {
		return questions;
	}

}
