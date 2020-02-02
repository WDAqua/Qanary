package eu.wdaqua.qanary.exceptions;

import java.net.URI;

public class TripleStoreNotProvided extends Exception {

	private static final long serialVersionUID = 3076968575465370941L;

	public TripleStoreNotProvided(URI triplestore) {
		super("" //
				+ "No triplestore provided: " //
				+ (triplestore == null ? "null" : triplestore.toASCIIString()) //
				+ " (check application.properties or command line parameters)." //
		);
	}

	public TripleStoreNotProvided(String triplestore) {
		super("" //
				+ "No triplestore provided: " //
				+ (triplestore == null ? "null" : triplestore) //
				+ " (check application.properties or command line parameters)." //
		);		
	}
}
