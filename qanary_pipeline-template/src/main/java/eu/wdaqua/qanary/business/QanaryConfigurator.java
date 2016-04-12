package eu.wdaqua.qanary.business;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.codecentric.boot.admin.model.Application;
import eu.wdaqua.qanary.message.QanaryComponentNotAvailableException;
import eu.wdaqua.qanary.message.QanaryMessage;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringFinished;

/**
 * Created by didier on 27.03.16.
 */
public class QanaryConfigurator {

	private final RestTemplate restTemplate;
	private List<QanaryComponent> components;
	private final Map<String, Integer> componentsToIndexMap;
	private final int port;
	private final String host;
	private final URI endpoint;

	public QanaryConfigurator(RestTemplate restTemplate, Map<String, Integer> componentsToIndexMap, String host,
			int port, URI endpoint) {
		this.restTemplate = restTemplate;
		this.componentsToIndexMap = Maps.newHashMap();

		// TODO: from config
		this.components = Lists.newArrayList();

		// from config
		this.port = port;
		this.host = host;
		this.endpoint = endpoint;
	}

	/**
	 * call the provided components sequentially, pass throught a QanaryMessage
	 * 
	 * @param myComponents
	 * @param message
	 * @return
	 */
	public QanaryQuestionAnsweringFinished callServices(List<QanaryComponent> myComponents, QanaryMessage message) {
		QanaryQuestionAnsweringFinished result = new QanaryQuestionAnsweringFinished();
		result.startQuestionAnswering();
		List<QanaryComponent> componentsToUse = myComponents.stream()
				.filter(qanaryComponent -> qanaryComponent.isUsed()).collect(Collectors.toList());

		for (QanaryComponent component : componentsToUse) {
			HttpEntity<QanaryMessage> request = new HttpEntity<QanaryMessage>(message);
			ResponseEntity<QanaryMessage> responseEntity = restTemplate.exchange(component.getUrl(), HttpMethod.POST,
					request, QanaryMessage.class);
			if (responseEntity.getStatusCode() == HttpStatus.OK) {
				message = responseEntity.getBody();
			}
		}
		result.endQuestionAnswering();
		return result;
	}

	/**
	 * create a list of components from the
	 * 
	 * @param myComponentNames
	 * @return
	 * @throws QanaryComponentNotAvailableException
	 */
	public List<QanaryComponent> getComponentsByName(List<String> myComponentNames)
			throws QanaryComponentNotAvailableException {
		// check if the list contains valid IDs of components if not all are
		// available throw an exception
		List<QanaryComponent> qanaryComponents = new LinkedList<>();
		for (String componentName : myComponentNames) {
			try {
				QanaryComponent qanaryComponent = this.getComponent(componentName);
				if (qanaryComponent == null) {
					throw new NullPointerException("qanaryComponent was null.");
				}
				qanaryComponents.add(qanaryComponent);
			} catch (Exception e) {
				throw new QanaryComponentNotAvailableException(componentName
						+ " (currently) not available. Please check the name and that the component is still online.\n"
						+ e.getMessage());
			}
		}

		return qanaryComponents;
	}

	/**
	 * call of all components (identified by String)
	 * 
	 * @param myComponentNames
	 * @param message
	 * @return
	 * @throws QanaryComponentNotAvailableException
	 */
	public QanaryQuestionAnsweringFinished callServicesByName(List<String> myComponentNames, QanaryMessage message)
			throws QanaryComponentNotAvailableException {

		List<QanaryComponent> qanaryComponents = this.getComponentsByName(myComponentNames);
		return this.callServices(qanaryComponents, message);
	}

	public void addComponent(Application application) {
		if (componentsToIndexMap.containsKey(application.getName())) {
			Integer index = Math.min(components.size(), componentsToIndexMap.get(application.getName()));
			components.add(index, new QanaryComponent(application, true));
		}
		components.add(new QanaryComponent(application, false));
	}

	/**
	 * get the component for a given name
	 * 
	 * @param componentName
	 * @return
	 */
	public QanaryComponent getComponent(String componentName) {
		int componentIndex = componentsToIndexMap.get(componentName);
		return components.get(componentIndex);
	}

	public void removeComponent(Application application) {
		components.removeIf(qanaryComponent -> qanaryComponent.getName().equals(application.getName()));
	}

	public int getPort() {
		return this.port;
	}

	public String getHost() {
		return this.host;
	}

	public URI getEndpoint() {
		return this.endpoint;
	}

}
