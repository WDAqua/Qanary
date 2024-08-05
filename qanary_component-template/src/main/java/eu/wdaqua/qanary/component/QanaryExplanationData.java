package eu.wdaqua.qanary.component;

import java.util.Map;

public class QanaryExplanationData {

        private String graph;
        private String questionId;
        private String serverHost;
        private String component;
        private Map<String,String> explanations;

        public QanaryExplanationData() {

        }

        public QanaryExplanationData(String graph, String questionId, String serverHost) {
            this.graph = graph;
            this.questionId = questionId;
            this.serverHost = serverHost;
        }

        public String getComponent() {
            return component;
        }

    public void setExplanations(Map<String, String> explanations) {
        this.explanations = explanations;
    }

    public Map<String, String> getExplanations() {
        return explanations;
    }

    public String getGraph() {
            return graph;
        }

        public String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String questionId) {
            this.questionId = questionId;
        }

        public void setComponent(String component) {
            this.component = component;
        }

        public void setGraph(String graph) {
            this.graph = graph;
        }

        public String getServerHost() {
            return serverHost;
        }

        public void setServerHost(String serverHost) {
            this.serverHost = serverHost;
        }

}
