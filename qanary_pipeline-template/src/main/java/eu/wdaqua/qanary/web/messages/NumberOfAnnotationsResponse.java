package eu.wdaqua.qanary.web.messages;

public class NumberOfAnnotationsResponse {
	
	private String componentUrl;
	private int annotationCount;
	private String graph;
	private String sparqlGet;	

	public NumberOfAnnotationsResponse(String componentUrl, int annotationCount, String graph, String sparqlGet) {
		this.componentUrl = componentUrl;
		this.annotationCount = annotationCount;
		this.graph = graph;
		this.sparqlGet = sparqlGet;
	}

	public String getComponentUrl() {
		return componentUrl;
	}

	public void setComponentUrl(String componentUrl) {
		this.componentUrl = componentUrl;
	}

	public int getAnnotationCount() {
		return annotationCount;
	}

	public void setAnnotationCount(int annotationCount) {
		this.annotationCount = annotationCount;
	}

	public String getGraph() {
		return graph;
	}

	public void setGraph(String graph) {
		this.graph = graph;
	}

	public String getSparqlGet() {
		return sparqlGet;
	}

	public void setSparqlGet(String sparqlGet) {
		this.sparqlGet = sparqlGet;
	}

}
