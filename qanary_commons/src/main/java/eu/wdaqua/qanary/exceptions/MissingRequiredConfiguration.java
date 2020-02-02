package eu.wdaqua.qanary.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * fatal exception cased by a missing configuration preventing a Qanary
 * component to start
 * 
 * @author AnBo
 *
 */
public class MissingRequiredConfiguration extends RuntimeException {
	private static final Logger logger = LoggerFactory.getLogger(MissingRequiredConfiguration.class);
	private static final long serialVersionUID = 1L;

	public MissingRequiredConfiguration(String key) {
		super("Configuration parameter '"+key+"' was not provided in environment.");
		logger.error("Configuration parameter '"+key+"' was not provided in environment.");
	}
}
