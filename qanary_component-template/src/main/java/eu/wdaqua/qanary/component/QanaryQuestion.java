package eu.wdaqua.qanary.component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.component.config.QanaryConfiguration;
import eu.wdaqua.qanary.component.QanaryUtils;
import eu.wdaqua.qanary.component.ontology.TextPositionSelector;

/**
 * represents the access to a question in a triplestore
 * 
 * TODO: should be refactored to disctinguished class for different representations like text, audio, ...
 *
 * @param <T>
 */
public class QanaryQuestion<T> {

	private static final Logger logger = LoggerFactory.getLogger(QanaryQuestion.class);

	private final URI endpoint;
	private final URI inGraph;
	private final URI outGraph;
	private URI uri;
	private QanaryUtils qanaryUtil;
	private T raw;
	private URI uriTextualRepresentation;
	private String textualRepresentation;
	private URI uriAudioRepresentation;
	private byte[] audioRepresentation;

	public QanaryQuestion(QanaryMessage qanaryMessage) {
		this.endpoint = qanaryMessage.getEndpoint();
		this.inGraph = qanaryMessage.getInGraph();
		this.outGraph = qanaryMessage.getOutGraph();
		qanaryUtil = new QanaryUtils(qanaryMessage);
	}

	/**
	 * returns the endpoint provided by the QanaryMessage object provided via
	 * constructor
	 */
	public URI getEndpoint() {
		return this.endpoint;
	}

	/**
	 * returns the inGraph provided by the QanaryMessage object provided via
	 * constructor
	 */
	public URI getInGraph() {
		return this.inGraph;
	}

	/**
	 * returns the outGraph provided by the QanaryMessage object provided via
	 * constructor
	 */
	public URI getOutGraph() {
		return this.outGraph;
	}

	/**
	 * get original question URI from the pipeline endpoint
	 */
	public URI getUri() throws Exception {
		if (this.uri == null) {
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
				throw new Exception("More than 1 question (count: " + i + ") in graph " + this.getInGraph() + " at "
						+ this.getEndpoint());
			} else if (i == 0) {
				throw new Exception(
						"No question available in graph " + this.getInGraph() + " at " + this.getEndpoint());
			}
			this.uri = new URI(question);
		}
		return this.uri;
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

}
