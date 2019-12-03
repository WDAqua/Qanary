package eu.wdaqua.qanary.web;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * a simple endpoint for mapping request of older Spring Boot Admin clients (v1)
 * to the new registration endpoint (Spring Boot Admin Server v2) 
 * implemented for compatibility purposes only
 * 
 * @author AnBo
 *
 */
@Controller
public class QanarySpringBootAdminCompatibilityRedirectController {
	private static final Logger logger = LoggerFactory
			.getLogger(QanarySpringBootAdminCompatibilityRedirectController.class);

	@Autowired
	Environment environment;

	/**
	 * inner class representing the response to the registering component
	 */
	class Result {
		private String id;

		public Result(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}

	@PostMapping(path = "/api/applications") // path of old Spring Boot Admin Server endpoint for registering components
	@ResponseBody
	public ResponseEntity<Result> registrationOfOldSpringBootAdminClient(@RequestBody String values)
			throws IOException {

		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/json");

		String newSpringBootAdminServerRegisterEndpoint = environment.getProperty("server.host") + ":"
				+ environment.getProperty("server.port") + "/instances";
		ResponseEntity<String> response = new RestTemplate().postForEntity(newSpringBootAdminServerRegisterEndpoint,
				new HttpEntity<>(values, headers), String.class);

		JsonNode json = new ObjectMapper().readTree(response.getBody());
		Result result = new Result(json.get("id").asText());

		logger.info("Spring Boot Admin legacy interface was used {} => Response=({},{})", values,
				response.getStatusCode(), json.toString());
		return new ResponseEntity<>(result, response.getStatusCode());
	}
}
