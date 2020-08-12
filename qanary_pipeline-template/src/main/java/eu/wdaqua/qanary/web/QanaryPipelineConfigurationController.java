package eu.wdaqua.qanary.web;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * controller for service w.r.t pipeline configuration
 */
@RestController
public class QanaryPipelineConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(QanaryPipelineConfiguration.class);
    private final Environment environment;
    private final QanaryPipelineConfiguration qanaryPipelineConfiguration;
    // define properties that should not be configurable using this controller
    private final String[] notConfigurable = {"server.host", "server.port", "qanary.components"};

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
     */
    @CrossOrigin
    @RequestMapping(value = "/configuration", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<JSONObject> getConfigurablePipelineProperties() {

        Map<String, Object> configurationMap = qanaryPipelineConfiguration.getAllKnownProperties(this.environment);

       this.excludeProperties(this.notConfigurable, configurationMap);

        JSONObject json = new JSONObject(configurationMap);
        ResponseEntity<JSONObject> response = new ResponseEntity<>(json, HttpStatus.OK);

        return response;
    }

    /**
     * writes changes in pipeline property values to application.local.properties
     * TODO better logging outputs
     */
    @CrossOrigin
    @RequestMapping(value = "/configuration", method = RequestMethod.POST, consumes = "application/json")
    public void updateLocalPipelineProperties(@RequestBody JSONObject configJson) {

        String filePath = "qanary_pipeline-template/src/main/resources/application.local.properties";
        Path localConfigPath = Paths.get(filePath);

        logger.info("Request pipeline configuration change");

        // TODO implement fail save with tmp file
        try {
            boolean replace = Files.deleteIfExists(localConfigPath);
            File file = new File(filePath);
            logger.info(replace?
                    "Overwriting application.local.properties":"application.local.properties was not found");

            if (file.createNewFile()) {
                logger.info("Saved changes to application.local.properties");
            } else {
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter writer = new FileWriter(filePath);

            for( Object key : configJson.keySet()) {
                String value = configJson.get(key.toString()).toString();
                writer.write(key+"="+value+"\n");
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
