package eu.wdaqua.qanary.commons;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import eu.wdaqua.qanary.commons.ontology.TextPositionSelector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * represents the access to a question in a triplestore
 *
 * TODO: should be refactored to distinguished class for different
 * representations like text, audio, ...
 */
public class QanaryQuestion<T> {

	private static final Logger logger = LoggerFactory.getLogger(QanaryQuestion.class);

	private QanaryMessage qanaryMessage;
	private QanaryUtils qanaryUtils;
	private T raw;
	// note: uri not final due to different sources for the value (some from
	// triplestore, some pre-set)
	private URI uri;
	private URI uriTextualRepresentation;
	private URI uriAudioRepresentation;
	private final URI namedGraph; // where the question is stored
	private String language;
	private List<String> knowledgeBase;
	private QanaryTripleStoreConnector myQanaryTripleStoreConnector; 

	private QanaryConfigurator myQanaryConfigurator;

  private String FILENAME_SELECT_TRANSLATION_ANNOTATION = "/queries/select_all_AnnotationOfQuestionTranslation.rq";
  private String REQUIRED_TRIPLES = "/queries/insert_required_triples.rq";
	private String FILENAME_SELECT_URI_TEXTUAL_REPRESENTATION = "/queries/select_uri_textual_representation.rq";

	/**
	 * init the graph in the triplestore (c.f., application.properties), a new graph
	 * is constructed
	 * 
	 * @param questionUri
	 * @param qanaryConfigurator
	 * @throws URISyntaxException
	 * @throws SparqlQueryFailed
	 */
	public QanaryQuestion(final URL questionUri, QanaryConfigurator qanaryConfigurator, URI priorConversation)
			throws URISyntaxException, SparqlQueryFailed {
		// Create a new named graph and insert it into the triplestore
		// in this graph the data is stored
		this.namedGraph = new URI("urn:graph:" + UUID.randomUUID().toString());
		this.uri = questionUri.toURI();
		
		if (qanaryConfigurator == null) {
			String message = "qanaryConfigurator was null";
			logger.error(message);
			throw new RuntimeException(message);
		}
		
		this.myQanaryConfigurator = qanaryConfigurator;

		final URI triplestore = qanaryConfigurator.getEndpoint();
		String sparqlquery = "";
		String namedGraphMarker = "<" + namedGraph.toString() + ">";
		String questionUrlString = questionUri.toString();
		logger.info("Triplestore: {}, Current graph: {}", triplestore, namedGraph.toString());

		String addPriorConversation = "";
		if( priorConversation != null && !priorConversation.toASCIIString().trim().isEmpty() ) {
			logger.info("previous graph (qa:priorConversation) provided: |{}|", priorConversation);
			addPriorConversation = "<" + questionUrlString + "> qa:priorConversation <" + priorConversation.toASCIIString() + "> . ";
		} else {
			logger.warn("No previous graph (qa:priorConversation) provided.");
		}
		
		// IMPORTANT: The following processing steps will fail if the used
		// triplestore is not allowed to access data from the Web

		// Load the Open Annotation Ontology
		sparqlquery = "" //
				+ "LOAD <http://www.w3.org/ns/oa.rdf> " //
				+ "INTO GRAPH " + namedGraphMarker;
		logger.info("SPARQL query: {}", sparqlquery);
		// loadTripleStore(sparqlquery, qanaryConfigurator); // TODO: remove 
		qanaryConfigurator.getQanaryTripleStoreConnector().update(sparqlquery);

		// Load the Qanary Ontology using the permanent GitHub location
		// specified in application.properties
		sparqlquery = "" //
				+ "LOAD <"+qanaryConfigurator.getQanaryOntology()+"> " //
				+ "INTO GRAPH " + namedGraphMarker;
		logger.warn("SPARQL query: {}", sparqlquery);
		// loadTripleStore(sparqlquery, qanaryConfigurator); // TODO: remove
		qanaryConfigurator.getQanaryTripleStoreConnector().update(sparqlquery);
		

		// Prepare the question, answer and dataset objects
		sparqlquery = "" //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> \n" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> \n" //
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" //
				+ "INSERT DATA { \n" //
				+ "	GRAPH " + namedGraphMarker + " { \n" //
				+ "		<" + questionUrlString + "> a qa:Question .\n" //
				+ "		<" + questionUrlString + "> owl:sameAs <urn:qanary:currentQuestion> .\n" //
				+ "		<" + questionUrlString + "#Answer> a qa:Answer . \n" //
				+ "		<" + questionUrlString + "#Dataset> a qa:Dataset . \n" //
				+ "		<" + questionUrlString + "#Annotation:1> a  oa:AnnotationOfQuestion; \n" //
				+ "				oa:hasTarget <" + questionUrlString + "> ; \n" //
				+ "				oa:hasBody   <" + questionUrlString + "#Answer> . \n" //
				+ "		<" + questionUrlString + "#Annotation:2> a  oa:AnnotationOfQuestion; \n" //
				+ "				oa:hasTarget <" + questionUrlString + "> ; \n" //
				+ "				oa:hasBody   <" + questionUrlString + "#Dataset> . \n" //
				+ "		" + addPriorConversation + "\n" //
				+ "	} \n" // end: graph
				+ "}";
		logger.info("SPARQL query (initial annotations for question {}):\n{}", questionUrlString, sparqlquery);
		// loadTripleStore(sparqlquery, qanaryConfigurator); // TODO: remove
		qanaryConfigurator.getQanaryTripleStoreConnector().update(sparqlquery);

		loadRequiredTriplesToTriplestore(qanaryConfigurator.getQanaryTripleStoreConnector());

		initFromTriplestore(qanaryConfigurator);
	}

	/**
	 * Function that inserts required triples for graph initialization
	 * @param qanaryTripleStoreConnector
	 */
	private void loadRequiredTriplesToTriplestore(QanaryTripleStoreConnector qanaryTripleStoreConnector) {
		QuerySolutionMap querySolutionMap = new QuerySolutionMap();
		querySolutionMap.add("graph", ResourceFactory.createResource(namedGraph.toASCIIString()));
		try {
			String sparqlQuery = QanaryTripleStoreConnector.readFileFromResourcesWithMap(REQUIRED_TRIPLES, querySolutionMap);
			qanaryTripleStoreConnector.update(sparqlQuery);
		} catch(IOException e) {
			logger.warn("Read query from file failed with message: {}", e.getMessage());
		} catch(SparqlQueryFailed e) {
			logger.warn("Inserting required triples to triplestore failed, skipping.");
		}
	}

	/**
	 * create QanaryQuestion from the information available in the provided graph
	 * (data is retrieved from the Qanary triplestore, see application.properties)
	 * 
	 * @param namedGraph
	 * @param qanaryConfigurator
	 * @throws URISyntaxException
	 */
	public QanaryQuestion(URI namedGraph, QanaryConfigurator qanaryConfigurator) throws URISyntaxException {
		this.initFromTriplestore(qanaryConfigurator);
		this.qanaryMessage = new QanaryMessage(qanaryConfigurator.getEndpoint(), namedGraph);
		this.myQanaryConfigurator = qanaryConfigurator;
		// save where the answer is stored
		this.namedGraph = namedGraph;
	}

	/**
	 * create a QanaryQuestion from a QanaryMessage
	 * 
	 * @param qanaryMessage
	 */
	public QanaryQuestion(QanaryMessage qanaryMessage, final QanaryConfigurator myQanaryConfigurator) {
		this(qanaryMessage, myQanaryConfigurator.getQanaryTripleStoreConnector());
	}

	public QanaryQuestion(QanaryMessage qanaryMessage, final QanaryTripleStoreConnector myQanaryTripleStoreConnector) {
		this.qanaryMessage = qanaryMessage;
		this.myQanaryTripleStoreConnector = myQanaryTripleStoreConnector;
		this.qanaryUtils = new QanaryUtils(qanaryMessage, myQanaryTripleStoreConnector);
		// save where the answer is stored
		this.namedGraph = qanaryMessage.getInGraph();
	}

	/**
	 * safe retrieval of QanaryTripleStoreConnector instance 
	 * 
	 * remark: needed for transition between Qanary versions
	 * 
	 * @return
	 */
	public QanaryTripleStoreConnector getQanaryTripleStoreConnector() {
		if( this.myQanaryConfigurator != null) {
			return this.myQanaryConfigurator.getQanaryTripleStoreConnector();
		} else if(this.myQanaryTripleStoreConnector != null) {
			return this.myQanaryTripleStoreConnector;
		} else {
			String message = "in QanaryQuestion a QanaryTripleStoreConnector instance is required";
			logger.error(message);
			throw new RuntimeException(message);
		}
	}
	
	/**
	 * init object properties from a given triplestore URI
	 *
	 * @throws URISyntaxException
	 */
	private void initFromTriplestore(final QanaryConfigurator myQanaryConfigurator) throws URISyntaxException {
		this.qanaryMessage = new QanaryMessage(myQanaryConfigurator.getEndpoint(), namedGraph);
		this.qanaryUtils = new QanaryUtils(this.qanaryMessage, this.getQanaryTripleStoreConnector());
	}

	/**
	 * returns the QanaryMessage object provided via constructor
	 */
	public QanaryMessage getQanaryMessage() {
		return this.qanaryMessage;
	}

	/**
	 * returns the endpoint provided by the QanaryMessage object provided via
	 * constructor
	 */
	public URI getEndpoint() {
		return this.qanaryMessage.getEndpoint();
	}

	/**
	 * returns the inGraph provided by the QanaryMessage object provided via
	 * constructor
	 */
	public URI getInGraph() {
		return this.qanaryMessage.getInGraph();
	}

	/**
	 * returns the outGraph provided by the QanaryMessage object provided via
	 * constructor
	 */
	public URI getOutGraph() {
		return this.qanaryMessage.getOutGraph();
	}

	/**
	 * graph where the question is stored
	 * 
	 * @return
	 */
	public URI getNamedGraph() {
		return this.namedGraph;
	}

	/**
	 * get original question URI from the pipeline endpoint
	 * 
	 * @throws SparqlQueryFailed
	 */
	public URI getUri() throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {
		if (this.uri == null) {
			// check if a graph is provided
			if (this.getInGraph() == null) {
				throw new QanaryExceptionNoOrMultipleQuestions("inGraph is null.");
			} else {
				ResultSet resultset = this.getQanaryTripleStoreConnector().select("" //
						+ "SELECT ?question " //
						+ "FROM <" + this.getInGraph() + "> {" //
						+ "	?question <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.wdaqua.eu/qa#Question>" //
						+ "}");

				int i = 0;
				String question = null;
				while (resultset.hasNext()) {
					question = resultset.next().get("question").asResource().getURI();
					logger.debug("{}/{}: qa#Question = {}", i++, resultset.getRowNumber()-1, question);
				}
				if (i > 1) {
					throw new QanaryExceptionNoOrMultipleQuestions("More than 1 question (count: " + i + ") in graph "
							+ this.getInGraph() + " at " + this.getEndpoint());
				} else if (i == 0) {
					throw new QanaryExceptionNoOrMultipleQuestions(
							"No question available in graph " + this.getInGraph() + " at " + this.getEndpoint());
				}
				this.uri = new URI(question);
			}
		}
		return this.uri;
	}

	/**
	 * put AnnotationOfTextRepresentation
	 * 
	 * @throws SparqlQueryFailed
	 */
	public void putAnnotationOfTextRepresentation()
			throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {
		String sparqlquery = "" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
				+ "INSERT { " //
				+ "	GRAPH <" + this.getInGraph() + "> { " //
				+ "  		?a a qa:AnnotationOfTextRepresentation . " //
				+ "  		?a oa:hasTarget <" + this.getUri() + "> . " //
				+ "  		?a oa:hasBody <" + this.getUri() + "> . " //
				+ "	 	?a oa:annotatedAt ?time  "//
				+ "	} " //
				+ "} WHERE { " //
				+ "     BIND (IRI(CONCAT(\"urn:qanary:annotation:representation:text:\", STR(RAND()))) AS ?a) ." //
				+ "     BIND (now() as ?time) " //
				+ "}";
		this.getQanaryTripleStoreConnector().update(sparqlquery);
	}

	/**
	 * put AnnotationOfAudioRepresentation
	 * 
	 * @throws SparqlQueryFailed
	 */
	public void putAnnotationOfAudioRepresentation()
			throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {
		String sparqlquery = "" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
				+ "INSERT { " //
				+ "	GRAPH <" + this.getInGraph() + "> { " //
				+ "  		?a a qa:AnnotationOfAudioRepresentation . " //
				+ "  		?a oa:hasTarget <" + this.getUri() + "> . " //
				+ "  		?a oa:hasBody <" + this.getUri() + "> . " //
				+ "	 	?a oa:annotatedAt ?time  "//
				+ "	} " //
				+ "} WHERE { " //
				+ "     BIND (IRI(CONCAT(\"urn:qanary:annotation:representation:audio:\", STR(RAND()))) AS ?a) ." //
				+ "     BIND (now() as ?time) " //
				+ "}";
		this.getQanaryTripleStoreConnector().update(sparqlquery);
	}

	/**
	 * returns the raw data of the question fetched from the URI provided via the
	 * constructor, the result is cached to prevent unnecessary calls to remote
	 * services
	 *
	 * TODO: replace with Spring rest client using the message class from the
	 * QanaryPipeline
	 */
	@SuppressWarnings("unchecked")
	public T getRawData() throws Exception {

		if (this.raw == null) {
			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<String> response = restTemplate.getForEntity(this.getUri(), String.class);
			logger.debug("while calling {} received response: {}", this.getUri(), response.getBody(), response);

			JsonObject body = JSON.parse(response.getBody());

			String uriString = body.get(QanaryConfiguration.questionRawDataProperyName).getAsString().toString().trim();
			if (uriString.startsWith("\"")) {
				uriString = uriString.substring(1);
			}
			if (uriString.endsWith("\"")) {
				uriString = uriString.substring(0, uriString.length() - 1);
			}

			// TODO catch exception here and throw an own one
			logger.debug("{}: <{}>", QanaryConfiguration.questionRawDataProperyName, uriString);
			URI rawUri = new URI(uriString);
			ResponseEntity<String> responseRaw = restTemplate.getForEntity(rawUri, String.class);
			logger.debug("raw data fetched from {} is \"{}\"", rawUri, responseRaw);

			this.raw = (T) responseRaw.getBody();

			if (this.raw == null) {
				logger.warn("fetched raw data from {}, result is null: {}", rawUri, responseRaw.getBody());
			}
		}
		return this.raw;
	}

	/**
	 * get the uri of the textual representation of the question
	 */
	public URI getUriTextualRepresentation() throws Exception {
		if (this.uriTextualRepresentation == null) {
			QuerySolutionMap bindingsForSelect = new QuerySolutionMap();
			bindingsForSelect.add("graph", ResourceFactory.createResource(this.getOutGraph().toASCIIString()));

			String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(FILENAME_SELECT_URI_TEXTUAL_REPRESENTATION, bindingsForSelect);
			logger.info("SPARQL query: {}", sparql);
			ResultSet resultSet = this.qanaryUtils.getQanaryTripleStoreConnector().select(sparql);
			
			int i = 0;
			String uriTextRepresentation = null;
			while (resultSet.hasNext()) {
				uriTextRepresentation = resultSet.next().get("uri").asResource().getURI();
				logger.debug("{}: qa#Question = {}", i++, uriTextRepresentation);
			}
			if (i > 1) {
				throw new Exception("More than 1 text representation (count: " + i + ") in graph " + this.getInGraph()
						+ " at " + this.getEndpoint());
			} else if (i == 0) {
				throw new Exception("No uriTextRepresentation available in graph " + this.getInGraph() + " at "
						+ this.getEndpoint());
			}

			logger.info("uriTextRepresentation {} found in {} at {}", uriTextRepresentation, this.getInGraph(),
					this.getEndpoint());
			this.uriTextualRepresentation = new URI(uriTextRepresentation);
		}
		return this.uriTextualRepresentation;
	}

	/**
	 * get the textual representation of the question
	 */
	public String getTextualRepresentation() throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> responseRaw = restTemplate.getForEntity(
				this.getUriTextualRepresentation() + QanaryConfiguration.questionRawDataUrlSuffix, String.class);
		logger.debug("textRepresentation {} ", responseRaw.getBody());
		return responseRaw.getBody();
	}

	public String getTextualRepresentation(String language) throws Exception {
		// attempt to get original question language, if annotated by LD component
		String questionLanguage = "";
		try {
			questionLanguage = this.getLanguage();
			logger.info("Language of original text representation: {}", questionLanguage);
			if (questionLanguage.equals(language)) {
				// if language alrady matches, return getTextualRepresentation
				return this.getTextualRepresentation(); 
			}
		} catch (Exception e) {
			logger.warn("Language of original text representation is not known!\n{}", e.getMessage());
		} if (language.trim().length() == 2) { 
			// look for annotation of translation with matching language
			return this.getTranslatedTextualRepresentation(language); 
		} else {
			throw new Exception("parameter `language` is invalid: " + language);
		}
	}

	public String getTranslatedTextualRepresentation(String language) throws Exception {
		QuerySolutionMap bindingsForSelect = new QuerySolutionMap();
		bindingsForSelect.add("graph", ResourceFactory.createResource(this.getOutGraph().toASCIIString()));
		bindingsForSelect.add("targetQuestion", ResourceFactory.createResource(this.getUri().toASCIIString()));
		bindingsForSelect.add("language", ResourceFactory.createStringLiteral(language));

		String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(FILENAME_SELECT_TRANSLATION_ANNOTATION, bindingsForSelect);
		logger.info("SPARQL query: {}", sparql);
		ResultSet resultSet = this.qanaryUtils.getQanaryTripleStoreConnector().select(sparql);

		while (resultSet.hasNext()) {
			QuerySolution result = resultSet.next();
			String translatedQuestionString = result.get("hasBody").asLiteral().getString();
			// take the first best result
			return translatedQuestionString;
		}
		// if nothing was found
		throw new Exception("No uriTextRepresentation available in graph " + this.getInGraph() 
				+ " for language " + language + " at " + this.getEndpoint());
	}

	/**
	 * get the uri of the audio representation of the question
	 */
	public URI getUriAudioRepresentation() throws Exception {
		if (this.uriAudioRepresentation == null) {
			String sparql = "" //
					+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "SELECT ?uri " //
					+ "FROM <" + this.getInGraph() + "> " //
					+ "WHERE { " //
					+ "  ?a a qa:AnnotationOfAudioRepresentation . " //
					+ "  ?a oa:hasBody ?uri . " //
					+ "}";

			ResultSet resultset = this.getQanaryTripleStoreConnector().select(sparql);

			int i = 0;
			String uriAudioRepresentation = null;
			while (resultset.hasNext()) {
				uriAudioRepresentation = resultset.next().get("uri").asResource().getURI();
				logger.debug("{}: qa#Question = {}", i++, uriAudioRepresentation);
			}
			if (i > 1) {
				throw new Exception("More than 1 text representation (count: " + i + ") in graph " + this.getInGraph()
						+ " at " + this.getEndpoint());
			} else if (i == 0) {
				throw new Exception("No uriAudioRepresentation available in graph " + this.getInGraph() + " at "
						+ this.getEndpoint());
			}

			logger.info("uriAudioRepresentation {} found in {} at {}", uriAudioRepresentation, this.getInGraph(),
					this.getEndpoint());
			this.uriAudioRepresentation = new URI(uriAudioRepresentation);
		}
		return this.uriAudioRepresentation;
	}

	/**
	 * get the audio representation of the question
	 */
	public byte[] getAudioRepresentation() throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<byte[]> responseRaw = restTemplate.getForEntity(
				this.getUriAudioRepresentation() + QanaryConfiguration.questionRawDataUrlSuffix, byte[].class);
		logger.info("Audio Representation retrieved");
		return responseRaw.getBody();
	}

	/**
	 * adds oa:TextPositionSelectors to triplestore
	 *
	 * if scores are available, then they are also saved to the triplestore
	 *
	 * if resource URIs are available, then they are also saved to the triplestore
	 *
	 * TODO: move this to a SPARQL builder
	 */
	public void addAnnotations(Collection<TextPositionSelector> selectors) throws Exception {
		String sparql;
		String resourceSparql;
		String scoreSparql;

		for (TextPositionSelector s : selectors) {

			// score might not be available
			if (s.getScore() != null) {
				scoreSparql = "     oa:score \"" + s.getScore() + "\"^^xsd:double ;";
			} else {
				scoreSparql = "";
			}

			// resource might not be available
			if (s.getResourceUri() != null) {
				resourceSparql = "     oa:hasBody <" + s.getResourceUri() + "> ;";
			} else {
				resourceSparql = "";
			}

			sparql = "" //
					+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "INSERT { " //
					+ "GRAPH <" + this.getOutGraph() + "> { " //
					+ "  ?a a qa:AnnotationOfInstance . " //
					+ "  ?a oa:hasTarget [ " //
					+ "                a oa:SpecificResource; " //
					+ "                oa:hasSource    <" + this.getUriTextualRepresentation() + ">; " //
					+ "                oa:hasSelector  [ " //
					+ "                    a oa:TextPositionSelector ; " //
					+ "                    oa:start \"" + s.getStart() + "\"^^xsd:nonNegativeInteger ; " //
					+ "                    oa:end  \"" + s.getEnd() + "\"^^xsd:nonNegativeInteger  " //
					+ "           ] " //
					+ "  ] ; " //
					+ resourceSparql //
					+ scoreSparql //
					+ "     oa:annotatedBy <" + qanaryUtils.getComponentUri() + "> ; " //
					+ "	    oa:annotatedAt ?time . " //
					+ "}} WHERE { " //
					+ "     BIND (IRI(CONCAT(\"urn:qanary:annotation:instance:\", STR(RAND()))) AS ?a) ." //
					+ "     BIND (now() as ?time) " //
					+ "}";

			this.getQanaryTripleStoreConnector().update(sparql);
		}
	}

	public List<SparqlAnnotation> getSparqlResults() throws SparqlQueryFailed {
		String sparql = "" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> \n" //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> \n" // 
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" // 
				+ "SELECT (?sparqlquery AS ?sparql) ?confidence ?knowledgegraphEndpoint (?time1 AS ?time) \n" //
				+ "FROM <" + this.getInGraph() + "> \n" //
				+ "WHERE { \n" //
				+ "  ?a a qa:AnnotationOfAnswerSPARQL . \n" //
				+ "  OPTIONAL { \n" //
				+ "			?a oa:hasBody ?s . \n" //
				+ "			?s rdf:type qa:SparqlQuery . \n" // 
				+ "			?s rdf:value ?sparqlquery . \n" //
				+ "  } \n" //
				+ "  OPTIONAL { ?a qa:score ?confidence . } \n" //
				+ "  OPTIONAL { ?a qa:overKnowledgeGraph ?kb . } \n" //
				+ "  OPTIONAL { \n" //
				+ "	?a oa:annotatedAt ?time1 . \n" //
				+ "  	{ \n" //
				+ "  	SELECT ?time1 { \n" //
				+ "  		?a a qa:AnnotationOfAnswerSPARQL . \n" //
				+ "  		?a oa:annotatedAt ?time1 . \n" //
				+ "  	} \n" //
				+ "	ORDER BY DESC(?time1) \n" //
				+ "	LIMIT 1 \n" //
				+ "  	} \n" //
				+ "  } \n" //
				+ "} \n" //
				+ "ORDER BY DESC(?score) \n";
		logger.debug("getSparqlResults: SELECT using\n{}", sparql);
		ResultSet resultset = this.getQanaryTripleStoreConnector().select(sparql);
		
		List<SparqlAnnotation> annotationList = new ArrayList<SparqlAnnotation>();
		while (resultset.hasNext()) {
			QuerySolution next = resultset.next();
			SparqlAnnotation sparqlAnnotation = new SparqlAnnotation();
			sparqlAnnotation.query = next.get("sparql").asLiteral().toString();
			if (next.get("confidence") != null) {
				sparqlAnnotation.confidence = next.get("confidence").asLiteral().toString();
			}
			if (next.get("knowledgegraphEndpoint") != null) {
				sparqlAnnotation.knowledgegraphEndpoint = next.get("knowledgegraphEndpoint").asLiteral().toString();
			}
			annotationList.add(sparqlAnnotation);
		}
		return annotationList;
	}

	public class SparqlAnnotation {
		public String query;
		public String confidence;
		public String knowledgegraphEndpoint;
	}

	public String getSparqlResult() throws SparqlQueryFailed {
		// added try and catch to prevent indexOutOfBoundsException and NullPointerException, returns empty String if no Query was found
		String sparqlResult = "";
		try {
			sparqlResult = this.getSparqlResults().get(0).query;
		} catch (IndexOutOfBoundsException | NullPointerException e) {
			logger.warn("No SPARQL Query found, index out of bounds");
		}
		return sparqlResult;
	}

	public String getJsonResult() throws SparqlQueryFailed {
		String sparql = "" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " //
				+ "SELECT ?json " //
				+ "FROM <" + this.getInGraph() + "> " //
				+ "WHERE { " //
				+ "	?a a qa:AnnotationOfAnswerJson . " //
				+ "  	?a oa:hasBody ?answer . " //
				+ " 	?answer rdf:value ?json . " //
				+ "}";
		logger.debug("getJsonResult: SELECT using:\n{}", sparql);
		ResultSet resultset = this.getQanaryTripleStoreConnector().select(sparql);

		// OLD: the default value has to be null to distinguish missing values from empty values
		// NEW: returning null would result in "null" being part of the response JSON, 
		//		return an empty string instead.
		//		This way, a distinction is still possible:
		//		missing values: ""
		//		empty values: []
		String sparqlAnnotation = null; 
		while (resultset.hasNext()) {
			sparqlAnnotation = resultset.next().get("json").asLiteral().toString();
		}
		
		if (sparqlAnnotation == null) {
			return "";
		} else {
			return sparqlAnnotation.replace("\\\"", "\"");
		}
	}

	public void putTextRepresentation(String text) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("question", text);
		String r = restTemplate.postForObject(qanaryUtils.getHostUri() + "/question", map, String.class);
		logger.info("DEBUG {}", r);
		JSONObject obj = new JSONObject(r);
		String uriTextRepresention = obj.get("questionURI").toString();
		logger.info("Text representation: {}", uriTextRepresention);
		logger.info("store data in graph {}", this.qanaryMessage.getEndpoint());
		String sparql = "" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
				+ "INSERT { " + "GRAPH <" + this.getOutGraph() + "> { " //
				+ " 	?a a qa:AnnotationOfTextualRepresentation . " //
				+ " 	?a oa:hasTarget <" + this.getUri() + "> . " //
				+ " 	?a oa:hasBody <" + uriTextRepresention + "> ;" //
				+ "        oa:annotatedBy <" + qanaryUtils.getComponentUri() + "> ; " //
				+ "	   oa:AnnotatedAt ?time  " //
				+ "}} " //
				+ "WHERE { " //
				+ "	BIND (IRI(CONCAT(\"urn:qanary:annotation:representation:text:\", STR(RAND()))) AS ?a) ." //
				+ "	BIND (now() as ?time) " //
				+ "}";
		logger.info("SPARQL query: {}", sparql);
		this.getQanaryTripleStoreConnector().update(sparql);
	}

	/**
	 * set a new language for the current question, stored in the Qanary triplestore
	 *
	 * @param language
	 */
	public void setLanguageText(String language) throws Exception {
		String part = "";
		if (language != null && !language.isEmpty()) {
			part = "?a oa:hasBody \"" + language + "\" . ";
		}
		String sparql = "" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "INSERT { " //
				+ "	GRAPH <" + this.getOutGraph() + "> { " //
				+ "		?a a qa:AnnotationOfQuestionLanguage . " //
				+ part //
				+ "		?a 	oa:hasTarget <" + this.getUri() + "> ; " //
				+ "   			oa:annotatedBy <www.wdaqua.eu/qanary> ; " //
				+ "   			oa:annotatedAt ?time  " //
				+ " } " //
				+ "} " //
				+ "WHERE { " //
				+ "	BIND (IRI(CONCAT(\"urn:qanary:annotation:questionlanguage:\", STR(RAND()))) AS ?a) . " //
				+ "	BIND (now() as ?time) . " //
				+ "}";
		this.getQanaryTripleStoreConnector().update(sparql);
	}

	/**
	 * get the language of the question, enabled for feedback
	 */
	public String getLanguage() throws Exception {
		if (this.language == null) {
			String sparql = "" //
					+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "SELECT ?uri " //
					+ "FROM <" + this.getInGraph() + "> " //
					+ "WHERE { " //
					+ "  ?a a qa:AnnotationOfQuestionLanguage . " //
					+ "  OPTIONAL { ?a oa:hasBody ?uri . } " // look for a bug in stardog
					+ "  ?a oa:annotatedAt ?time . " //
					+ "  { " //
					+ "    SELECT ?time { " //
					+ "     ?a a qa:AnnotationOfQuestionLanguage . " //
					+ "     ?a oa:annotatedAt ?time . " //
					+ "    } ORDER BY ?time LIMIT 1 " //
					+ "  } " //
					+ "}";

			ResultSet resultset = this.getQanaryTripleStoreConnector().select(sparql);
					
			int i = 0;
			List<String> languageArray = new ArrayList<>();
			String language = "";
			while (resultset.hasNext()) {
				languageArray.add(resultset.next().get("uri").toString());
				i++;
			}
			if (i == 1) {
				language = languageArray.get(0);
			}
			else if (i > 1) {
				throw new Exception("More than 1 language (count: " + i + ") in graph " + this.getInGraph() + " at "
						+ this.getEndpoint());
			} else if (i == 0) {
				throw new Exception(
						"No language available in graph " + this.getInGraph() + " at " + this.getEndpoint());
			}

			logger.info("language {} found in {} at {}", language, this.getInGraph(), this.getEndpoint());
			this.language = language;
		}
		return this.language;
	}

	/**
	 * set a new knowledge-base for the current question, stored in the Qanary
	 * triplestore
	 *
	 * @param targetData
	 */
	public void setTargetData(List<String> targetData) throws Exception {
		String part = "";
		for (int i = 0; i < targetData.size(); i++) {
			part += "?a oa:hasBody \"" + targetData.get(i) + "\" . ";
		}
		String sparql = "" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "INSERT { " //
				+ "GRAPH <" + this.getOutGraph() + "> { " //
				+ "?a a qa:AnnotationDataset . " //
				+ part //
				+ "?a oa:hasTarget <" + this.getUri() + "> ; " //
				+ "   oa:annotatedBy <www.wdaqua.eu/qa> ; " //
				+ "   oa:annotatedAt ?time ; " //
				+ " }" //
				+ "} " //
				+ "WHERE { " //
				+ "	BIND (IRI(CONCAT(\"urn:qanary:annotation:dataset:\", STR(RAND()))) AS ?a) . " //
				+ "	BIND (now() as ?time) . " //
				+ "}";
		this.getQanaryTripleStoreConnector().update(sparql);
	}

	/**
	 * get the knowledge base of the question, enabled for feedback
	 */
	public List<String> getTargetData() throws Exception {
		if (this.knowledgeBase == null) {
			String sparql = "" //
					+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "SELECT ?uri " //
					+ "FROM <" + this.getInGraph() + "> " //
					+ "WHERE { " //
					+ "  ?a a qa:AnnotationDataset . " //
					+ "  OPTIONAL {?a oa:hasBody ?uri . } " // look for a bug in Stardog
					+ "  ?a oa:annotatedAt ?time . " //
					+ "  { " //
					+ "    SELECT ?time { " //
					+ "     ?a a qa:AnnotationDataset . " //
					+ "     ?a oa:annotatedAt ?time . " //
					+ "    } ORDER BY ?time LIMIT 1 " //
					+ "  } " //
					+ "}";
			ResultSet resultset = this.getQanaryTripleStoreConnector().select(sparql);

			int i = 0;
			List<String> knowledgeBase = new ArrayList<String>();
			while (resultset.hasNext()) {
				knowledgeBase.add(resultset.next().get("uri").toString());
				i++;
			}
			if (i == 0) {
				throw new Exception(
						"No knwoledgebase available in graph " + this.getInGraph() + " at " + this.getEndpoint());
			}

			logger.info("knowledge base {} found in {} at {}", knowledgeBase, this.getInGraph(), this.getEndpoint());
			this.knowledgeBase = knowledgeBase;
		}
		return this.knowledgeBase;
	}

	public String getAnswerFound() throws SparqlQueryFailed {
		String sparql = "" //
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
				+ "SELECT ?found " //
				+ "FROM <" + this.getInGraph() + "> " //
				+ "WHERE { " //
				+ "  <URIAnswer> qa:found ?found .  " //
				+ "}";
		ResultSet resultset = this.getQanaryTripleStoreConnector().select(sparql);

		String found = "undefined";
		while (resultset.hasNext()) {
			found = resultset.next().get("found").toString();
		}
		return found;
	}

}
