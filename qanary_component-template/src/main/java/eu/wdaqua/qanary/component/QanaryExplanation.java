package eu.wdaqua.qanary.component;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.message.QanaryQuestionAnsweringRun;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

public class QanaryExplanation {


    private final String GRAPH_QUERY = "PREFIX oa: <http://www.w3.org/ns/openannotation/core/>\n" +
            "PREFIX qa: <http://www.wdaqua.eu/qa#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "\n" +
            "SELECT DISTINCT ?graph ?p ?o\n" +
            "WHERE {\n" +
            "\tGRAPH ?graph {\n" +
            "  \t\t?questionId owl:sameAs <urn:qanary:currentQuestion> .\n" +
            "  \t}\n" +
            "}";

    // Good caching candidate
    // Explanations aren't stored in the triplestore, instead, the /explain endpoint is used
    public void explain(QanaryQuestionAnsweringRun qanaryQuestionAnsweringRun) {
        // PaC can have two endpoints

    }

    /**
     * Get all used components by filtering those which created AnnotationOfLog+ annotations
     * @throws IOException
     */
    public void getAllUsedComponents(String graph) throws IOException {

    }

    public void getAllGraphs(String questionId) {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("questionId", ResourceFactory.createResource(questionId));
        String query = GRAPH_QUERY.replace("?questionId", "<" + questionId + ">");

    }

}
