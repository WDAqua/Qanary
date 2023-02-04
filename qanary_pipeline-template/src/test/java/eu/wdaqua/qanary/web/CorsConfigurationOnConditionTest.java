package eu.wdaqua.qanary.web;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.env.Environment;
import org.yaml.snakeyaml.error.MissingEnvironmentVariableException;

import com.complexible.stardog.plan.eval.ExecutionException;

class CorsConfigurationOnConditionTest {
	private final Logger logger = LoggerFactory.getLogger(CorsConfigurationOnConditionTest.class);

	private enum ExpectedStatus {
		CREATED, EXCEPTION
	};

	@Autowired
	public Environment environment;

	@Test
	void testNoValuesGiven() {
		logger.debug(
				"in real-world execution this should not happen as the condition should prevent the component creation");
		System.clearProperty(CorsConfigurationOnCondition.DISABLE_ALL_RESTRICTIONS);
		System.clearProperty(CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN);
		System.clearProperty(CorsConfigurationOnCondition.ADD_ALLOWED_HEADER);
		System.clearProperty(CorsConfigurationOnCondition.ADD_ALLOWED_METHOD);
		System.clearProperty(CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN_PATTERN);
		System.clearProperty(CorsConfigurationOnCondition.ENDPOINT_PATTERN);
		this.getCreatedBean(ExpectedStatus.EXCEPTION);
	}

	@Test
	void testDisableAllRestrictions() {
		System.setProperty(CorsConfigurationOnCondition.DISABLE_ALL_RESTRICTIONS, "true");
		System.clearProperty(CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN);
		System.clearProperty(CorsConfigurationOnCondition.ADD_ALLOWED_HEADER);
		System.clearProperty(CorsConfigurationOnCondition.ADD_ALLOWED_METHOD);
		System.clearProperty(CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN_PATTERN);
		System.clearProperty(CorsConfigurationOnCondition.ENDPOINT_PATTERN);
		assertTrue(this.getCreatedBean(ExpectedStatus.CREATED).isAllCorsRestrictionsOff());
	}

	@Test
	void testAllParametersGivenWhichDisablesAllRestrictions() {
		System.setProperty(CorsConfigurationOnCondition.DISABLE_ALL_RESTRICTIONS, "true");
		System.setProperty(CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN, "*");
		System.setProperty(CorsConfigurationOnCondition.ADD_ALLOWED_HEADER, "*");
		System.setProperty(CorsConfigurationOnCondition.ADD_ALLOWED_METHOD, "*");
		System.setProperty(CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN_PATTERN, "*");
		System.setProperty(CorsConfigurationOnCondition.ENDPOINT_PATTERN, "/**");
		assertTrue(this.getCreatedBean(ExpectedStatus.CREATED).isAllCorsRestrictionsOff());
	}	
	
	@Test
	void testAllSpecificParametersUsed() {
		System.clearProperty(CorsConfigurationOnCondition.DISABLE_ALL_RESTRICTIONS);
		System.setProperty(CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN, "*");
		System.setProperty(CorsConfigurationOnCondition.ADD_ALLOWED_HEADER, "*");
		System.setProperty(CorsConfigurationOnCondition.ADD_ALLOWED_METHOD, "*");
		System.setProperty(CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN_PATTERN, "*");
		System.setProperty(CorsConfigurationOnCondition.ENDPOINT_PATTERN, "/**");
		this.getCreatedBean(ExpectedStatus.CREATED);
		assertFalse(this.getCreatedBean(ExpectedStatus.CREATED).isAllCorsRestrictionsOff());
	}

	@Test
	void testSomeParametersNotUsed() {
		logger.debug(
				"in real-world execution this should not happen as the condition should prevent the component creation");
		System.clearProperty(CorsConfigurationOnCondition.DISABLE_ALL_RESTRICTIONS);
		System.clearProperty(CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN);
		System.setProperty(CorsConfigurationOnCondition.ADD_ALLOWED_HEADER, "*");
		System.setProperty(CorsConfigurationOnCondition.ADD_ALLOWED_METHOD, "*");
		System.setProperty(CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN_PATTERN, "*");
		System.setProperty(CorsConfigurationOnCondition.ENDPOINT_PATTERN, "/**");
		this.getCreatedBean(ExpectedStatus.EXCEPTION);
	}

	private CorsConfigurationOnCondition getCreatedBean(ExpectedStatus expectedStatus) {

		CorsConfigurationOnCondition myCorsConfigurationOnCondition = new CorsConfigurationOnCondition(
				Boolean.parseBoolean(
						System.getProperty(CorsConfigurationOnCondition.DISABLE_ALL_RESTRICTIONS, "false")),
				System.getProperty(CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN, null),
				System.getProperty(CorsConfigurationOnCondition.ADD_ALLOWED_HEADER, null),
				System.getProperty(CorsConfigurationOnCondition.ADD_ALLOWED_METHOD, null),
				System.getProperty(CorsConfigurationOnCondition.ADD_ALLOWED_ORIGIN_PATTERN, null),
				System.getProperty(CorsConfigurationOnCondition.ENDPOINT_PATTERN, null));

		switch (expectedStatus) {

		case CREATED:
			FilterRegistrationBean<?> bean = myCorsConfigurationOnCondition.corsConfigurationSource();
			assertNotNull(bean);
			return myCorsConfigurationOnCondition;

		case EXCEPTION:
			assertThrows(MissingEnvironmentVariableException.class, () -> {
				myCorsConfigurationOnCondition.corsConfigurationSource();
			}, "Expected myCorsConfigurationOnCondition.corsConfigurationSource() to throw, but it didn't");
			return null;

		default:
			throw new ExecutionException("missing parameter, this should not happen");

		}
	}
}
