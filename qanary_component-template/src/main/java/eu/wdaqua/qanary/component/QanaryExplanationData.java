package eu.wdaqua.qanary.component;

public class QanaryExplanationData {

        private String graph;
        private String questionId;
        private String component;
        private String serverHost;

        public QanaryExplanationData() {

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
