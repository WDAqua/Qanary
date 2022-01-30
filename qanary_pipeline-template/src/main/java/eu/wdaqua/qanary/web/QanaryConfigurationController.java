package eu.wdaqua.qanary.web;

import java.util.List;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import eu.wdaqua.qanary.QanaryComponentRegistrationChangeNotifier;
import eu.wdaqua.qanary.business.QanaryComponent;

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
	public ResponseEntity<JSONArray> getAvailableComponents() {
		JSONArray json = new JSONArray();
		List<String> componentNames = registrationChangeNotifier.getAvailableComponentNames();
		List<QanaryComponent> components = 
			registrationChangeNotifier.getAvailableComponentsFromNames(componentNames);
		for (QanaryComponent component : components) {
			JSONObject object = new JSONObject();
			String name = component.getName();
			String url = "/components/" + name; // create a relative url components/name
			object.put("name", name);
			object.put("url", url);
			json.add(object);
		}

		ResponseEntity<JSONArray> response = new ResponseEntity<>(json, HttpStatus.OK);
		return response;
	}
}
