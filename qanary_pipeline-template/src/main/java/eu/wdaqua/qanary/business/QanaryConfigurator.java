package eu.wdaqua.qanary.business;

import java.net.URL;
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
import eu.wdaqua.qanary.message.QanaryMessage;

/**
 * Created by didier on 27.03.16.
 */
public class QanaryConfigurator {

	private final RestTemplate restTemplate;
	private List<QanaryComponent> components;
	private final Map<String, Integer> componentsToIndexMap;
	private final int port;
	private final String host;
	private final URL endpoint;

	public QanaryConfigurator(RestTemplate restTemplate, Map<String, Integer> componentsToIndexMap, String host,
			int port, URL endpoint) {
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
	 * Call the url of the services
	 */
	public void callServices(QanaryMessage message) {
		List<QanaryComponent> componentsToUse = components.stream().filter(qanaryComponent -> qanaryComponent.isUsed())
				.collect(Collectors.toList());

		for (QanaryComponent component : componentsToUse) {
			HttpEntity<QanaryMessage> request = new HttpEntity<QanaryMessage>(message);
			ResponseEntity<QanaryMessage> responseEntity = restTemplate.exchange(component.getUrl(), HttpMethod.POST,
					request, QanaryMessage.class);
			if (responseEntity.getStatusCode() == HttpStatus.OK) {
				message = responseEntity.getBody();
			}
		}
	}

	public void addComponent(Application application) {
		if (componentsToIndexMap.containsKey(application.getName())) {
			Integer index = Math.min(components.size(), componentsToIndexMap.get(application.getName()));
			components.add(index, new QanaryComponent(application, true));
		}
		components.add(new QanaryComponent(application, false));
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

	public URL getEndpoint() {
		return this.endpoint;
	}

}
