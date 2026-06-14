package eu.wdaqua.qanary.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

import eu.wdaqua.qanary.QanaryComponentRegistrationChangeNotifier;
import eu.wdaqua.qanary.business.QanaryComponent;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.values.Registration;
import io.swagger.v3.oas.annotations.Operation;

/**
 * Controller for Qanary pipeline service w.r.t. components intended to offer configuration information, 
 * s.t., an integration in other applications is possible
 */
@CrossOrigin
@RestController
public class QanaryConfigurationController {

	private final QanaryComponentRegistrationChangeNotifier registrationChangeNotifier; 

	@Autowired
	public QanaryConfigurationController(QanaryComponentRegistrationChangeNotifier registrationChangeNotifier) {
		this.registrationChangeNotifier = registrationChangeNotifier;
	}

	@RequestMapping(value="/components", method=RequestMethod.GET, produces="application/json")
	@Operation(
		summary="get a list of all registered components",
		operationId="getAvailableComponents",
		description="Returns a list of registered and available components, containing their name and url."
	)
	public ResponseEntity<List<Map<String, String>>> getAvailableComponents() {
		List<Map<String, String>> json = new ArrayList<>();
		List<String> componentNames = registrationChangeNotifier.getAvailableComponentNames();
		List<QanaryComponent> components =
			registrationChangeNotifier.getAvailableComponentsFromNames(componentNames);
		for (QanaryComponent component : components) {
			Map<String, String> object = new LinkedHashMap<>();
			String name = component.getName();
			String url = "/components/" + name; // create a relative url components/name
			object.put("name", name);
			object.put("url", url);
			json.add(object);
		}

		ResponseEntity<List<Map<String, String>>> response = new ResponseEntity<>(json, HttpStatus.OK);
		return response;
	}

	/**
	 * Returns ALL registered components together with their current availability
	 * status, so a client (e.g. the embedded web frontend) can also show
	 * components that are registered but not accessible (and mark them as not
	 * selectable). Unlike {@link #getAvailableComponents()} this does not filter
	 * out components that are currently down.
	 */
	@RequestMapping(value = "/components/availability", method = RequestMethod.GET, produces = "application/json")
	@Operation(
		summary = "get all registered components with their availability status",
		operationId = "getComponentsWithAvailability",
		description = "Returns every registered component with its name, description URL, current status (e.g. UP/DOWN/OFFLINE) and an 'accessible' flag. Accessible components can be used in a pipeline run; the others are registered but currently not callable."
	)
	public ResponseEntity<List<Map<String, Object>>> getComponentsWithAvailability() {
		List<Map<String, Object>> json = new ArrayList<>();
		// getAvailableComponents() actually holds every registered instance,
		// including the ones that are not UP (see the registration notifier).
		for (Map.Entry<String, Instance> entry : registrationChangeNotifier.getAvailableComponents().entrySet()) {
			Instance instance = entry.getValue();
			String status = instance.getStatusInfo().getStatus();
			Map<String, Object> object = new LinkedHashMap<>();
			object.put("name", entry.getKey());
			object.put("url", "/components/" + entry.getKey());
			object.put("status", status);
			object.put("accessible", instance.getStatusInfo().isUp());

			// expose the component's own registration so the frontend can show its
			// service URL (for an embedded iframe), host/IP and port in an info overlay
			Registration registration = instance.getRegistration();
			if (registration != null) {
				String serviceUrl = registration.getServiceUrl();
				object.put("serviceUrl", serviceUrl);
				object.put("healthUrl", registration.getHealthUrl());
				object.put("managementUrl", registration.getManagementUrl());
				if (serviceUrl != null && !serviceUrl.isEmpty()) {
					try {
						URI uri = URI.create(serviceUrl);
						object.put("host", uri.getHost());
						object.put("port", uri.getPort() == -1 ? null : uri.getPort());
					} catch (IllegalArgumentException e) {
						// leave host/port unset if the service URL is not parseable
					}
				}
			}
			json.add(object);
		}
		return new ResponseEntity<>(json, HttpStatus.OK);
	}
}
