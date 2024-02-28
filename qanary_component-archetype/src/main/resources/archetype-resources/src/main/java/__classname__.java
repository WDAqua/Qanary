#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class ${classname} extends QanaryComponent {
	// yuou might use this parameterizable file to store the query that should be
	// executed for fetching the annotations required for this component from the
	// Qanary triplestore.
	// we encourage re-using existing files from the qa.commons package
	private static final String FILENAME_FETCH_REQUIRED_ANNOTATIONS = "/queries/fetchRequiredAnnotations.rq";
	// you might use this parameterizable file to store the query that should be
	// executed for storing the annotations computed for this component from the
	// Qanary triplestore.
	// we encourage re-using existing files from the qa.commons package
	private static final String FILENAME_STORE_COMPUTED_ANNOTATIONS = "/queries/storeComputedAnnotations.rq";
	
	private static final Logger logger = LoggerFactory.getLogger(${classname}.class);

	private final String applicationName;

	public ${classname}(@Value("${spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;

		// here if the files are available and do contain content
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_FETCH_REQUIRED_ANNOTATIONS);
		QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_STORE_COMPUTED_ANNOTATIONS);
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

		// typical helpers
		QanaryUtils myQanaryUtils = this.getUtils();
		QanaryTripleStoreConnector connectorToQanaryTriplestore = myQanaryUtils.getQanaryTripleStoreConnector();
		
		// --------------------------------------------------------------------
		// STEP 1: get the required data from the Qanary triplestore (the global process memory)
		// --------------------------------------------------------------------
		// if required, then fetch the origin question (here the question is a
		// textual/String question)
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion();

		// retrieve the data you need to implement your component's functionality

		// TODO: define the SPARQL query fetch the data that your component requires
		QuerySolutionMap bindingsForSelect = new QuerySolutionMap();
		// at least the variable GRAPH needs to be replaced by the ingraph as each query needs to be specific for the current process 
		bindingsForSelect.add("GRAPH", ResourceFactory.createResource(myQanaryQuestion.getInGraph().toASCIIString()));

		// TODO: define your SPARQL UPDATE query in the mentioned file
		String sparqlSelectQuery = QanaryTripleStoreConnector.readFileFromResourcesWithMap(FILENAME_FETCH_REQUIRED_ANNOTATIONS, bindingsForSelect);		
		logger.info("generated SPARQL INSERT query: {}", sparqlSelectQuery);
		ResultSet resultset = connectorToQanaryTriplestore.select(sparqlSelectQuery);
		while (resultset.hasNext()) {
			QuerySolution tuple = resultset.next();
		}

		// --------------------------------------------------------------------
		// STEP 2: compute new knowledge about the given question
		// --------------------------------------------------------------------
		// TODO: implement the custom code for your component

		// --------------------------------------------------------------------
		// STEP 3: store computed knowledge about the given question into the Qanary triplestore 
		// (the global process memory)
		// --------------------------------------------------------------------
		logger.info("store data in graph {} of Qanary triplestore endpoint {}", //
				myQanaryMessage.getValues().get(myQanaryMessage.getOutGraph()), //
				myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));

		// push the new data to the Qanary triplestore

		// TODO: define the SPARQL query fetch the data that your component requires
		QuerySolutionMap bindingsForUpdate = new QuerySolutionMap();
		// at least the variable GRAPH needs to be replaced by the outgraph as each query needs to be specific for the current process 
		bindingsForUpdate.add("GRAPH", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));

		// TODO: define your SPARQL UPDATE query in the mentioned file
		String sparqlUpdateQuery = QanaryTripleStoreConnector.readFileFromResourcesWithMap(FILENAME_STORE_COMPUTED_ANNOTATIONS, bindingsForUpdate);
		logger.info("generated SPARQL UPDATE query: {}", sparqlUpdateQuery);
		connectorToQanaryTriplestore.update(sparqlUpdateQuery);

		return myQanaryMessage;
	}
}
