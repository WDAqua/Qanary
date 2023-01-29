package eu.wdaqua.qanary.component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import io.swagger.v3.oas.annotations.Operation;

/**
 * provides access to the components definition in different RDF formats
 *
 */
@Controller
public class QanaryComponentDescriptionController {

	private static final Logger logger = LoggerFactory.getLogger(QanaryComponentDescriptionController.class);
	private static final String COMPONENT_TURTLE_DESCRIPTION_FILE_LOCATION = "component-description.ttl";

	private ApplicationContext context;

	public Model model;

	public QanaryComponentDescriptionController(ApplicationContext context) throws FileNotFoundException {
		this.context = context;
		initQanaryServiceDecription();
	}

	private void initQanaryServiceDecription(){
		String filename;

		try {
			try {
				// standard location
				Resource file = context.getResource("classpath:" + COMPONENT_TURTLE_DESCRIPTION_FILE_LOCATION);
				filename = file.getURI().toASCIIString();
			} catch (Exception e) {
				// fallback for some development scenarios
				File x = new File("src/main/resources/" + COMPONENT_TURTLE_DESCRIPTION_FILE_LOCATION);
				filename = x.getAbsolutePath();
			}

			if (filename != null) {
				logger.info("location of {}: {}", COMPONENT_TURTLE_DESCRIPTION_FILE_LOCATION, filename);
				try {
					if (model == null) {
						model = RDFDataMgr.loadModel(filename);
						logger.info("size of loaded model: {} triples as TURTLE\n{}", model.size(),
								getModelAsString("TURTLE"));
					}
				} catch (Exception e) {
					logger.warn("could not find a Turtle service description at '{}': {}", filename, e);
				}
			} else {
				logger.warn("could not find a Turtle service description as null is provided.");
			}
		} catch (Exception e) {
			logger.warn("problem with component description at '{}': {}", COMPONENT_TURTLE_DESCRIPTION_FILE_LOCATION, e);
		}
	}

	/**
	 * provides a description HTML page of the component, replace description.html
	 * to custom page
	 */
	@GetMapping(value = QanaryConfiguration.description)
	public String description(HttpServletResponse response) {
		return QanaryConfiguration.description_file;
	}

	private void logHeader(HttpServletRequest request) {
		Enumeration<String> headerNames = request.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				logger.debug("Header: {}", request.getHeader(headerNames.nextElement()));
			}
		} else {
			logger.warn("headers: null");
		}
	}

	@ExceptionHandler(NotImplementedException.class)
	@GetMapping(value = QanaryConfiguration.rdfcomponentdescription, produces = { "application/x-javascript",
			"application/json", "application/ld+json" })
	public @ResponseBody String getModelAsJson(HttpServletRequest request) {
		this.logHeader(request);
		return getModelAsString("RDF/JSON");
	}

	@GetMapping(value = QanaryConfiguration.rdfcomponentdescription, produces = { "application/xml",
			"application/rdf+xml" })
	public @ResponseBody String getModelAsXml(HttpServletRequest request) {
		this.logHeader(request);
		return getModelAsString("RDF/XML");
	}

	@GetMapping(value = QanaryConfiguration.rdfcomponentdescription, produces = { "application/n-triples" })
	public @ResponseBody String getModelAsNTriples(HttpServletRequest request) throws IOException {
		this.logHeader(request);
		return getModelAsString("N-TRIPLE");
	}

	@GetMapping(value = QanaryConfiguration.rdfcomponentdescription, produces = { "text/n3" })
	public @ResponseBody String getModelAsN3(HttpServletRequest request) throws IOException {
		this.logHeader(request);
		return getModelAsString("N3");
	}

	/**
	 * request RDF Turtle formatted service description
	 * 
	 * <pre>
	curl --location --request GET 'http://localhost:10008/service-description' \
	--header 'Content-Type: application/json'
	 * </pre>
	 * 
	 * 
	 * @return
	 * @throws IOException
	 */
	@GetMapping(value = QanaryConfiguration.rdfcomponentdescription, produces = { "text/turtle" })
	@Operation(
			summary = "Returns the component description as RDF (required data types, producable data types).", //
			operationId = "getModel", //
			description = "note: change the data format using RDF-compatible headers" //  
	)
	public @ResponseBody String getModelAsTurtle(HttpServletRequest request) throws IOException {
		this.logHeader(request);
		return getModelAsString("TURTLE");
	}

	/**
	 * parameter might have the values: Turtle N-Triples NQuads TriG JSON-LD RDF/XML
	 * RDF/JSON TriX RDF Binary
	 * 
	 * @param returnType
	 * @return
	 */
	private String getModelAsString(String returnType) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		if(this.model != null) {
			model.write(os, returnType);
			logger.info("result for '{}':\n{}", returnType, os.toString());
		} else {
			logger.warn("No component description model available. (model == null).");
		}
		return os.toString();
	}

}
