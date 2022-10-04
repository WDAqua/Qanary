package eu.wdaqua.qanary.web;

/**
 * provides a standard definition of common configuration paramters
 */
public interface QanaryConfigurationAccessParameters {
	final String ACCESSKEY = "configuration.access";
	final String USERNAMEKEY = "configuration.username";
	final String PASSWORDKEY = "configuration.password";
	final String WEBACCESS = "web";
	final String DISALLOWACCESS = "disallow";
	final String LOGINENDPOINT = "/login";
	final String CONFIGURATIONENDPOINT = "/configuration";
}
