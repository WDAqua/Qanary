package eu.wdaqua.qanary.queryExecuter;

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

import eu.wdaqua.qanary.component.QanaryQuestion;
import eu.wdaqua.qanary.component.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

@Component
public class QueryExecuter extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(QueryExecuter.class);

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        	
		// STEP 1: the SPARQL query with highest score is retrived from the bounge of queries pushed at the last time stemp
		String sparql="PREFIX qa: <http://www.wdaqua.eu/qa#> "
    			+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
  			+ "SELECT ?sparql "
  			+ "FROM <"+ myQanaryMessage.getInGraph() + "> "
  			+ "WHERE { "
  			+ "  ?a a qa:AnnotationOfAnswerSPARQL . "
  			+ "  OPTIONAL {?a oa:hasBody ?sparql } "
  			+ "  ?a qa:hasScore ?score . "
                        + "  ?a oa:annotatedAt ?time . " 
                        + "  { "
                        + "    SELECT ?time { "
                        + "     ?a a qa:AnnotationOfAnswerSPARQL . "
                        + "     ?a oa:annotatedAt ?time . "
  	                + "    } order by ?time limit 1 "
                        + "  } "
  			+ "} "
  			+ "ORDER BY DESC(?score) LIMIT 1"  ;
		ResultSet resultset = myQanaryUtils.selectFromTripleStore(sparql);
                String sparqlQuery="";
                while (resultset.hasNext()) {
			sparqlQuery = resultset.next().get("sparql").toString();	
		}
		logger.info("Generated SPARQL query: {} ", sparqlQuery);
		// STEP 2: execute the first sparql query
        	String endpoint = "";
                if (sparqlQuery.contains("http://dbpedia.org")){
                	endpoint = "http://dbpedia.org/sparql";
                } else {
                	endpoint = "https://query.wikidata.org/sparql";
                }
		Query query = QueryFactory.create(sparqlQuery);
        	String json;
        	if (query.isAskType()){
                	Boolean result = myQanaryUtils.askTripleStore(sparqlQuery, endpoint);
                	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                	ResultSetFormatter.outputAsJSON(outputStream, result);
                	json = new String(outputStream.toByteArray(), "UTF-8");
        	} else {
               		ResultSet result = myQanaryUtils.selectFromTripleStore(sparqlQuery, endpoint);
                	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                	ResultSetFormatter.outputAsJSON(outputStream, result);
                	json = new String(outputStream.toByteArray(), "UTF-8");
        	}
        	logger.info("Generated answers in RDF json: {}", json);

        	// STEP 3: Push the the json object to the named graph reserved for the question
        	logger.info("apply vocabulary alignment on outgraph");
		sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
                	+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
                	+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
                	+ "INSERT { "
                	+ "GRAPH <" + myQanaryUtils.getOutGraph() + "> { "
                	+ "  ?b a qa:AnnotationOfAnswerJSON . "
                	+ "  ?b oa:hasTarget <URIAnswer> . "
                	+ "  ?b oa:hasBody \"" + json.replace("\n", " ").replace("\"", "\\\"") + "\" ;"
                	+ "     oa:annotatedBy <www.wdaqua.eu> ; "
                	+ "         oa:annotatedAt ?time  "
                	+ "}} "
                	+ "WHERE { "
                	+ "  BIND (IRI(str(RAND())) AS ?b) ."
                	+ "  BIND (now() as ?time) "
                	+ "}";
        	myQanaryUtils.updateTripleStore(sparql);
		return myQanaryMessage;
	}
}
