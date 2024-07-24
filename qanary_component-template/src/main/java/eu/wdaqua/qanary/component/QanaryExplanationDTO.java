package eu.wdaqua.qanary.component;

//TODO: Remove and Rename

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QanaryExplanationDTO {

    private URI rootGraph;
    private URI priorGraph;
    private URI questionId;
    private Map<URI, ArrayList<URI>> endpointAndGraphs;

    public URI popAndReturnGraph(URI endpoint) {
        URI graph = endpointAndGraphs.get(endpoint).get(0);
        endpointAndGraphs.get(endpoint).remove(0);
        return graph;
    }

    public ArrayList<URI> getGraphs(URI endpoint) {
        return this.endpointAndGraphs.get(endpoint);
    }

    public URI setNewEndpointWithGraph(URI endpoint, ArrayList<URI> graphs) {
        this.endpointAndGraphs.put(endpoint, graphs);
        return popAndReturnGraph(endpoint);
    }

    public boolean doesEndpointExist(URI endpoint) {
        return !this.endpointAndGraphs.get(endpoint).isEmpty();
    }

    public URI getPriorGraph() {
        return priorGraph;
    }

    public URI getQuestionId() {
        return questionId;
    }

    public URI getRootGraph() {
        return rootGraph;
    }

    public void setEndpointAndGraphs(Map<URI, ArrayList<URI>> endpointAndGraphs) {
        this.endpointAndGraphs = endpointAndGraphs;
    }

    public void setPriorGraph(URI priorGraph) {
        this.priorGraph = priorGraph;
    }

    public void setQuestionId(URI questionId) {
        this.questionId = questionId;
    }

    public void setRootGraph(URI rootGraph) {
        this.rootGraph = rootGraph;
    }
}
