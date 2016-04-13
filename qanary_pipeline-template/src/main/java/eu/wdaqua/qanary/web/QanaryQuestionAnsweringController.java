package eu.wdaqua.qanary.web;

import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.sparql.modify.GraphStoreBasic;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.message.QanaryComponentNotAvailableException;
import eu.wdaqua.qanary.message.QanaryMessage;

/**
 * controller for processing questions, i.e., related to the question answering
 * process
 *
 * @author AnBo
 */
@Controller
public class QanaryQuestionAnsweringController {

	private static final Logger logger = LoggerFactory.getLogger(QanaryQuestionAnsweringController.class);

	private final QanaryConfigurator qanaryConfigurator;

	/**
	 * Jena model
	 */
	private final GraphStore inMemoryStore;

	/**
	 * inject QanaryConfigurator
	 */
	@Autowired
	public QanaryQuestionAnsweringController(final QanaryConfigurator qanaryConfigurator) {
		this.qanaryConfigurator = qanaryConfigurator;

		inMemoryStore = new GraphStoreBasic(new DatasetImpl(ModelFactory.createDefaultModel()));
	}

	/**
	 * a simple HTML input form for starting a question answering processs
	 *
	 * @return
	 */
	@RequestMapping(value = "/startquestionanswering", method = RequestMethod.GET)
	public String startquestionanswering() {
		return "startquestionanswering";
	}

	/**
	 * exposing the oa vocabulary
	 *
	 * @return
	 */
	@RequestMapping(value = "/oa.owl", method = RequestMethod.GET, produces = "application/sparql-results+xml")
	@ResponseBody
	public FileSystemResource getFile1() {
		return new FileSystemResource("src/main/resources/oa.owl");
	}

	/**
	 * exposing the qanary ontology
	 *
	 * @return
	 */
	@RequestMapping(value = "/qanaryOntology.ttl", method = RequestMethod.GET, produces = "text/turtle")
	@ResponseBody
	public FileSystemResource getFile2() {
		return new FileSystemResource("src/main/resources/qanaryOntology.ttl");
	}

	/*
	 * @ResponseBody public String plaintext(HttpServletResponse response) {
	 * response.setContentType("text/turtle");
	 * response.setCharacterEncoding("UTF-8"); return "qanaryOntology"; }
	 * 
	 */

	/**
	 * start a configured process
	 *
	 * @return
	 * @throws QanaryComponentNotAvailableException
	 */
	@RequestMapping(value = "/questionanswering", method = RequestMethod.POST)
	@ResponseBody
	public String questionanswering(@RequestParam(value = "question", required = true) final URL questionUri,
			@RequestParam(value = "componentlist") final LinkedList<String> componentsToBeCalled)
					throws QanaryComponentNotAvailableException {
		// Create the name of a new named graph
		final UUID runID = UUID.randomUUID();
		final String namedGraph = runID.toString();
		URI endpoint = this.initGraphInTripelStore(namedGraph, questionUri);

		QanaryMessage myQanaryMessage = new QanaryMessage(endpoint, namedGraph);

		// execute synchronous calls to all components with the same message
		for (String componentName : componentsToBeCalled) {
			qanaryConfigurator.callServicesByName(componentsToBeCalled, myQanaryMessage);
		}

		return runID.toString();
	}

	/**
	 * init the grpah in the triplestore (c.f., applicationproperties)
	 *
	 * @param namedGraph
	 * @param questionUri
	 */
	private URI initGraphInTripelStore(String namedGraph, final URL questionUri) {
		final URI triplestore = qanaryConfigurator.getEndpoint();
		logger.info("Triplestore " + triplestore);
		namedGraph = "<urn:graph:" + namedGraph + ">";
		String sparqlquery = "";

		/*
		 * // TODO: please look if this code is still necessary. PS: I really
		 * would prefer as a triplesotre and external service and not an
		 * internal one. It is easier to debug. // Using local jean store to
		 * store the data // TODO: is this step needed on every execution or
		 * should it no be an // init step? final Node graphName =
		 * NodeUtils.asNode(namedGraph); inMemoryStore.addGraph(graphName,
		 * GraphFactory.createGraphMem()); final Model model =
		 * RDFDataMgr.loadModel(
		 * "http://www.openannotation.org/spec/core/20130208/oa.ow");
		 * model.listStatements().forEachRemaining(statement -> {
		 * inMemoryStore.add(new Quad(graphName, statement.asTriple())); });
		 * 
		 * System.out.println("\n ++++++++++++++++\n" + sparqlquery);
		 * insertSparqlIntoTriplestore(sparqlquery); // fail //
		 * loadTripleStore(sparqlquery, triplestore);
		 */

		// Load the Open Annotation Ontology
		sparqlquery = "LOAD <http://localhost:" + qanaryConfigurator.getPort() + "/oa.owl> INTO GRAPH " + namedGraph;
		logger.info("Sparql query " + sparqlquery);
		loadTripleStore(sparqlquery, triplestore);

		// Load the Qanary Ontology
		sparqlquery = "LOAD <http://localhost:" + qanaryConfigurator.getPort() + "/qanaryOntology.ttl> INTO GRAPH "
				+ namedGraph;
		logger.info("Sparql query " + sparqlquery);
		loadTripleStore(sparqlquery, triplestore);

		// Prepare the question, answer and dataset objects
		sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#> " + "INSERT DATA {GRAPH " + namedGraph + " { <"
				+ questionUri.toString() + "> a qa:Question}}";
		logger.info("Sparql query " + sparqlquery);
		loadTripleStore(sparqlquery, triplestore);

		sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#>" + "INSERT DATA {GRAPH " + namedGraph + " {<"
				+ this.getQuestionAnsweringHostUrlString() + "/Answer> a qa:Answer}}";
		logger.info("Sparql query " + sparqlquery);
		loadTripleStore(sparqlquery, triplestore);

		sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#>" + "INSERT DATA {GRAPH " + namedGraph + " {<"
				+ qanaryConfigurator.getHost() + ":" + qanaryConfigurator.getPort() + "/Dataset> a qa:Dataset}}";
		logger.info("Sparql query " + sparqlquery);
		loadTripleStore(sparqlquery, triplestore);

		// Make the first two annotations
		sparqlquery = "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
				+ "INSERT DATA { " + "GRAPH " + namedGraph + " { " //
				+ "<anno1> a  oa:AnnotationOfQuestion; " //
				+ "   oa:hasTarget <" + questionUri.toString() + "> ;" //
				+ "   oa:hasBody   <URIAnswer>   . " //
				+ "<anno2> a  oa:AnnotationOfQuestion; " //
				+ "   oa:hasTarget <" + questionUri.toString() + "> ; " //
				+ "   oa:hasBody   <URIDataset> " + "}}";
		logger.info("Sparql query " + sparqlquery);
		loadTripleStore(sparqlquery, triplestore);

		return triplestore;
	}

	/**
	 * insert into local Jena triplestore
	 * <p>
	 * TODO: needs to be extracted
	 *
	 * @param sparqlQuery
	 */
	public void insertSparqlIntoTriplestore(final String sparqlQuery) {
		final UpdateRequest request = UpdateFactory.create(sparqlQuery);
		final UpdateProcessor updateProcessor = UpdateExecutionFactory.create(request, inMemoryStore);
		updateProcessor.execute();
	}

	/**
	 * executes a SPARQL INSERT into the triplestore
	 *
	 * @param query
	 * @return map
	 */
	public static void loadTripleStore(final String sparqlQuery, final URI endpoint) {
		final UpdateRequest request = UpdateFactory.create(sparqlQuery);
		final UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint.toString());
		proc.execute();
	}

	/**
	 * returns a valid URL (string) of configured properties
	 */
	private String getQuestionAnsweringHostUrlString() {
		return this.qanaryConfigurator.getHost() + ":" + this.qanaryConfigurator.getPort() + "/";
	}
}
