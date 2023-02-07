package eu.wdaqua.qanary.web;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import eu.wdaqua.qanary.commons.config.QanaryConfiguration;

/**
 * collection of helpers implemented as web UI to provide convenient access to Qanary
 * implementation
 *
 * @author AnBo
 */
@Controller
public class QanaryWebHelperController {

	private static final Logger logger = LoggerFactory.getLogger(QanaryWebHelperController.class);

    @Autowired
    private Environment env;

    /**
     * get some information about your Qanary implementation using the fancy HTML template
     * description.html
     */
    @RequestMapping("/description")
    public String description(HttpServletResponse response, Model model, HttpSession session) {
    	
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
    	
    	Map<String,String> envImportantPropertyNameValue = new HashMap<>();
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

    	for (Map.Entry<String, String> entry : envImportantPropertyNameValue.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
	    	session.setAttribute(key, val);
    		logger.info("session | {}={}", key, session.getAttribute(key));
		}
        return "description";
    }

    /**
     * web form for input a question as defined in inputtextquestion.html
     */
    @RequestMapping("/inputtextquestion")
    public String inputtextquestion() {
        return "inputtextquestion";
    }

    /**
     * start a question answering process by using this web form as defined in
     * startquestionanswering.html
     */
    @RequestMapping("/startquestionanswering")
    public String startquestionanswering() {
        return "startquestionanswering";
    }

}
