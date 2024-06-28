package eu.wdaqua.qanary.component;

import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import eu.wdaqua.qanary.component.exceptions.AmbiguousExtendingComponentClass;
import eu.wdaqua.qanary.component.exceptions.NoExtendingComponentClass;
import io.swagger.v3.oas.annotations.Operation;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;

// Conditional controller
@Controller
// Used by default, but excluded when a pipeline doesn't act as component
@ConditionalOnProperty(name = "pipeline.as.component", matchIfMissing = true, havingValue = "true")

public class QanaryServiceControllerRoot {

    private final Logger logger = LoggerFactory.getLogger(QanaryServiceControllerRoot.class);
    @Autowired
    private Environment env;

    /**
     * fallback: showing description
     *
     * @param response
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/")
    @Operation(
            summary = "Show description of component (HTML page)", //
            operationId = "showDescriptionOnGetRequestOnRoot", //
            description = "for showing information in a Web browser" //
    )
    public String showDescriptionOnGetRequestOnRoot(HttpServletResponse response, Model model, HttpSession session) throws Exception {

        List<String> envImportantPropertyNames = Arrays.asList("spring.application.name", "spring.application.description", "springdoc.swagger-ui.path", "spring.boot.admin.url");

        // collect all important properties and add them to the session attributes
        for (String name : envImportantPropertyNames) {
            // replace characters that will not be accepted by Thymeleaf
            String sessionDataName = name.replace(".", "_").replace("-", "_");
            // add all values to the session including a default text if the parameter is not available
            String sessionDataValue = env.getProperty(name, "This text is shown as the property " + name + " is not defined (e.g., in application.properties).");
            session.setAttribute(sessionDataName, sessionDataValue);
            logger.info("session | {} -> {}={}", name, sessionDataName, session.getAttribute(sessionDataName));
        }

        Map<String, String> envImportantPropertyNameValue = new HashMap<>();
        envImportantPropertyNameValue.put("component_description_url", QanaryConfiguration.description);
        envImportantPropertyNameValue.put("component_description_file", QanaryConfiguration.description_file);
        envImportantPropertyNameValue.put("rdfcomponentdescription", QanaryConfiguration.rdfcomponentdescription);
        envImportantPropertyNameValue.put("ImplementationVersion", getClass().getPackage().getImplementationVersion());
        envImportantPropertyNameValue.put("ImplementationTitle", getClass().getPackage().getImplementationTitle());
        envImportantPropertyNameValue.put("ImplementationVendor", getClass().getPackage().getImplementationVendor());
        envImportantPropertyNameValue.put("Name", getClass().getPackage().getName());
        envImportantPropertyNameValue.put("SpecificationTitle", getClass().getPackage().getSpecificationTitle());
        envImportantPropertyNameValue.put("SpecificationVendor", getClass().getPackage().getSpecificationVendor());
        envImportantPropertyNameValue.put("SpecificationVersion", getClass().getPackage().getSpecificationVersion());

        try {
            //find the specific implemented Qanary component
            Class<? extends QanaryComponent> extendingComponent = getExtendingComponent();
            envImportantPropertyNameValue.put("componentImplementationVersion", extendingComponent.getPackage().getImplementationVersion());
            envImportantPropertyNameValue.put("componentImplementationTitle", extendingComponent.getPackage().getImplementationTitle());
            envImportantPropertyNameValue.put("componentImplementationVendor", extendingComponent.getPackage().getSpecificationVendor());

        } catch (Exception e) {
            logger.warn("No class implementing QanaryComponent could be found during runtime!");
            logger.warn(e.getMessage());
        }

        for (Map.Entry<String, String> entry : envImportantPropertyNameValue.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            session.setAttribute(key, val);
            logger.info("session | {}={}", key, session.getAttribute(key));
        }

        return QanaryConfiguration.description_file;
    }

    /**
     * get the specific implemented component that extends eu.wdaqua.qanary.component
     *
     * @return
     * @throws Exception
     */
    private Class<? extends QanaryComponent> getExtendingComponent() throws Exception {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .addScanners(Scanners.SubTypes.filterResultsBy(s -> true))
                        .forPackages("eu.wdaqua.qanary.component"));
        // use reflections to find subtypes of eu.wdaqua.qanary.component
        Set<Class<? extends QanaryComponent>> classes = reflections.getSubTypesOf(QanaryComponent.class);
        // exactly one class is expected
        if (classes.size() == 1) {
            logger.debug("Found class: {}", classes.iterator().next().getName());
            logger.debug("version: {}", classes.iterator().next().getPackage().getImplementationVersion());
            // return the first (and only) one
            return classes.iterator().next();
        } else if (classes.size() == 0) {
            throw new NoExtendingComponentClass(QanaryComponent.class);
        } else {
            throw new AmbiguousExtendingComponentClass(QanaryComponent.class);
        }
    }

}
