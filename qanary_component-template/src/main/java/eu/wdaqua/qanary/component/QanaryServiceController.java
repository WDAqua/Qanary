package eu.wdaqua.qanary.component;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import io.swagger.v3.oas.annotations.Operation;


@Controller
public class QanaryServiceController {

    private static final Logger logger = LoggerFactory.getLogger(QanaryServiceController.class);
    
    public final static String filenameOnlyPostInteractionAllowed = "only-post-is-allowed.html";

    @Value("${spring.boot.admin.client.url}")
    private String qanaryHost;

    private QanaryComponent qanaryComponent;
    
    @Autowired
    private Environment env;

    @Inject
    public QanaryServiceController(QanaryComponent qanaryComponent) {
        this.qanaryComponent = qanaryComponent;
        logger.info("qanaryComponent: {}", this.qanaryComponent);
    }

    /**
     * example: 
     <pre>
     	curl -X POST -d 'message={"http://qanary/#endpoint":"http://x.y"}' http://localhost:8080/annotatequestion | python -m json.tool
     </pre>
     *
     */
    @PostMapping(value = { QanaryConfiguration.annotatequestion, "/" + QanaryConfiguration.annotatequestion }, consumes = "application/json", produces = "application/json")
    @ResponseBody
	@Operation(
			summary = "Each Qanary process will implement this endpoint as it is required ", //
			operationId = "showDescriptionOnGetRequestOnRoot", //
			description = "for showing information in a Web browser" //  
	)
    public ResponseEntity<?> annotatequestion(HttpServletRequest request, @RequestBody String message) throws Exception {
        logger.info("annotatequestion: {}", message);
        long start = QanaryUtils.getTime();

        QanaryConfiguration.setServiceUri(new URI(String.format("%s://%s:%d/" + QanaryConfiguration.annotatequestion,
                request.getScheme(), request.getServerName(), request.getServerPort())));
        QanaryConfiguration.setServiceUri(new URI(qanaryHost));

        QanaryMessage myQanaryMessage = new QanaryMessage(message);
        
        this.qanaryComponent.setQanaryMessage(myQanaryMessage);
        this.qanaryComponent.setUtils(myQanaryMessage);
        
        try {
            this.qanaryComponent.process(myQanaryMessage);
		} catch (Exception e) {
			logger.error("Something went wrong while executing the 'process' method: {}", e.getMessage());
			return new ResponseEntity<String>(e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
        

        logger.debug("processing took: {} ms", QanaryUtils.getTime() - start);

        return new ResponseEntity<QanaryMessage>(myQanaryMessage, HttpStatus.OK);
    }

    /**
     * not intended -> fallback: showing information to the user
     * 
     * @param response
     * @return
     * @throws Exception
     */
    @GetMapping(value =  { QanaryConfiguration.annotatequestion, "/" + QanaryConfiguration.annotatequestion } )
    public String showDescriptionOnGetRequest(HttpServletResponse response) throws Exception {
    	return filenameOnlyPostInteractionAllowed;
    }

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
    	
    	return QanaryConfiguration.description_file;
    }
}
