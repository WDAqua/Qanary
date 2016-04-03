package eu.wdaqua.qanary.business;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.codecentric.boot.admin.model.Application;
import eu.wdaqua.qanary.message.QanaryMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by didier on 27.03.16.
 */
public class QanaryConfigurator {

    private final RestTemplate restTemplate;
    private List<QanaryComponent> components;
    private final Map<String, Integer> componentsToIndexMap;

    public QanaryConfigurator(RestTemplate restTemplate, Map<String, Integer> componentsToIndexMap) {
        this.restTemplate = restTemplate;
        this.componentsToIndexMap = Maps.newHashMap();
        this.components = Lists.newArrayList();
    }

    /**
     * Call the url of the services
     */
    public void callServices(QanaryMessage message) {
        List<QanaryComponent> componentsToUse = components.stream()
                .filter(qanaryComponent -> qanaryComponent.isUsed())
                .collect(Collectors.toList());

        for (QanaryComponent component : componentsToUse) {
            HttpEntity<QanaryMessage> request = new HttpEntity<QanaryMessage>(message);
            ResponseEntity<QanaryMessage> responseEntity = restTemplate.exchange(component.getUrl(), HttpMethod.POST,
                    request, QanaryMessage.class);
            if(responseEntity.getStatusCode() == HttpStatus.OK){
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
        components.removeIf(
                qanaryComponent -> qanaryComponent.getName().equals(application.getName()
                )
        );
    }

}
