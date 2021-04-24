package eu.wdaqua.qanary.web.messages;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Request data for creating a Question Answering process
 * 
 * Example: { "question":"What is the real name of Batman?", "componentList":
 * ["NED-DBpediaSpotlight","QueryBuilderSimpleRealNameOfSuperHero"],
 * "priorConversation": "urn:graph:806261d9-4601-4c8c-8603-926eee707c38", }
 * 
 * @author anbo-de
 *
 */
public class RequestQuestionAnsweringProcess {
	private String question;
	private List<String> componentlist = new ArrayList<>();
	private List<String> language = new ArrayList<>();
	private List<String> targetdata = new ArrayList<>();
	private URI priorConversation;

	public RequestQuestionAnsweringProcess() {
		// pass
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public List<String> getcomponentlist() {
		return componentlist;
	}

	public void setcomponentlist(List<String> componentsToBeCalled) {
		this.componentlist = componentsToBeCalled;
	}

	public List<String> getLanguage() {
		return language;
	}

	public void setLanguage(List<String> language) {
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
				+ " -- componentList: " + Arrays.toString(getcomponentlist().toArray()) //
				+ " -- priorConversation: " + getPriorConversation();
	}

}
