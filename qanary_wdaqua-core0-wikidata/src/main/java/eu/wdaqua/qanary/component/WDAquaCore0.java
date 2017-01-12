package eu.wdaqua.qanary.component;

import java.lang.*;
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
import eu.wdaqua.core0.connection.MyAnnotation;

import eu.wdaqua.core0.qa.Execute;

@Component
public class WDAquaCore0 extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(WDAquaCore0.class);

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
	//logger.info("Question {}", myQuestion);
	String lang = null;
	try {
		lang = myQanaryQuestion.getLanguage();
	} catch (Exception e){
		lang = "en";
	}
	logger.info("Question language {}", lang);
        Execute e = new Execute();
        MyAnnotation m = e.goAnnotation(myQuestion, "", lang);
 
        // STEP 2: Push the sparql query and the json object to the named graph reserved for the question
	String sparqlPart1="";
	String sparqlPart2="";
	for (int i=0; i<Math.min(m.numQueries(),30); i++){
		sparqlPart1+="?a"+i+" a qa:AnnotationOfAnswerSPARQL . "
                + "  ?a"+i+" oa:hasTarget <URIAnswer> . "
                + "  ?a"+i+" oa:hasBody \"" +  m.getQuery(i).replace("\n", " ") + "\" ;"
                + "     oa:annotatedBy <www.wdaqua.eu> ; "
                + "         oa:annotatedAt ?time ; "
		+ "         qa:hasScore "+ m.getQueryScore(i) + " . \n";	
		sparqlPart2+= "BIND (IRI(str(RAND())) AS ?a"+i+") . \n";
	}

        String sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
                + "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
                + "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
                + "INSERT { "
                + "GRAPH <" + myQanaryUtils.getInGraph() + "> { "
                + sparqlPart1
                + "}} "
                + "WHERE { "
                + sparqlPart2
                + "BIND (IRI(str(RAND())) AS ?b) ."
                + "BIND (now() as ?time) "
                + "}";	
        myQanaryUtils.updateTripleStore(sparql);

	return myQanaryMessage;
	}

}
