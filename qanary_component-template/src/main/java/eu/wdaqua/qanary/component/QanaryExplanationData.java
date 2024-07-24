package eu.wdaqua.qanary.component;

public class QanaryExplanationData {

        private String graph;
        private String questionId;
        private String component;
        private String serverHost;

        public QanaryExplanationData() {

        }

        public QanaryExplanationData(String graph, String questionId, String component, String serverHost) {
            this.graph = graph;
            this.questionId = questionId;
            this.component = component;
            this.serverHost = serverHost;
        }

        public String getComponent() {
            return component;
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
