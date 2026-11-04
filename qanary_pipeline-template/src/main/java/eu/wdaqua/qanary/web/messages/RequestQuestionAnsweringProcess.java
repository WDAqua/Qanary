package eu.wdaqua.qanary.web.messages;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.wdaqua.qanary.process.QanaryProcessDefinition;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request data for creating a Question Answering process
 *
 * Example: { "question":"What is the real name of Batman?", "componentlist":
 * ["NED-DBpediaSpotlight","QueryBuilderSimpleRealNameOfSuperHero"],
 * "priorConversation": "urn:graph:806261d9-4601-4c8c-8603-926eee707c38", }
 *
 * The componentlist might also contain parallel blocks (see
 * {@link QanaryProcessDefinition}), e.g., { "question":"...", "componentlist":
 * ["a", {"p1": ["b"], "x": ["c", "d"]}, "e"] }
 *
 * @author anbo-de
 *
 */
public class RequestQuestionAnsweringProcess {
	private String question;
	private QanaryProcessDefinition componentlist = QanaryProcessDefinition.EMPTY;
	private String language = null;
	private List<String> targetdata = new ArrayList<>();
	private URI priorConversation;
	private String additionalTriples;

	public RequestQuestionAnsweringProcess() {
		// pass
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	/**
	 * @return the components to be called: their names (called one after another)
	 *         and parallel blocks
	 */
	@JsonProperty("componentlist")
	@Schema(name = "componentlist", types = { "array" }, defaultValue = "[]", //
			description = "The components to be called, in this order. An element is either the name of a component "
					+ "or a parallel block: an object whose properties are named streams (arrays like this one), which "
					+ "run at the same time. The stream names are chosen freely, have to be unique, and identify the "
					+ "streams in error reports.", //
			example = "[\"a\", {\"p1\": [\"b\"], \"x\": [\"c\", \"d\"]}, \"e\"]")
	public QanaryProcessDefinition getProcessDefinition() {
		return componentlist;
	}

	/**
	 * @param processDefinition the components to be called, null for none
	 */
	@JsonProperty("componentlist")
	public void setProcessDefinition(QanaryProcessDefinition processDefinition) {
		this.componentlist = processDefinition == null ? QanaryProcessDefinition.EMPTY : processDefinition;
	}

	/**
	 * @return the names of all components in the order they are listed
	 * @deprecated does not show parallel blocks; use
	 *             {@link #getProcessDefinition()}
	 */
	@Deprecated
	@JsonIgnore
	public List<String> getcomponentlist() {
		return componentlist.componentNames();
	}

	/**
	 * @param componentsToBeCalled the components to be called one after another
	 */
	@JsonIgnore
	public void setcomponentlist(List<String> componentsToBeCalled) {
		this.componentlist = QanaryProcessDefinition.sequential(componentsToBeCalled);
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public List<String> getTargetdata() {
		return targetdata;
	}

	public void setTargetdata(List<String> targetdata) {
		this.targetdata = targetdata;
	}

	public URI getPriorConversation() {
		return priorConversation;
	}

	public void setPriorConversation(URI priorConversation) {
		this.priorConversation = priorConversation;
	}

	@Override
	public String toString() {
		return "RequestQuestionAnsweringProcess " //
				+ " -- question: \"" + getQuestion() + "\"" //
				+ " -- componentList: " + getProcessDefinition() //
				+ " -- priorConversation: " + getPriorConversation() //
				+ " -- language: " + getLanguage();
	}

	public String getAdditionalTriples() {
		return this.additionalTriples;
	}

	public void setAdditionalTriples(String additionalTriples) {
		this.additionalTriples = additionalTriples;
	}

}
