package eu.wdaqua.qanary.component;

import org.apache.commons.cli.MissingArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class QanaryComponentConfiguration {
    private final Logger logger = LoggerFactory.getLogger(QanaryComponentConfiguration.class);
    private Environment environment;
    private final String[] propertiesToDisplayOnStartup = { //
            "server.port", //
            "spring.application.name", //
            "spring.application.description", //
            "spring.boot.admin.url", //
            "spring.boot.admin.client.url"
    };
    //add any required parameters
    private final String[] requiredParameters = {"server.port","spring.application.name","spring.boot.admin.url"};

    public QanaryComponentConfiguration(@Autowired Environment environment){
        this.environment = environment;
    }

    @PostConstruct
    public void validateRequiredArguments() throws MissingArgumentException {
        for(int i=0;i<requiredParameters.length;i++){
            if(!this.propertyIsValid(requiredParameters[i])){
                logger.error("Configuration parameter '" + requiredParameters[i] + "' was not provided.\n\n" //
                        + "\tRequired: Add parameter '" + requiredParameters[i] + "' to environment configuration\n" //
                        + "\t(c.f., https://www.tutorialspoint.com/spring_boot/spring_boot_application_properties.htm)\n\n");
                throw new MissingArgumentException(requiredParameters[i]);
            }
        }
        logger.info("Current Component Configuration: \n{}", this);
    }

    private boolean propertyIsValid(String property){
        if (this.propertyExists(property)){
            if (!this.getPropertyValue(property).equals("")){
                return true;
            }
        }
        return false;
    }

    private boolean propertyExists(String requiredParameter) {
        return this.environment.containsProperty(requiredParameter);
    }

    public String getPropertyValue(String property){
        return this.environment.getProperty(property);
    }

    public String toString(){
        String output = "";
        for(int i=0;i<propertiesToDisplayOnStartup.length;i++){
            output += String.format("%-40s = %s\n",
                    propertiesToDisplayOnStartup[i],this.getPropertyValue(propertiesToDisplayOnStartup[i]));
        }
        return output;
    }
}
