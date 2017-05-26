package eu.wdaqua.qanary.commons;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import eu.wdaqua.qanary.commons.ontology.TextPositionSelector;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.query.ResultSet;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static eu.wdaqua.qanary.commons.QanaryUtils.loadTripleStore;

/**
 * represents the access to a question in a triplestore
 *
 * TODO: should be refactored to disctinguished class for different
 * representations like text, audio, ...
 *
 * @param <T>
 */
public class QanaryQuestion<T> {

	private static final Logger logger = LoggerFactory.getLogger(QanaryQuestion.class);

	private QanaryMessage qanaryMessage;
	private QanaryUtils qanaryUtil;
	private T raw;
	// note: uri not final due to different sources for the value (some from
	// triplestore, some pre-set)
	private URI uri;
	private URI uriTextualRepresentation;
	private String textualRepresentation;
	private URI uriAudioRepresentation;
	private byte[] audioRepresentation;
	private final URI namedGraph; // where the question is stored

	/**
	 * init the graph in the triplestore (c.f., application.properties) a new
	 * graph is constructed
	 * 
	 * @param questionUri
	 * @param qanaryConfigurator
	 * @throws URISyntaxException
	 */
	public QanaryQuestion(final URL questionUri, QanaryConfigurator qanaryConfigurator) throws URISyntaxException {
		// Create a new named graph and insert it into the triplestore
		// in this graph the data is stored
		this.namedGraph = new URI("urn:graph:" + UUID.randomUUID().toString());
		this.uri = questionUri.toURI();

		final URI triplestore = qanaryConfigurator.getEndpoint();
		logger.info("Triplestore " + triplestore);
		String sparqlquery = "";
		String namedGraphMarker = "<" + namedGraph.toString() + ">";

		// Load the Open Annotation Ontology
		sparqlquery = "LOAD <http://localhost:" + qanaryConfigurator.getPort() + "/oa.owl> INTO GRAPH "
				+ namedGraphMarker;
		logger.info("Sparql query " + sparqlquery);
		loadTripleStore(sparqlquery, triplestore);

		// Load the Qanary Ontology
		sparqlquery = "LOAD <http://localhost:" + qanaryConfigurator.getPort() + "/qanaryOntology.ttl> INTO GRAPH "
				+ namedGraphMarker;
		logger.info("Sparql query " + sparqlquery);
		loadTripleStore(sparqlquery, triplestore);

		// Prepare the question, answer and dataset objects
		sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "INSERT DATA {GRAPH " + namedGraphMarker + " { <" + questionUri.toString() + "> a qa:Question}}";
		logger.info("Sparql query " + sparqlquery);
		loadTripleStore(sparqlquery, triplestore);

		sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#>" //
				+ "INSERT DATA {GRAPH " + namedGraphMarker + " { " //
				+ "<http://localhost/Answer> a qa:Answer}}";
		logger.info("Sparql query " + sparqlquery);
		loadTripleStore(sparqlquery, triplestore);

		sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#>" //
				+ "INSERT DATA {GRAPH " + namedGraphMarker + " { " //
				+ "  <http://localhost/Dataset> a qa:Dataset} " //
				+ "}";
		logger.info("Sparql query " + sparqlquery);
		loadTripleStore(sparqlquery, triplestore);

		// Make the first two annotations
		sparqlquery = "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "INSERT DATA { " + "GRAPH " + namedGraphMarker + " { " //
				+ "<anno1> a  oa:AnnotationOfQuestion; " //
				+ "   oa:hasTarget <" + questionUri.toString() + "> ;" //
				+ "   oa:hasBody   <URIAnswer>   . " //
				+ "<anno2> a  oa:AnnotationOfQuestion; " //
				+ "   oa:hasTarget <" + questionUri.toString() + "> ; " //
				+ "   oa:hasBody   <URIDataset> " + "}}";
		logger.info("Sparql query " + sparqlquery);
		loadTripleStore(sparqlquery, triplestore);
		initFromTriplestore(triplestore);
	}

	/**
	 * create QanaryQuestion from the information available in the provided
	 * graph (data is retrieved from the Qanary triplestore, see
	 * application.properties)
	 * 
	 * @param namedGraph
	 * @param qanaryConfigurator
	 * @throws URISyntaxException
	 */
	public QanaryQuestion(URI namedGraph, QanaryConfigurator qanaryConfigurator) throws URISyntaxException {
		this.initFromTriplestore(qanaryConfigurator.getEndpoint());
		this.qanaryMessage = new QanaryMessage(qanaryConfigurator.getEndpoint(), namedGraph);
		// save where the answer is stored
		this.namedGraph = namedGraph;
	}

	/**
	 * create a QanaryQuestion from a QanaryMessage
	 * 
	 * @param qanaryMessage
	 */
	public QanaryQuestion(QanaryMessage qanaryMessage) {
		this.qanaryMessage = qanaryMessage;
		this.qanaryUtil = new QanaryUtils(qanaryMessage);
		// save where the answer is stored
		this.namedGraph = qanaryMessage.getInGraph();
	}

	/**
	 * init object properties from a given triplestore URI
	 * 
	 * @param triplestore
	 * @throws URISyntaxException
	 */
	private void initFromTriplestore(final URI triplestore) throws URISyntaxException {
		this.qanaryMessage = new QanaryMessage(triplestore, namedGraph);
		this.qanaryUtil = new QanaryUtils(this.qanaryMessage);
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
	 */
	public URI getUri() throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException {
		if (this.uri == null) {
			// check if a graph is provided
			if (this.getInGraph() == null) {
				throw new QanaryExceptionNoOrMultipleQuestions("inGraph is null.");
			} else {
				ResultSet resultset = qanaryUtil.selectFromTripleStore(
						"SELECT ?question FROM <" + this.getInGraph()
								+ "> {?question <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.wdaqua.eu/qa#Question>}",
						this.getEndpoint().toString());

				int i = 0;
				String question = null;
				while (resultset.hasNext()) {
					question = resultset.next().get("question").asResource().getURI();
					logger.debug("{}: qa#Question = {}", i++, question);
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
	 */
	public void putAnnotationOfTextRepresentation() throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException {
		String sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
				+ "INSERT { " //
				+ "	GRAPH <" + this.getInGraph() + "> { " //
				+ "  ?a a qa:AnnotationOfTextRepresentation . " //
				+ "  ?a oa:hasTarget <" + this.getUri() + "> . " //
				+ "  ?a oa:hasBody <" + this.getUri() + "> . " //
				+ "	 ?a oa:annotatedAt ?time  "//
				+ "	} " //
				+ "} WHERE { " //
				+ "     BIND (IRI(str(RAND())) AS ?a) ." //
				+ "     BIND (now() as ?time) " //
				+ "}";
		this.qanaryUtil.updateTripleStore(sparqlquery);
	}

	/**
	 * put AnnotationOfAudioRepresentation
	 */
	public void putAnnotationOfAudioRepresentation() throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException {
		String sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
				+ "INSERT { " //
				+ "	GRAPH <" + this.getInGraph() + "> { " //
				+ "  ?a a qa:AnnotationOfAudioRepresentation . " //
				+ "  ?a oa:hasTarget <" + this.getUri() + "> . " //
				+ "  ?a oa:hasBody <" + this.getUri() + "> . " //
				+ "	 ?a oa:annotatedAt ?time  "//
				+ "	} " //
				+ "} WHERE { " //
				+ "     BIND (IRI(str(RAND())) AS ?a) ." //
				+ "     BIND (now() as ?time) " //
				+ "}";
		this.qanaryUtil.updateTripleStore(sparqlquery);
	}

	/**
	 * returns the raw data of the question fetched from the URI provided via
	 * the constructor, the result is cached to prevent unnecessary calls to
	 * remote services
	 *
	 * TODO: replace with Spring rest client using the message class from the
	 * QanaryPipeline
	 */
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
			String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "SELECT ?uri " //
					+ "FROM <" + this.getInGraph() + "> { " //
					+ "  ?a a qa:AnnotationOfTextRepresentation . " //
					+ "  ?a oa:hasBody ?uri " //
					+ "}"; //

			ResultSet resultset = qanaryUtil.selectFromTripleStore(sparql, this.getEndpoint().toString());

			int i = 0;
			String uriTextRepresentation = null;
			while (resultset.hasNext()) {
				uriTextRepresentation = resultset.next().get("uri").asResource().getURI();
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
		logger.info("textRepresentation {} ", responseRaw.getBody());
		return responseRaw.getBody();
	}

	/**
	 * get the uri of the audio representation of the question
	 */
	public URI getUriAudioRepresentation() throws Exception {
		if (this.uriAudioRepresentation == null) {
			String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "SELECT ?uri " //
					+ "FROM <" + this.getInGraph() + "> " //
					+ "WHERE { " //
					+ "  ?a a qa:AnnotationOfAudioRepresentation . " //
					+ "  ?a oa:hasBody ?uri . " //
					+ "}";

			ResultSet resultset = qanaryUtil.selectFromTripleStore(sparql, this.getEndpoint().toString());

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
		logger.info("Audio Representation retrived");
		return responseRaw.getBody();
	}

	/**
	 * adds oa:TextPositionSelectors to triplestore
	 *
	 * if scores are available, then they are also saved to the triplestore
	 *
	 * if resource URIs are available, then they are also saved to the
	 * triplestore
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

			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
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
					+ "     oa:annotatedBy <" + qanaryUtil.getComponentUri() + "> ; " //
					+ "	    oa:annotatedAt ?time  " //
					+ "}} WHERE { " //
					+ "     BIND (IRI(str(RAND())) AS ?a) ." //
					+ "     BIND (now() as ?time) " //
					+ "}";

			qanaryUtil.updateTripleStore(sparql);
		}
	}



	public List<String> getSparqlResults() {
		String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "SELECT ?sparql "
				+ "FROM <" + this.getInGraph() + "> "
				+ "WHERE { "
				+ "  ?a a qa:AnnotationOfAnswerSPARQL . "
				+ "  OPTIONAL {?a oa:hasBody ?sparql . } "
				+ "  OPTIONAL {?a qa:hasScore ?score . } "
				+ "  ?a oa:annotatedAt ?time1 . "
				+ "  { "
				+ "   select ?time1 { "
				+ "    ?a a qa:AnnotationOfAnswerSPARQL . "
				+ "    ?a oa:annotatedAt ?time1 "
				+ "    } order by DESC(?time1) limit 1 "
				+ "  } "
				+ "} "
				+ "ORDER BY DESC(?score)";
		ResultSet resultset = qanaryUtil.selectFromTripleStore(sparql, this.getEndpoint().toString());

		int i = 0;
		List<String> sparqlAnnotation = new ArrayList<String>();
		while (resultset.hasNext()) {
			sparqlAnnotation.add(resultset.next().get("sparql").asLiteral().toString());
		}
		return sparqlAnnotation;
	}

	public String getSparqlResult() {
		return this.getSparqlResults().get(0);
	}

	public String getJsonResult() {
		String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "SELECT ?json "
				+ "FROM <" + this.getInGraph() + "> "
				+ "WHERE { "
				+ "  ?a a qa:AnnotationOfAnswerJSON . "
				+ "  ?a oa:hasBody ?json "
				+ "}";
		ResultSet resultset = qanaryUtil.selectFromTripleStore(sparql, this.getEndpoint().toString());

		int i = 0;
		String sparqlAnnotation = null;
		while (resultset.hasNext()) {
			sparqlAnnotation = resultset.next().get("json").asLiteral().toString();
		}
		return sparqlAnnotation.replace("\\\"","\"");
	}

	public void putTextRepresentation(String text) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("question", text);
		String r = restTemplate.postForObject(qanaryUtil.getHostUri() + "/question", map, String.class);
		logger.info("DEBUG {}", r);
		JSONObject obj = new JSONObject(r);
		String uriTextRepresention = obj.get("questionURI").toString();
		logger.info("Text representation: {}", uriTextRepresention);
		logger.info("store data in graph {}", this.qanaryMessage.getEndpoint());
		String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
				+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "INSERT { " + "GRAPH <" + this.getOutGraph() + "> { "
				+ "  ?a a qa:AnnotationOfTextualRepresentation . "
				+ "  ?a oa:hasTarget <" + this.getUri() + "> . "
				+ "  ?a oa:hasBody <" + uriTextRepresention + "> ;"
				+ "     oa:annotatedBy <" + qanaryUtil.getComponentUri() + "> ; "
				+ "	    oa:AnnotatedAt ?time  " + "}} "
				+ "WHERE { "
				+ "BIND (IRI(str(RAND())) AS ?a) ."
				+ "BIND (now() as ?time) "
				+ "}";
		logger.info("Sparql query {}", sparql);
		this.qanaryUtil.updateTripleStore(sparql);
	}


	/**
	 * set a new language for the current question, stored in the Qanary
	 * triplestore
	 *
	 * @param language
	 */
	public void setLanguageText(String language) throws Exception {
		String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
				+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "INSERT { " 
                                + "GRAPH <"+ this.getOutGraph() + "> { "
				+ "?a a qa:AnnotationOfQuestionLanguage . "
				+ "?a oa:hasBody \"" + language + "\" . "
				+ "?a oa:hasTarget <" + this.getUri() + "> ; "
				+ "   oa:annotatedBy <www.wdaqua.eu/qanary> ; "
				+ "   oa:annotatedAt ?time  " 
                                + " }} "
				+ "WHERE { "
				+ "BIND (IRI(str(RAND())) AS ?a) . "
				+ "BIND (now() as ?time) . "
				+ "}";
                System.out.println(sparql);
		qanaryUtil.updateTripleStore(sparql);
	}


	/**
	 * set a new knowledge-base for the current question, stored in the Qanary
	 * triplestore
	 *
	 * @param targetData
	 */
	public void setTargetData(String targetData) throws Exception {
		String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
				+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "INSERT { "
				+ "GRAPH <" + this.getOutGraph() + "> { "
				+ "?a a qa:AnnotationDataset . "
				+ "?a oa:hasBody \"" + targetData + "\" ;"
				+ "?a oa:hasTarget " + this.getUri()
				+ "   oa:annotatedBy <www.wdaqua.eu/qa> ; "
				+ "   oa:annotatedAt ?time ; "
				+ " }} "
				+ "WHERE { "
				+ "BIND (IRI(str(RAND())) AS ?a) . "
				+ "BIND (now() as ?time) . "
				+ "}";
		qanaryUtil.updateTripleStore(sparql);
	}

}
