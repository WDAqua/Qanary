package eu.wdaqua.qanary.component;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
//import org.apache.jena.query.Query;
//import org.apache.jena.query.QueryExecution;
//import org.apache.jena.query.QueryExecutionFactory;
//import org.apache.jena.query.QueryFactory;
//import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * represent the behavior of an annotator following the Qanary methodology
 * 
 * @author AnBo
 *
 */
public abstract class QanaryComponent {

	private static final Logger logger = LoggerFactory.getLogger(QanaryComponent.class);

	// TODO need to be changed
	final String questionUrl = "http://localhost:8080/question/28f56d32-b30a-428d-ac90-79372a6f7625/";

	/**
	 * needs to be implemented for any new Qanary component
	 * 
	 * @param myQanaryMessage
	 * @return
	 */
	public abstract QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception;

	/**
	 * fetch raw data for a question
	 * 
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String getQuestionRawData() throws ClientProtocolException, IOException {
		/*
		 * @SuppressWarnings("deprecation") HttpClient client = new
		 * DefaultHttpClient(); HttpGet request = new HttpGet(questionUrl +
		 * QanaryConfiguration.questionRawDataUrlSuffix); HttpResponse response
		 * = client.execute(request);
		 * 
		 * // Get the response BufferedReader rd = new BufferedReader(new
		 * InputStreamReader(response.getEntity().getContent()));
		 * 
		 * String rawText = ""; String line = ""; while ((line = rd.readLine())
		 * != null) { rawText.concat(line); }
		 */
		String rawText = "";
		return rawText;
	}

	/**
	 * get Qanary question
	 */
	public QanaryQuestion getQuestion() {

		// TODO: fetch from endpoint+ingraph via SPARQL the resource of rdf:type
		// qa:Question

		// TODO: create QanaryQuestion object with question URL and raw data
		// this.getQuestionRawData()

		return null;
	}

	/**
	 * query a SPARQL endpoint with a given query
	 * 
	 * @param sparqlQuery
	 * @param endpoint
	 * @return
	 */
	// public ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
	// logger.debug("selectTripleStore on {} execute {}", endpoint,
	// sparqlQuery);
	// Query query = QueryFactory.create(sparqlQuery);
	// QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint,
	// query);
	// ResultSet resultset = qExe.execSelect();
	// return resultset;
	// }

	/**
	 * get the question URI from the pipeline endpoint
	 * 
	 * @param myQanaryMessage
	 * @return
	 * @throws Exception
	 */
	// public URI getQuestion(QanaryMessage myQanaryMessage) throws Exception {
	// ResultSet resultset = this.selectTripleStore(
	// "SELECT ?question FROM <" + myQanaryMessage.getInGraph()
	// + "> {?question <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>
	// <http://www.wdaqua.eu/qa#Question>}",
	// myQanaryMessage.getEndpoint().toString());
	//
	// int i = 0;
	// String question = null;
	// while (resultset.hasNext()) {
	// question = resultset.next().get("question").asResource().getURI();
	// logger.debug("{}: qa#Question = {}", i++, question);
	// }
	// if (i > 1) {
	// throw new Exception("More than 1 question (count: " + i + ") in graph " +
	// myQanaryMessage.getInGraph()
	// + " at " + myQanaryMessage.getEndpoint());
	// } else if (i == 0) {
	// throw new Exception("No question available in graph " +
	// myQanaryMessage.getInGraph() + " at "
	// + myQanaryMessage.getEndpoint());
	// }
	//
	// URI questionUri = new URI(question);
	//
	// logger.info("question {} found in {} at {}", question,
	// myQanaryMessage.getInGraph(),
	// myQanaryMessage.getEndpoint());
	//
	// return questionUri;
	//
	// }

}
