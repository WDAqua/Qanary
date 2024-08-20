package eu.wdaqua.qanary.explainability;

import java.util.Map;

/**
 * This class represents the data structure used to store information related to a Qanary explanation request.
 * It contains details about the Qanary process graph, the question ID, the server host, the component being 
 * explained, and any explanations generated.
 */
public class QanaryExplanationData {

    private String graph;
    private String questionId;
    private String serverHost;
    private String component;
    private Map<String,String> explanations;

    /**
     * Default constructor for creating an empty instance of {@link QanaryExplanationData}.
     */
    public QanaryExplanationData() {

    }

    /**
     * Constructor for creating an instance of {@link QanaryExplanationData} with the specified Qanary process graph, question ID, and server host.
     *
     * @param graph      the graph representing the QA process
     * @param questionId the identifier for the question being explained
     * @param serverHost the host of the server processing the explanation request
     */
    public QanaryExplanationData(String graph, String questionId, String serverHost) {
        this.graph = graph;
        this.questionId = questionId;
        this.serverHost = serverHost;
    }

    /**
     * Gets the component being explained.
     *
     * @return the component being explained
     */
    public String getComponent() {
        return component;
    }

    /**
     * Sets the component being explained.
     *
     * @param component the component to set
     */
    public void setComponent(String component) {
        this.component = component;
    }

    /**
     * Gets the explanations generated for the component.
     *
     * @return a map of explanations, where keys are explanation types and values are explanations
     */
    public Map<String, String> getExplanations() {
        return explanations;
    }

    /**
     * Sets the explanations generated for the component.
     *
     * @param explanations a map of explanations, where keys are explanation types and values are explanations
     */
    public void setExplanations(Map<String, String> explanations) {
        this.explanations = explanations;
    }

    /**
     * Gets the graph representing the Qanary process.
     *
     * @return the graph representing the Qanary process 
     */
    public String getGraph() {
        return graph;
    }

    /**
     * Sets the graph representing the QA process.
     *
     * @param graph the graph to set
     */
    public void setGraph(String graph) {
        this.graph = graph;
    }

    /**
     * Gets the identifier for the question being explained.
     *
     * @return the question ID
     */
    public String getQuestionId() {
        return questionId;
    }

    /**
     * Sets the identifier for the question being explained.
     *
     * @param questionId the question ID to set
     */
    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    /**
     * Gets the host of the server processing the explanation request.
     *
     * @return the server host
     */
    public String getServerHost() {
        return serverHost;
    }

    /**
     * Sets the host of the server processing the explanation request.
     *
     * @param serverHost the server host to set
     */
    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

}
