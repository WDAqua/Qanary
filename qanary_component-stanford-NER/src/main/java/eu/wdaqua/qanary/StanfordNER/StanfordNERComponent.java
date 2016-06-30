package eu.wdaqua.qanary.StanfordNER;

import java.util.ArrayList;
import java.util.Properties;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

/**
 * represents a wrapper of the Stanford NER tool used here as a spotter
 * 
 * @author Dennis Diefenbach
 *
 */

@Component
public class StanfordNERComponent extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(StanfordNERComponent.class);

	/**
	 * default processor of a QanaryMessage
	 */
	public QanaryMessage process(QanaryMessage myQanaryMessage) {
		long startTime = System.currentTimeMillis();
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		logger.info("Qanary Message: {}", myQanaryMessage);

		// STEP1: Retrieve the named graph and the endpoint
		String endpoint = myQanaryMessage.getEndpoint().toASCIIString();
		String namedGraph = myQanaryMessage.getInGraph().toASCIIString();
		logger.info("Endpoint: {}", endpoint);
		logger.info("InGraph: {}", namedGraph);

		// STEP2: Retrieve information that are needed for the computations
		//Retrive the uri where the question is exposed 
		String sparql = "PREFIX qa:<http://www.wdaqua.eu/qa#> "
						+"SELECT ?questionuri "
						+"FROM <"+namedGraph+"> "
						+"WHERE {?questionuri a qa:Question}";
		ResultSet result=selectTripleStore(sparql,endpoint);
		String uriQuestion=result.next().getResource("questionuri").toString();
		logger.info("Uri of the question: {}", uriQuestion);
		//Retrive the question itself
		RestTemplate restTemplate = new RestTemplate();
		//TODO: pay attention to "/raw" maybe change that
		ResponseEntity<String> responseEntity = restTemplate.getForEntity(uriQuestion+"/raw", String.class);
		String question=responseEntity.getBody();
		logger.info("Question: {}", question);
		
		// STEP3: Pass the information to the component and execute it
		// TODO: ATTENTION: This should be done only ones when the component
		// is started
		// Define the properties needed for the pipeline of the Stanford
		// parser
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		// Create a new pipline
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// Create an empty annotation just with the given text
		Annotation document = new Annotation(question);
		// Run the stanford annotator on question
		pipeline.annotate(document);
		// Identify which parts of the question is tagged by the NER tool
		// Go ones through the question,
		ArrayList<Selection> selections = new ArrayList<Selection>();
		CoreLabel startToken = null; // stores the last token with non-zero
										// tag, if it does not exist set to
										// null
		CoreLabel endToken = null; // stores the last found token with
									// non-zero tag, if it does not exist
									// set to null
		//Note that consequent non-zero tokens with the same tag like " 0 PERSON PERSON 0 " must be considered together
		// Iterate over the tags
		for (CoreLabel token : document.get(TokensAnnotation.class)) {
			logger.info("Tagged question (token ---- tag): {}", token.toString() + "  ----  " +token.get(NamedEntityTagAnnotation.class));
			if (!token.get(NamedEntityTagAnnotation.class).equals("O")) {
				if (startToken == null) {
					startToken = token;
					endToken = token;
				} else {
					if (startToken.get(NamedEntityTagAnnotation.class) == token.get(NamedEntityTagAnnotation.class)) {
						endToken = token;
					} else {
						Selection s = new Selection();
						s.begin = startToken.beginPosition();
						s.end = endToken.endPosition();
						selections.add(s);
						startToken = token;
						endToken = token;
					}
				}
			} else {
				if (startToken != null) {
					Selection s = new Selection();
					s.begin = startToken.beginPosition();
					s.end = endToken.endPosition();
					selections.add(s);
					startToken = null;
					endToken = null;
				}
			}
		}
		if (startToken != null) {
			Selection s = new Selection();
			s.begin = startToken.beginPosition();
			s.end = endToken.endPosition();
			selections.add(s);
		}

		// STEP4: Push the result of the component to the triplestore
		logger.info("Apply vocabulary alignment on outgraph");
		for (Selection s : selections) {
			sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
					+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " + "INSERT { " + "GRAPH <" + namedGraph + "> { "
					+ "  ?a a qa:AnnotationOfSpotInstance . " + "  ?a oa:hasTarget [ "
					+ "           a    oa:SpecificResource; " + "           oa:hasSource    <" + uriQuestion + ">; "
					+ "           oa:hasSelector  [ " + "                    a oa:TextPositionSelector ; "
					+ "                    oa:start \"" + s.begin + "\"^^xsd:nonNegativeInteger ; "
					+ "                    oa:end  \"" + s.end + "\"^^xsd:nonNegativeInteger  " + "           ] "
					+ "  ] ; " + "     oa:annotatedBy <http://nlp.stanford.edu/software/CRF-NER.shtml> ; "
					+ "	    oa:AnnotatedAt ?time  " + "}} " + "WHERE { " + "BIND (IRI(str(RAND())) AS ?a) ."
					+ "BIND (now() as ?time) " + "}";
			loadTripleStore(sparql, endpoint);
		}
		long estimatedTime = System.currentTimeMillis() - startTime;
		logger.info("Time: {}", estimatedTime);

		return myQanaryMessage;
	}

	public void loadTripleStore(String sparqlQuery, String endpoint) {
		UpdateRequest request = UpdateFactory.create(sparqlQuery);
		UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);
		proc.execute();
	}

	public ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
		return qExe.execSelect();
	}

	class Selection {
		public int begin;
		public int end;
	}

}
