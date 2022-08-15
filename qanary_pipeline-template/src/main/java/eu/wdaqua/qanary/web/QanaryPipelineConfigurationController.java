package eu.wdaqua.qanary.web;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * controller for service w.r.t pipeline configuration
 */
@CrossOrigin
@RestController
public class QanaryPipelineConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(QanaryPipelineConfiguration.class);
    private final Environment environment;
    private final QanaryPipelineConfiguration qanaryPipelineConfiguration;
    // define properties that should not be configurable using this controller
    private final String[] notConfigurable = {"server.host", "server.port", "qanary.components", "spring.config.name"};

    @Autowired
    public QanaryPipelineConfigurationController(
            Environment environment,
            final QanaryPipelineConfiguration qanaryPipelineConfiguration) {
        this.environment = environment;
        this.qanaryPipelineConfiguration = qanaryPipelineConfiguration;
    }

    private Map<String, Object> excludeProperties(String[] excludedProperties, Map<String, Object> propertyMap) {
        for (String property : excludedProperties) {
            propertyMap.remove(property);
        }
        return propertyMap;
    }

    /**
     * returns configurable properties and their values based on the Environment
     * @throws Exception
     */
    @RequestMapping(value = "/configuration", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getConfigurablePipelineProperties() {

        Map<String, Object> configurationMap = qanaryPipelineConfiguration.getAllKnownProperties(this.environment);
        this.excludeProperties(this.notConfigurable, configurationMap);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(configurationMap);
            return new ResponseEntity<>(jsonString, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * writes changes in pipeline property values to application.local.properties
     */
    @RequestMapping(value = "/configuration", method = RequestMethod.POST, consumes = "application/json")
    public void updateLocalPipelineProperties(@RequestBody String jsonString) throws JSONException {

        JSONObject configJson = new JSONObject(jsonString);
        String filePath = environment.getProperty("spring.config.location");
        Path localConfigPath = Paths.get(new ClassPathResource(filePath).getPath());

        logger.warn("Reloading the configuration. URL is file:{}", localConfigPath);

        try {
            boolean replace = Files.deleteIfExists(localConfigPath);
            File file = new File(filePath);
            logger.info(replace?
                    "Replacing application.local.properties":"Creating new application.local.properties");

            if (file.createNewFile()) {
                logger.info("File was created");
            } else {
                logger.info("File was not created!");
            }
        } catch (IOException e) {
            logger.error("An error occurred creating application.local.properties!");
            e.printStackTrace();
        }

        try {
            FileWriter writer = new FileWriter(filePath);
            Iterator<String> keys = configJson.keys();
            logger.info("writing keys ...");
            while(keys.hasNext()) {
                String key = keys.next();
                String value = configJson.get(key).toString();

                writer.write(key+"="+value+"\n");
            }

            logger.info("saved configuration to application.local.properties");
            writer.close();

        } catch (IOException e) {
            logger.error("An error occurred writing changes to application.local.properties!");
            e.printStackTrace();
        }
    }
}
