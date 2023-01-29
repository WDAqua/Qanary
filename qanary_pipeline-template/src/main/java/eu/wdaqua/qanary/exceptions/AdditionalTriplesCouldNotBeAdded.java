package eu.wdaqua.qanary.exceptions;

public class AdditionalTriplesCouldNotBeAdded extends Exception {

	private static final long serialVersionUID = -1360834613714974755L;

	public AdditionalTriplesCouldNotBeAdded(Exception e, String sparqlquery) {
		super("additionaltriples could not be added through the query:\n" + sparqlquery + "\n" + e.getMessage());
	}

}
