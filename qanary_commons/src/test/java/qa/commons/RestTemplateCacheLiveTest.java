package qa.commons;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;

import eu.wdaqua.qanary.commons.config.CacheConfig;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;

/**
 * Test is validating the correct functionality of RestTemplateWithCaching
 * (i.e., repeated requests to a Web service are cached)
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes=RestTemplateCacheLiveTest.class)
@ContextConfiguration(loader = AnnotationConfigWebContextLoader.class, classes = { CacheConfig.class })
@Import(RestTemplateCacheLiveTestConfiguration.class)
class RestTemplateCacheLiveTest {
	// time span for caching, tests wait this time span during the test runs
	protected final static int MAX_TIME_SPAN_SECONDS = 5;

	private Logger logger = LoggerFactory.getLogger(RestTemplateCacheLiveTest.class);

	private enum Cache {
		CACHED, NOTCACHED
	}

	@Autowired
	RestTemplateWithCaching restTemplate;

	@Autowired
	CacheOfRestTemplateResponse myCacheOfResponse;

	@Test
	void givenRestTemplate_whenRequested_thenLogAndModifyResponse()
			throws InterruptedException, URISyntaxException {

		// myCacheOfResponse = new CacheOfRestTemplateResponse();
		// restTemplate =  new RestTemplateWithCaching(myCacheOfResponse);
		assertNotNull(restTemplate);
		assertNotNull(myCacheOfResponse);

		LoginForm loginForm0 = new LoginForm("userName", "password");
		LoginForm loginForm1 = new LoginForm("userName2", "password2");

		assertEquals(0, myCacheOfResponse.getNumberOfExecutedRequests());

		callRestTemplateWithCaching(loginForm0, Cache.NOTCACHED); // cache miss
		callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
		callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
		TimeUnit.SECONDS.sleep(MAX_TIME_SPAN_SECONDS + 1); // wait until it is too late for caching
		callRestTemplateWithCaching(loginForm0, Cache.NOTCACHED); // cache miss: too long ago
		callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
		callRestTemplateWithCaching(loginForm1, Cache.NOTCACHED); // cache miss: different body
		callRestTemplateWithCaching(loginForm0, Cache.CACHED); // cache hit
		callRestTemplateWithCaching(loginForm1, Cache.CACHED); // cache hit

		assertEquals(3, myCacheOfResponse.getNumberOfExecutedRequests());

	}

	private void callRestTemplateWithCaching(LoginForm loginForm, Cache cacheStatus) throws URISyntaxException {
		URI TESTSERVICEURL = new URI("http://httpbin.org/post");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<LoginForm> requestEntity = new HttpEntity<LoginForm>(loginForm, headers);

		long numberOfNewlyExecutedRequests = myCacheOfResponse.getNumberOfExecutedRequests();
		ResponseEntity<String> responseEntity = restTemplate.postForEntity(TESTSERVICEURL, requestEntity, String.class);
		numberOfNewlyExecutedRequests = myCacheOfResponse.getNumberOfExecutedRequests()
				- numberOfNewlyExecutedRequests;
		logger.info("numberOfExecutedRequest since last request: new={}, count={}, teststatus={}", //
				numberOfNewlyExecutedRequests, myCacheOfResponse.getNumberOfExecutedRequests(), cacheStatus);

		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

		switch (cacheStatus) {
		case NOTCACHED:
			assertEquals(1, numberOfNewlyExecutedRequests);
			break;
		case CACHED:
			assertEquals(0, numberOfNewlyExecutedRequests);
			break;
		default:
			fail("Test case misconfigured");
			break;
		}
	}

	public class LoginForm {
		private String username;
		private String password;

		public LoginForm() {
		}

		public LoginForm(String username, String password) {
			super();
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}
}
