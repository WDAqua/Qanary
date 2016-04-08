package eu.wdaqua.qanary.web;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.hp.hpl.jena.sparql.modify.GraphStoreBasic;
import com.hp.hpl.jena.sparql.util.NodeUtils;
import com.hp.hpl.jena.update.*;
import eu.wdaqua.qanary.business.QanaryConfigurator;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URI;
import java.net.URL;
import java.util.UUID;

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
     * start a configured process
     *
     * @return
     */
    @RequestMapping(value = "/questionanswering", method = RequestMethod.POST)
    @ResponseBody
    public String questionanswering(@RequestParam(value = "question", required = true) final URL questionUri) {
        // Create the name of a new named graph
        final UUID runID = UUID.randomUUID();
        final String namedGraph = runID.toString();
        this.initGraphInTripelStore(namedGraph, questionUri);

        // TODO: call all defined components

        return runID.toString();
    }

    /**
     * init the grpah in the triplestore (c.f., applicationproperties)
     *
     * @param namedGraph
     * @param questionUri
     */
    private void initGraphInTripelStore(String namedGraph, final URL questionUri) {
        final URI triplestore = qanaryConfigurator.getEndpoint();
        namedGraph = "<urn:graph:" + namedGraph + ">";

        // Load the Open Annotation Ontology
        // TODO: store this locally for performance issues
//        String sparqlquery = "";
//        sparqlquery = "LOAD <http://www.openannotation.org/spec/core/20130208/oa.owl> INTO GRAPH " + namedGraph;

        // Using local jean store to store the data
        //TODO: is this step needed on every execution or should it no be an init step?
        final Node graphName = NodeUtils.asNode(namedGraph);
        inMemoryStore.addGraph(graphName, GraphFactory.createGraphMem());
        final Model model = RDFDataMgr.loadModel("http://www.openannotation.org/spec/core/20130208/oa.ow");
        model.listStatements().forEachRemaining(statement -> {
            inMemoryStore.add(new Quad(graphName, statement.asTriple()));
        });

        System.out.println("\n ++++++++++++++++\n" + sparqlquery);
        insertSparqlIntoTriplestore(sparqlquery); // fail
        // loadTripleStore(sparqlquery, triplestore);

        logger.debug("UPDATED");

        // TODO: load ontology into graph
        // Load the QAontology
        sparqlquery = "LOAD <http://localhost:" + qanaryConfigurator.getPort() + "/QAOntology_raw.ttl> INTO GRAPH "
                + namedGraph;
        loadTripleStore(sparqlquery, triplestore);

        // Prepare the question, answer and dataset objects
        sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#>" + "INSERT DATA {GRAPH " + namedGraph + "{ <" + questionUri
                .toString() + "> a qa:Question}}";
        loadTripleStore(sparqlquery, triplestore);

        sparqlquery = "PREFIX qa: <http://www.wdaqua.eu/qa#>" + "INSERT DATA {GRAPH " + namedGraph + "{" + this
                .getQuestionAnsweringHostUrlString() + "/Answer> a qa:Answer}}";
        loadTripleStore(sparqlquery, triplestore);

        sparqlquery =
                "PREFIX qa: <http://www.wdaqua.eu/qa#>" + "INSERT DATA {GRAPH " + namedGraph + "{" + qanaryConfigurator
                        .getHost() + ":" + qanaryConfigurator.getPort() + "/Dataset> a qa:Dataset}}";
        loadTripleStore(sparqlquery, triplestore);

        // Make the first two annotations
        sparqlquery = "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
                + "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
                + "INSERT DATA { " + "GRAPH " + namedGraph //
                + "{ " //
                + "<anno1> a  oa:AnnotationOfQuestion; " //
                + "   oa:hasTarget <" + questionUri.toString() + "> ;" //
                + "   oa:hasBody   <URIAnswer>   ." //
                + "<anno2> a  oa:AnnotationOfQuestion;" //
                + "   oa:hasTarget <" + questionUri.toString() + "> ;" //
                + "   oa:hasBody   <URIDataset> " + "}}";
        loadTripleStore(sparqlquery, triplestore);
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
        final UpdateProcessor updateProcessor = UpdateExecutionFactory.create(sparqlQuery, inMemoryStore);
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
