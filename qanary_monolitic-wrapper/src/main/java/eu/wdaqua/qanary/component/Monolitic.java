package eu.wdaqua.qanary.component;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

@Component
public class Monolitic extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(Monolitic.class);

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

        // the class QanaryUtils provides some helpers for standard tasks
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        // STEP 1: the question is retrived
        String myQuestion = myQanaryQuestion.getTextualRepresentation();

        // STEP 2: answer the question and give back the sparql query and the answers in RDF json http://www.w3.org/TR/sparql11-results-json/
        String sparqlAnswer = "SELECT DISTINCT ?x WHERE { "
        		+ "<http://dbpedia.org/resource/DBpedia> <http://dbpedia.org/ontology/developer> ?x . "
        		+ "?x     <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Agent> . "
        		+ "}";
       
	sparqlAnswer = "ASK WHERE { "
                        + "<http://dbpedia.org/resource/DBpedia> <http://dbpedia.org/ontology/developer> ?x . "
                        + "?x     <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Agent> . "
                        + "}";
	 
	Query query = QueryFactory.create(sparqlAnswer);
	String json;
	if (query.isAskType()){
		Boolean result = myQanaryUtils.askTripleStore(sparqlAnswer, "http://dbpedia.org/sparql");
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ResultSetFormatter.outputAsJSON(outputStream, result);
		json = new String(outputStream.toByteArray());
	} else {
		ResultSet result = myQanaryUtils.selectFromTripleStore(sparqlAnswer, "http://dbpedia.org/sparql"); 
        	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        	ResultSetFormatter.outputAsJSON(outputStream, result);
     		json = new String(outputStream.toByteArray());
	}
        logger.info("Generated SPARQL query: {} ", sparqlAnswer);
        logger.info("Generated answers in RDF json: {}", json);
        
        // STEP 3: pPush the sparql query and the json object to the named graph reserved for the question
        String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
                + "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
                + "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
                + "INSERT { "
                + "GRAPH <" + myQanaryUtils.getInGraph() + "> { "
                + "  ?a a qa:AnnotationOfAnswerSPARQL . "
                + "  ?a oa:hasTarget <URIAnswer> . "
                + "  ?a oa:hasBody \"" + sparqlAnswer.replace("\n", " ") + "\" ;"
                + "     oa:annotatedBy <http://monolitic-component.org> ; "
                + "	    oa:AnnotatedAt ?time . "
                + "  ?b a qa:AnnotationOfAnswerJSON . "
                + "  ?b oa:hasTarget <URIAnswer> . "
                + "  ?b oa:hasBody \"" + json.replace("\n", " ").replace("\"", "\\\"") + "\" ;"
                + "     oa:annotatedBy <http://monolitic-component.org> ; "
                + "	    oa:annotatedAt ?time  "
                + "}} "
                + "WHERE { "
                + "BIND (IRI(str(RAND())) AS ?a) ."
                + "BIND (IRI(str(RAND())) AS ?b) ."
                + "BIND (now() as ?time) "
                + "}";	
        myQanaryUtils.updateTripleStore(sparql);

		return myQanaryMessage;
	}

}
