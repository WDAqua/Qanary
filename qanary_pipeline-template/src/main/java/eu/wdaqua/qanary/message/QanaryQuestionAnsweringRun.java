package eu.wdaqua.qanary.message;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.web.QanaryQuestionAnsweringController;

/**
 * represents the message send back to the client after starting a QA process
 *
 * @author AnBo
 */
public class QanaryQuestionAnsweringRun {

    private final URI questionAnsweringRunUri;

    private final URI endpoint;

    private final URI inGraph;
    private final URI outGraph;

    private final URI question;

    public QanaryQuestionAnsweringRun(UUID runId, URI question, URI endpoint, URI inGraph, URI outGraph,
                                      QanaryConfigurator configurator) throws URISyntaxException {
        this.questionAnsweringRunUri = new URI(configurator.getHost() + ":" + configurator.getPort()
                + QanaryQuestionAnsweringController.QUESTIONANSWERING + "/" + runId.toString());
        this.endpoint = endpoint;
        this.inGraph = inGraph;
        this.outGraph = outGraph;
        this.question = question;
    }

    public String toString() {
        return this.getQuestionAnsweringRunUri().toASCIIString();
    }

    private URI getQuestionAnsweringRunUri() {
        return this.questionAnsweringRunUri;
    }

    public URI getEndpoint() {
        return this.endpoint;
    }

    public URI getInGraph() {
        return this.inGraph;
    }

    public URI getOutGraph() {
        return this.outGraph;
    }

    public URI getQuestion() {
        return this.question;
    }

}
