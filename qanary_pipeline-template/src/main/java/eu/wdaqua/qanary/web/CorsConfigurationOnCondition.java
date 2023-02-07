package eu.wdaqua.qanary.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.yaml.snakeyaml.error.MissingEnvironmentVariableException;

/**
 * the purpose of this class is to enable user to define the CORS behavior of
 * the Qanary system
 * 
 * The component is used under conditions of environment variables. Two options
 * exist:
 * 
 * 1) boolean cors.disableAllRestrictions will disable all CORS checks
 * 
 * 2) all the following environment String variables need to be defined:
 * 
 * <pre>
	cors.global.addAllowedOrigin
	cors.global.addAllowedHeader
	cors.global.addAllowedMethod
	cors.global.addAllowedOriginPattern
	cors.global.endpointPattern
 * </pre>
 * 
 * see below the standard values that are equal to removing all restrictions via
 * cors.disableAllRestrictions
 * 
 */
@ConditionalOnExpression("" //
		+ "${" + CorsConfigurationOnCondition.DISABLE_ALL_RESTRICTIONS + ":false}" //
		+ " or " //
		+ "(${" + CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN + ":null} != null " //
		+ " and ${" + CorsConfigurationOnCondition.ADD_ALLOWED_HEADER + ":null} != null " //
		+ " and ${" + CorsConfigurationOnCondition.ADD_ALLOWED_METHOD + ":null} != null " //
		+ " and ${" + CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN_PATTERN + ":null} != null" //
		+ " and ${" + CorsConfigurationOnCondition.ENDPOINT_PATTERN + ":null} != null" //
		+ ")")
@Component
public class CorsConfigurationOnCondition {
	private static final String CORS_GLOBAL = "cors.global.";
	public static final String DISABLE_ALL_RESTRICTIONS = CORS_GLOBAL + "disableAllRestrictions";
	public static final String ADD_ALLOWED_ORIGIN = CORS_GLOBAL + "addAllowedOrigin";
	public static final String ADD_ALLOWED_HEADER = CORS_GLOBAL + "addAllowedHeader";
	public static final String ADD_ALLOWED_METHOD = CORS_GLOBAL + "addAllowedMethod";
	public static final String ADD_ALLOWED_ORIGIN_PATTERN = CORS_GLOBAL + "addAllowedOriginPattern";
	public static final String ENDPOINT_PATTERN = CORS_GLOBAL + "endpointPattern";

	private final Logger logger = LoggerFactory.getLogger(CorsConfigurationOnCondition.class);
	private boolean disableAllCorsRestrictions;
	private String addAllowedOrigin;
	private String addAllowedHeader;
	private String addAllowedMethod;
	private String addAllowedOriginPattern;
	private String endpointPattern;
	private boolean isAllCorsRestrictionsOff;

	public CorsConfigurationOnCondition(
			@Value("${" + DISABLE_ALL_RESTRICTIONS + ":false}") boolean disableAllCorsRestrictions, //
			@Value("${" + ADD_ALLOWED_ORIGIN + ":null}") String addAllowedOrigin, //
			@Value("${" + ADD_ALLOWED_HEADER + ":null}") String addAllowedHeader, //
			@Value("${" + ADD_ALLOWED_METHOD + ":null}") String addAllowedMethod, //
			@Value("${" + ADD_ALLOWED_ORIGIN_PATTERN + ":null}") String addAllowedOriginPattern, //
			@Value("${" + ENDPOINT_PATTERN + ":null}") String endpointPattern //
	) {
		this.disableAllCorsRestrictions = disableAllCorsRestrictions;
		this.addAllowedOrigin = addAllowedOrigin;
		this.addAllowedHeader = addAllowedHeader;
		this.addAllowedMethod = addAllowedMethod;
		this.addAllowedOriginPattern = addAllowedOriginPattern;
		this.endpointPattern = endpointPattern;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Bean
	FilterRegistrationBean corsConfigurationSource( //
	) {
		String message = String.format("%s='%s' || (%s='%s', %s='%s', %s='%s', %s='%s' %s='%s').", //
				DISABLE_ALL_RESTRICTIONS, disableAllCorsRestrictions, //
				ADD_ALLOWED_ORIGIN, addAllowedOrigin, //
				ADD_ALLOWED_HEADER, addAllowedHeader, //
				ADD_ALLOWED_METHOD, addAllowedMethod, //
				ADD_ALLOWED_ORIGIN_PATTERN, addAllowedOriginPattern, //
				ENDPOINT_PATTERN, endpointPattern);
		if (disableAllCorsRestrictions || (addAllowedOrigin != null && addAllowedHeader != null
				&& addAllowedMethod != null && addAllowedOriginPattern != null && endpointPattern != null)) {
			logger.debug("custom CORS configuration available: {}", message);
		} else {
			throw new MissingEnvironmentVariableException("CORS configuration incomplete: " + message);
		}

		// allow everything if disableAllCorsRestrictions is true
		if (disableAllCorsRestrictions) {
			addAllowedOrigin = "*";
			addAllowedHeader = "*";
			addAllowedMethod = "*";
			addAllowedOriginPattern = "*";
			endpointPattern = "/**";
			this.switchAllCorsRestrictionsOff();
		}
		logger.warn("corsConfigurationSource: CORS updated: {}", message);
		CorsConfiguration configuration = new CorsConfiguration();
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		configuration.addAllowedOrigin(addAllowedOrigin);
		configuration.addAllowedHeader(addAllowedHeader);
		configuration.addAllowedMethod(addAllowedMethod);
		configuration.addAllowedOriginPattern(addAllowedOriginPattern);
		source.registerCorsConfiguration(endpointPattern, configuration);

		FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
		bean.setOrder(0);

		return bean;
	}

	private void switchAllCorsRestrictionsOff() {
		this.isAllCorsRestrictionsOff = true;
	}

	public boolean isAllCorsRestrictionsOff() {
		return isAllCorsRestrictionsOff;
	}
}
