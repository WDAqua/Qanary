package eu.wdaqua.qanary.component.config;

import org.springframework.stereotype.Component;

@Component
public class QanaryConfiguration {
	public static final String description = "/description";

	// TODO should move to commons package
	public static final String annotatequestion = "/annotatequestion";
	public static final String sparql = "/sparql";
	public static final String questionRawDataUrlSuffix = "/raw";
}
