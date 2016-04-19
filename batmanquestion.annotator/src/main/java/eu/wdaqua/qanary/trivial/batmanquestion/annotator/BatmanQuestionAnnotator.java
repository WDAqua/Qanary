package eu.wdaqua.qanary.trivial.batmanquestion.annotator;

import java.net.URI;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

@Component
public class BatmanQuestionAnnotator extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(BatmanQuestionAnnotator.class);

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * 
	 * @throws Exception
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

		logger.info("process: {}", myQanaryMessage);
		logger.info("received: sparql Endpoint: {}, inGraph: {}, outGraph: {}", myQanaryMessage.getEndpoint(),
				myQanaryMessage.getInGraph(), myQanaryMessage.getOutGraph());
				// TODO: implement processing of question

		// try {
		// logger.info("store data in graph {}", myQanaryMessage.get(new
		// URL(QanaryMessage.endpointKey)));
		// // TODO: insert data in QanaryMessage.outgraph
		// } catch (MalformedURLException e) {
		// e.printStackTrace();
		// }

		logger.info("apply vocabulary alignment on outgraph for the Batman question");
		// TODO: implement this (custom for every component)

		this.getQuestion(myQanaryMessage);

		// logger.debug("SELECT * FROM <" + myQanaryMessage.getInGraph() + ">
		// {?s ?p ?o }");

		return myQanaryMessage;
	}

	/**
	 * query a SPARQL endpoint with a given query
	 * 
	 * @param sparqlQuery
	 * @param endpoint
	 * @return
	 */
	public ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
		logger.debug("selectTripleStore on {} execute {}", endpoint, sparqlQuery);
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
		ResultSet resultset = qExe.execSelect();
		return resultset;
	}

	/**
	 * get the question URI from the pipeline endpoint
	 * 
	 * @param myQanaryMessage
	 * @return
	 * @throws Exception
	 */
	public URI getQuestion(QanaryMessage myQanaryMessage) throws Exception {
		ResultSet resultset = this.selectTripleStore(
				"SELECT ?question FROM <" + myQanaryMessage.getInGraph()
						+ "> {?question <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.wdaqua.eu/qa#Question>}",
				myQanaryMessage.getEndpoint().toString());

		int i = 0;
		String question = null;
		while (resultset.hasNext()) {
			question = resultset.next().get("question").asResource().getURI();
			logger.debug("{}: qa#Question = {}", i++, question);
		}
		if (i > 1) {
			throw new Exception("More than 1 question (count: " + i + ") in graph " + myQanaryMessage.getInGraph()
					+ " at " + myQanaryMessage.getEndpoint());
		} else if (i == 0) {
			throw new Exception("No question available in graph " + myQanaryMessage.getInGraph() + " at "
					+ myQanaryMessage.getEndpoint());
		}

		URI questionUri = new URI(question);

		logger.info("question {} found in {} at {}", question, myQanaryMessage.getInGraph(),
				myQanaryMessage.getEndpoint());

		return questionUri;

	}

}