package eu.wdaqua.qanary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.notify.AbstractEventNotifier;
import eu.wdaqua.qanary.business.QanaryComponent;
import org.springframework.cache.annotation.Cacheable;
import reactor.core.publisher.Mono;

public class QanaryComponentRegistrationChangeNotifier extends AbstractEventNotifier {
	private static final Logger logger = LoggerFactory.getLogger(QanaryComponentRegistrationChangeNotifier.class);

	private Map<String, Instance> availableComponents = new HashMap<>();

	public QanaryComponentRegistrationChangeNotifier(InstanceRepository repository) {
		super(repository);
		logger.warn("CustomNotifier");
	}

	@Override
	protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
		String instanceName = instance.getRegistration().getName();
		return Mono.fromRunnable(() -> {
			if (event instanceof InstanceStatusChangedEvent) {
				String status = ((InstanceStatusChangedEvent) event).getStatusInfo().getStatus();
				logger.info("Instance {} ({}) is {}", instance.getRegistration().getName(), instance.getId(), status);
				// only show components as available that are actually UP
				if (status.toUpperCase().compareTo("UP") == 0) {
					if (instance.getRegistration().getServiceUrl() != null) {
						logger.debug("registering component \"{}\" has URL ({})", instanceName,
								instance.getRegistration().getServiceUrl());
						this.addAvailableComponent(instanceName, instance);
					} else {
						logger.warn("registering component \"{}\" has no callable URL ({})", instanceName,
								instance.getRegistration().getServiceUrl());
					}
				} else if(status.toUpperCase().compareTo("OFFLINE") == 0) {
					availableComponents.remove(instanceName);
				} else {
					availableComponents.put(instanceName, null);
				}
			} else {
				logger.debug("Instance {} ({}) {}", instanceName, event.getInstance(), event.getType());
			}
		});
	}
	
			protected void addAvailableComponent(String instanceName, Instance instance) {
		this.getAvailableComponents().put(instanceName, instance);
	}

	public List<String> getAvailableComponentNames() {
		return new ArrayList<>(availableComponents.keySet());
	}

	@Cacheable
	public Map<String, String> getComponentsAndAvailability() {
		return this.availableComponents.entrySet().stream().collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> entry.getValue().getStatusInfo().getStatus()
		));
	}

	public Map<String, Instance> getAvailableComponents() {
		return this.availableComponents;
	}
	
	/**
	 * returns a Qanary component for a given name or NULL
	 * 
	 * @param componentName
	 * @return
	 */
	public QanaryComponent getAvailableComponentFromName(String componentName) {
		Instance componentInstance = this.getAvailableComponents().getOrDefault(componentName, null);
		if (componentInstance != null && componentInstance.getStatusInfo().isUp()) {
			boolean used = true;
			return new QanaryComponent(componentName, componentInstance.getRegistration().getServiceUrl(), used);
		} else {
			return null;
		}
	}

	/**
	 * for given list of component names find the ones that are registered and up (i.e., they can be called during the process)  
	 * 
	 * @param componentsToBeCalled
	 * @return
	 */
	public List<QanaryComponent> getAvailableComponentsFromNames(List<String> componentsToBeCalled) {
		logger.debug("getAvailableComponentsFromNames: componentsToBeCalled={} availableComponents={}", componentsToBeCalled, this.getAvailableComponents());
		List<QanaryComponent> components = new LinkedList<>();
		for (String componentName : componentsToBeCalled) {
			Instance componentInstance = this.getAvailableComponents().getOrDefault(componentName, null);
			logger.debug("searched for component with name \"{}\" and found: {}", componentName, componentInstance);
			if (this.isComponentUsable(componentInstance)) {
				boolean used = true;
				components.add(
						new QanaryComponent(componentName, componentInstance.getRegistration().getServiceUrl(), used));
			} else {
				if( componentInstance != null && !componentInstance.getStatusInfo().isUp() ) {
					logger.warn("component \"{}\" is registered, but offline", componentName);
				} else if( componentInstance == null ) {
					logger.warn("component \"{}\" is not registered", componentName);
				}

			}
		}

		if (components.size() == 0) {
			logger.warn("getAvailableComponentsFromNames: found 0 components");
		} else {
			logger.info("getAvailableComponentsFromNames: found {} components", components.size());
			for (QanaryComponent myQanaryComponent : components) {
				logger.debug("getAvailableComponentsFromNames: found component \"{}\": url={}", myQanaryComponent.getName(), myQanaryComponent.getUrl());
			}
		}

		return components;
	}

	protected boolean isComponentUsable(Instance componentInstance) {
		return componentInstance != null && componentInstance.getStatusInfo().isUp();
	}
}