package eu.wdaqua.qanary.exceptions;

import java.net.URI;

public class TripleStoreNotProvided extends Exception {
	public TripleStoreNotProvided(URI triplestore) {
		super("" //
				+ "No triplestore provided: " //
				+ (triplestore == null ? "null" : triplestore.toASCIIString()) //
				+ " (check application.properties or command line parameters)." //
		);
	}
}
