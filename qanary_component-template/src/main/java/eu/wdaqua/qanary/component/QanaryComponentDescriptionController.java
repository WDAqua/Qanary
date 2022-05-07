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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.wdaqua.qanary.commons.config.QanaryConfiguration;

/**
 * provides access to the components definition in different RDF formats
 *
 */
@Controller
public class QanaryComponentDescriptionController {

	private static final Logger logger = LoggerFactory.getLogger(QanaryComponentDescriptionController.class);
	private static final String componentTurtleDescriptionFileLocation = "component-description.ttl";

	private ApplicationContext context;

	public Model model;

	@Autowired
	public QanaryComponentDescriptionController(ApplicationContext context) throws FileNotFoundException {
		this.context = context;
		initQanaryServiceDecription();
	}

	private void initQanaryServiceDecription() throws FileNotFoundException {
		String filename;

		try {
			try {
				// standard location
				Resource file = context.getResource("classpath:" + componentTurtleDescriptionFileLocation);
				filename = file.getURI().toASCIIString();
			} catch (Exception e) {
				// fallback for some development scenarios
				File x = new File("src/main/resources/" + componentTurtleDescriptionFileLocation);
				filename = x.getAbsolutePath();
			}

			if (filename != null) {
				logger.info("location of {}: {}", componentTurtleDescriptionFileLocation, filename);
				try {
					if (model == null) {
						model = RDFDataMgr.loadModel(filename);
						logger.info("size of loaded model: {} triples as TURTLE\n", model.size(),
								getModelAsString("TURTLE"));
						// logger.info("size of loaded model: {} triples as RDF/JSON\n",
						// model.size(),getModelAsString("RDF/JSON"));
						// logger.info("size of loaded model: {} triples as RDF/XML\n", model.size(),
						// getModelAsString("RDF/XML"));
					}
				} catch (Exception e) {
					logger.warn("could not find a Turtle service description at '{}'", filename);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * provides a description HTML page of the component, replace description.html
	 * to custom page
	 */
	@GetMapping(value = QanaryConfiguration.description)
	// TODO: OpenAPI definitions
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
	// TODO: OpenAPI definitions
	public @ResponseBody String getModelAsJson(HttpServletRequest request) {
		this.logHeader(request);
		return getModelAsString("RDF/JSON");
	}

	@GetMapping(value = QanaryConfiguration.rdfcomponentdescription, produces = { "application/xml",
			"application/rdf+xml" })
	// TODO: OpenAPI definitions
	public @ResponseBody String getModelAsXml(HttpServletRequest request) {
		this.logHeader(request);
		return getModelAsString("RDF/XML");
	}

	@GetMapping(value = QanaryConfiguration.rdfcomponentdescription, produces = { "application/n-triples" })
	// TODO: OpenAPI definitions
	public @ResponseBody String getModelAsNTriples(HttpServletRequest request) throws IOException {
		this.logHeader(request);
		return getModelAsString("N-TRIPLE");
	}

	@GetMapping(value = QanaryConfiguration.rdfcomponentdescription, produces = { "text/n3" })
	// TODO: OpenAPI definitions
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
	// TODO: OpenAPI definitions
	@GetMapping(value = QanaryConfiguration.rdfcomponentdescription, produces = { "text/turtle" })
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
		model.write(os, returnType);
		logger.info("result for '{}':\n{}", returnType, os.toString());
		return os.toString();
	}

}
