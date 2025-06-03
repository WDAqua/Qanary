package eu.wdaqua.qanary.explainability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoggingConfigurator {

    private static boolean method_logging;

    private static boolean query_logging;
    private final Logger logger = LoggerFactory.getLogger(LoggingConfigurator.class);

    public LoggingConfigurator() {
        logger.info("LoggingConfigurator initialized");
    }

    public static boolean isMethod_logging() {
        return method_logging;
    }

    public static boolean isQuery_logging() {
        return query_logging;
    }

    @Value("${method.logging:false}")
    public void setMethodLogging(boolean method_logging) {
        LoggingConfigurator.method_logging = method_logging;
    }

    @Value("${query.logging:false}")
    public void setQueryLogging(boolean query_logging) {
        LoggingConfigurator.query_logging = query_logging;
    }

    public String getCurrentLoggingInformation() {
        return "Method logging: " + method_logging + "\n" +
                "Query logging: " + query_logging;
    }

    @GetMapping("/logging-settings")
    public ResponseEntity<String> getLoggingSettings() {
        return new ResponseEntity<>(getCurrentLoggingInformation(), HttpStatus.OK);
    }

    @Profile("development")
    @PostMapping("/methodlogging")
    public ResponseEntity<String> changeMethodLogging() {
        LoggingConfigurator.method_logging = !method_logging;
        logger.info("Changed method logging to: " + method_logging);
        return new ResponseEntity<>(getCurrentLoggingInformation(), HttpStatus.OK);
    }

    @Profile("development")
    @PostMapping("/querylogging")
    public ResponseEntity<String> changeQueryLogging() {
        LoggingConfigurator.query_logging = !query_logging;
        logger.info("Changed query logging to: " + query_logging);
        return new ResponseEntity<>(getCurrentLoggingInformation(), HttpStatus.OK);
    }

}
