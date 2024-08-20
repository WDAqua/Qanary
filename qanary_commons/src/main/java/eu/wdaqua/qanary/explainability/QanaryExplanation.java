package eu.wdaqua.qanary.explainability;

import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;

import java.io.IOException;
import java.net.URISyntaxException;

public interface QanaryExplanation {

    abstract public String explain(QanaryExplanationData qanaryExplanationData) throws IOException, URISyntaxException, SparqlQueryFailed;

}