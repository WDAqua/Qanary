package eu.wdaqua.qanary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.notify.AbstractEventNotifier;
import eu.wdaqua.qanary.business.QanaryComponent;
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
						availableComponents.put(instanceName, instance);
					} else {
						logger.warn("registering component \"{}\" has no callable URL ({})", instanceName,
								instance.getRegistration().getServiceUrl());
					}
				} else {
					availableComponents.put(instanceName, null);
				}
			} else {
				logger.debug("Instance {} ({}) {}", instanceName, event.getInstance(), event.getType());
			}
		});
	}

	public List<String> getAvailableComponentNames() {
		return new ArrayList<>(availableComponents.keySet());
	}

	public List<QanaryComponent> getAvailableComponentsFromNames(List<String> componentsToBeCalled) {

		List<QanaryComponent> components = new LinkedList<>();
		for (String componentName : componentsToBeCalled) {
			Instance componentInstance = this.availableComponents.getOrDefault(componentName, null);
			if (componentInstance != null && componentInstance.getStatusInfo().isUp()) {
				boolean used = true;
				components.add(
						new QanaryComponent(componentName, componentInstance.getRegistration().getServiceUrl(), used));
			}
		}

		if (components.size() == 0) {
			logger.warn("getAvailableComponentsFromNames: found 0 components");
		} else {
			logger.info("getAvailableComponentsFromNames: found {} components", components.size());
			for (QanaryComponent myQanaryComponent : components) {
				logger.debug("{}: url={}", myQanaryComponent.getName(), myQanaryComponent.getUrl());
			}
		}

		return components;
	}
}