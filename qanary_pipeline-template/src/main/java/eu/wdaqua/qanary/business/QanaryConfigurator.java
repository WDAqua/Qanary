package eu.wdaqua.qanary.business;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.codecentric.boot.admin.model.Application;

import eu.wdaqua.qanary.message.QanaryComponentNotAvailableException;
import eu.wdaqua.qanary.message.QanaryExceptionServiceCallNotOk;
import eu.wdaqua.qanary.message.QanaryMessage;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringFinished;

/**
 * Created by didier on 27.03.16.
 */
public class QanaryConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(QanaryConfigurator.class);
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
     */
    private QanaryQuestionAnsweringFinished callServices(List<QanaryComponent> myComponents, QanaryMessage message)
            throws QanaryExceptionServiceCallNotOk {
        QanaryQuestionAnsweringFinished result = new QanaryQuestionAnsweringFinished();
        result.startQuestionAnswering();

        logger.info(message.asJsonString());

        for (QanaryComponent component : myComponents) {

            URI myURI;
            try {
                myURI = new URI(component.getUrl() + "/annotatequestion");
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return result;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<String>(message.asJsonString(), headers);

            logger.debug("POST request will be performed to {} with {}", myURI, message.asJsonString());
            ResponseEntity<QanaryMessage> responseEntity = restTemplate.exchange(myURI, HttpMethod.POST, request,
                    QanaryMessage.class);

            result.appendProtocol(component);
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                message = responseEntity.getBody();
                logger.debug("received: {}", message);
            } else {
                logger.error("call to {} return HTTP {}", component.getName(), responseEntity.getStatusCode());
                throw new QanaryExceptionServiceCallNotOk(component.getName(), responseEntity.getStatusCode());
            }

        }
        result.endQuestionAnswering();
        logger.info("callServices finished: {}", result);
        return result;
    }

    /**
     * create a list of components from the
     */
    private List<QanaryComponent> getComponentsByName(List<String> myComponentNames)
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
                        + e + "\nplease take only one of the following list: " + this.getComponentNames().toString());
            }
        }

        return qanaryComponents;
    }

    /**
     * call of all components (identified by String)
     */
    public QanaryQuestionAnsweringFinished callServicesByName(List<String> myComponentNames, QanaryMessage message)
            throws QanaryComponentNotAvailableException, QanaryExceptionServiceCallNotOk {

        List<QanaryComponent> qanaryComponents = this.getComponentsByName(myComponentNames);
        return this.callServices(qanaryComponents, message);
    }

    public void addComponent(Application application) {
        logger.info("addComponent: id={} name={}", application.getId(), application.getName());
        if (componentsToIndexMap.containsKey(application.getName())) {
            Integer index = Math.min(components.size(), componentsToIndexMap.get(application.getName()));
            components.add(index, new QanaryComponent(application, true));
        }
        components.add(new QanaryComponent(application, false));
        logger.info("availableComponents: {}", this.getComponentNames());
    }

    /**
     * delivers a list of all components currently been available in the pipeline
     */
    public List<String> getComponentNames() {
        List<String> componentsNames = new LinkedList<>();

        for (QanaryComponent qanaryComponent : this.components) {
            componentsNames.add(qanaryComponent.getName());
        }

        return componentsNames;
    }

    /**
     * get the component for a given name
     */
    private QanaryComponent getComponent(String componentName) {
        for (QanaryComponent qanaryComponent : this.components) {
            if (qanaryComponent.getName().compareTo(componentName) == 0) {
                logger.info("found component: " + componentName);
                return qanaryComponent;
            }
        }
        return null;
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
