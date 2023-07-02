package eu.wdaqua.qanary.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import eu.wdaqua.qanary.QanaryComponentRegistrationChangeNotifier;
import eu.wdaqua.qanary.business.QanaryComponent;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import io.swagger.v3.oas.annotations.Operation;
import net.sf.json.JSONObject;

/**
 * a controller for accessing Qanary component information regarding their pre
 * and post conditions as RDF
 */
@CrossOrigin
@RestController
public class QanaryComponentsManagementController {
	private static final Logger logger = LoggerFactory.getLogger(QanaryComponentsManagementController.class);
	private final QanaryComponentRegistrationChangeNotifier myQanaryComponentRegistrationChangeNotifier;
	private final String rdfservicedescription = "component-description"; // TODO: remove duplicate to service name
	private final RestTemplateWithCaching myRestTemplateWithCaching;
	private final QanaryTripleStoreConnector myQanaryTripleStoreConnector;
	private final static String QUERYGETBASETRIPLESOFCOMPONENTINGRAPH = "/queries/select_base_triples_of_component_annotations_in_a_graph.rq"; 

	public QanaryComponentsManagementController(
			QanaryComponentRegistrationChangeNotifier myQanaryComponentRegistrationChangeNotifier,
			RestTemplateWithCaching myRestTemplateWithCaching,
			QanaryTripleStoreConnector myQanaryTripleStoreConnector
			) {
		this.myQanaryComponentRegistrationChangeNotifier = myQanaryComponentRegistrationChangeNotifier;
		this.myRestTemplateWithCaching = myRestTemplateWithCaching;
		this.myQanaryTripleStoreConnector = myQanaryTripleStoreConnector;
		
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(QUERYGETBASETRIPLESOFCOMPONENTINGRAPH);
	}

	/**
	 * proxy method for providing access to component's descriptions without the
	 * client requiring access to the component's location
	 * 
	 * example:
	 * 
	 * <pre>
	 curl --location --request GET 'http://localhost:8080/components/NED-DBpediaSpotlight' \
	      --header 'Accept: application/ld+json'
	 * </pre>
	 * 
	 * @param request
	 * @param componentName
	 * @return
	 */
	@GetMapping(value = "/components/{componentName}", produces = { "application/x-javascript", "application/json",
			"application/ld+json", "application/xml", "application/rdf+xml", "application/n-triples", "text/n3",
			"text/turtle" })
	@Operation(summary = "get the description of a registered Qanary component as RDF data (use header to define the format, default: JSON-LD)", //
			operationId = "getServiceDescriptionOfComponent", //
			description = "Returns set of RDF triples (presented in defined format), these triples describe the required inputs (data types) and expectedly produced outputs of the requested component. If the component is not available, then the request will result in an HTTP code 404 (not found).")
	public ResponseEntity<String> getServiceDescriptionOfComponentAsJsonLD(HttpServletRequest request,
			@PathVariable String componentName) {
		String acceptHeader = request.getHeader("Accept");
		logger.info("forward using the Accept header: {}", acceptHeader);
		return getServiceDescriptionOfComponent(componentName, acceptHeader);
	}

	/**
	 * fetches the component descriptions
	 * 
	 * @param componentName
	 * @param format
	 * @return
	 */
	public ResponseEntity<String> getServiceDescriptionOfComponent(String componentName, String format) {
		JSONObject response = new JSONObject();
		response.put("name", componentName);

		Map<String, Instance> availableComponents = myQanaryComponentRegistrationChangeNotifier
				.getAvailableComponents();
		if (availableComponents.containsKey(componentName)) {
			QanaryComponent component = myQanaryComponentRegistrationChangeNotifier
					.getAvailableComponentFromName(componentName);
			String endpoint = component.getUrl() + rdfservicedescription;
			logger.info("endpoint asked for {}: {}", format, endpoint);

			// fetch information from component
			HttpHeaders headers = new HttpHeaders();
			headers.set("Accept", format);
			HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
			// TODO: might be configured for caching
			ResponseEntity<String> serviceResponse = myRestTemplateWithCaching.exchange(endpoint, HttpMethod.GET,
					requestEntity, String.class);
			logger.info("response body: ", serviceResponse.getBody());
			return serviceResponse;

		} else {
			logger.warn("componentName: {}, isAvailable={} ", componentName, false);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
	
	@GetMapping(value = "/components/{graph}/{componentName}", produces = { "application/x-javascript", "application/json",
			"application/ld+json" })
	@Operation(summary = "Provides all annotations in a graph filter by a component name (name might be *)", //
	operationId = "getBaseTriplesOfComponentInGraphAsJsonLD", //
	description = "example: http://localhost:8080/components/urn:graph:9898e7e8-5fb6-4744-a69f-9997341ee89d/BirthDataQueryBuilderWikidata or http://localhost:8080/components/urn:graph:9898e7e8-5fb6-4744-a69f-9997341ee89d/*")
	public String getBaseTriplesOfComponentInGraphAsJsonLD(HttpServletRequest request, @PathVariable URI graph,
	@PathVariable String componentName) throws IOException, SparqlQueryFailed {
		
		logger.info("request for annotations of {} in graph {}.", componentName, graph);
		
		QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource(graph.toASCIIString()));
        bindings.add("componentName", ResourceFactory.createPlainLiteral(".*:" + componentName + "$"));
 
		String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(QUERYGETBASETRIPLESOFCOMPONENTINGRAPH, bindings);
        ResultSet results = myQanaryTripleStoreConnector.select(sparql);

        // write to a ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ResultSetFormatter.outputAsJSON(outputStream, results);

        // and turn that into a String
        String json = new String(outputStream.toByteArray());        

		return json;
	}
}
