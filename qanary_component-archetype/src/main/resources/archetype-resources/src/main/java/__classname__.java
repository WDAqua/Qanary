#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class ${classname} extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(${classname}.class);

	private final String applicationName;

	public ${classname}(@Value("${spring.application.name}")final String applicationName) {
		this.applicationName = applicationName;
	}
	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component, some helping notes w.r.t. the typical 3 steps of implementing a
	 * Qanary component are included in the method (you might remove all of them)
	 * 
	 * @throws SparqlQueryFailed
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);

		// STEP 1: get the required data from the Qanary triplestore (the global process memory)

		// if required, then fetch the origin question (here the question is a
		// textual/String question)
		QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion<String>(myQanaryMessage);

		// TODO: define the SPARQL query fetch the data that your component requires
		String sparqlSelectQuery = "..."; // define your SPARQL SELECT query here

		ResultSet resultset = myQanaryUtils.selectFromTripleStore(sparqlSelectQuery);
		while (resultset.hasNext()) {
			QuerySolution tupel = resultset.next();
			// TODO: retrieve the data you need to implement your component's functionality
		}

		// STEP 2: compute new knowledge about the given question
		// TODO: implement the custom code for your component

		// STEP 3: store computed knowledge about the given question into the Qanary triplestore 
		// (the global process memory)

		logger.info("store data in graph {} of Qanary triplestore endpoint {}", //
				myQanaryMessage.getValues().get(myQanaryMessage.getOutGraph()), //
				myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// push data to the Qanary triplestore
		// TODO: define your SPARQL UPDATE query here
		String sparqlUpdateQuery = "..."; 

		myQanaryUtils.updateTripleStore(sparqlUpdateQuery, myQanaryMessage.getEndpoint());

		return myQanaryMessage;
	}
}
