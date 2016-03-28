package eu.wdaqua.qanary.business;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.codecentric.boot.admin.model.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by didier on 27.03.16.
 */
@Service
public class QanaryConfigurator {

    private final RestTemplate restTemplate;
    private List<QanaryComponent> components;
    private final Map<String, Integer> componentsToIndexMap;

    @Autowired
    public QanaryConfigurator(RestTemplate restTemplate, Map<String,Integer> componentsToIndexMap) {
        this.restTemplate = restTemplate;
        this.componentsToIndexMap = Maps.newHashMap();
        this.components = Lists.newArrayList();
    }

    /**
     * Call the url of the services
     */
    public void callServices() {
        List<QanaryComponent> componentsToUse = components.stream()
                .filter(qanaryComponent -> qanaryComponent.isUsed())
                .collect(Collectors.toList());

        for (QanaryComponent component : componentsToUse) {
            // TODO call service
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
