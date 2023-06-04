package eu.wdaqua.qanary.commons.config;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import org.apache.commons.cli.MissingArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * this component configures Caffeine the cache used by the component
 * RestTemplateWithCaching to provide a cache for Web service calls
 * 
 * the following configuration property is required (e.g., provided in the
 * application.properties of the intended Qanary component)
 * 
 * <pre>
		qanary.webservicecalls.cache.specs=maximumSize=XXX,expireAfterAccess=XXXs
 * </pre>
 * 
 * a specific configuration would be the following that is creating a cache of
 * 10.000 elements and is caching for 1 hour
 * 
 * <pre>
		qanary.webservicecalls.cache.specs=maximumSize=10000,expireAfterAccess=3600s
 * </pre>
 */
@Configuration
@EnableCaching
@SuppressWarnings("rawtypes")
public class CacheConfig {
	public static final String CACHENAME = "qanary.webservicecalls.cache";

	private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

	@Bean
	public Caffeine caffeineConfig() {
		return Caffeine.newBuilder().recordStats();
	}

	@SuppressWarnings("unchecked")
	@Bean
	public CacheManager cacheManager(Caffeine caffeine,
			@Value("${qanary.webservicecalls.cache.specs:}") String caffeineSpec) {
		if (caffeineSpec == null || caffeineSpec == "") {
			caffeineSpec = "maximumSize=1,expireAfterAccess=0s"; // default value
		}
		logger.info("cacheManager configuration: {}", caffeineSpec);
		CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
		caffeineCacheManager.setCaffeine(caffeine);
		caffeineCacheManager.setCacheSpecification(caffeineSpec);
		return caffeineCacheManager;
	}

	public static void testCache(
			RestTemplateWithCaching myRestTemplate,
			CacheOfRestTemplateResponse myCacheOfResponse,
			int testPort,
			long maxTimeSpanSeconds
	) throws MissingArgumentException, RuntimeException, URISyntaxException, InterruptedException {


		if (myRestTemplate == null) {
			throw new MissingArgumentException("myRestTemplate is null");
		}
		if (myCacheOfResponse == null) {
			throw new MissingArgumentException("myCacheOfResponse is null");
		}

		URI testServiceURL0 = new URI("http://localhost:" + testPort + "/");
		URI testServiceURL1 = new URI("http://localhost:" + testPort + "/component-description");

		long initialNumberOfRequests = myCacheOfResponse.getNumberOfExecutedRequests();

		callRestTemplateWithCaching(myRestTemplate, myCacheOfResponse, testServiceURL0, Cache.NOT_CACHED); // cache miss
		callRestTemplateWithCaching(myRestTemplate, myCacheOfResponse, testServiceURL0, Cache.CACHED); // cache hit
		callRestTemplateWithCaching(myRestTemplate, myCacheOfResponse, testServiceURL0, Cache.CACHED); // cache hit
		TimeUnit.SECONDS.sleep(maxTimeSpanSeconds + 5); // wait until it is too late for caching
		callRestTemplateWithCaching(myRestTemplate, myCacheOfResponse, testServiceURL0, Cache.NOT_CACHED); // cache miss: too long ago
		callRestTemplateWithCaching(myRestTemplate, myCacheOfResponse, testServiceURL0, Cache.CACHED); // cache hit
		callRestTemplateWithCaching(myRestTemplate, myCacheOfResponse, testServiceURL1, Cache.NOT_CACHED); // cache miss: different URI
		callRestTemplateWithCaching(myRestTemplate, myCacheOfResponse, testServiceURL0, Cache.CACHED); // cache hit
		callRestTemplateWithCaching(myRestTemplate, myCacheOfResponse, testServiceURL1, Cache.CACHED); // cache hit

		if (initialNumberOfRequests + 3 != myCacheOfResponse.getNumberOfExecutedRequests()) {
			logger.error("Cache is not working as expected, number of executed requests should be {}: {}", initialNumberOfRequests + 3, myCacheOfResponse.getNumberOfExecutedRequests());

			long num = initialNumberOfRequests + 3;
			throw new RuntimeException("number of executed requests should be " + num  + ": " + myCacheOfResponse.getNumberOfExecutedRequests());
		}
	}

	/**
	 * @param uri
	 * @param cacheStatus
	 * @throws URISyntaxException
	 */
	private static void callRestTemplateWithCaching(
			RestTemplateWithCaching myRestTemplate,
			CacheOfRestTemplateResponse myCacheOfResponse,
			URI uri,
			Cache cacheStatus
	) throws RuntimeException {
		long numberOfNewlyExecutedRequests = myCacheOfResponse.getNumberOfExecutedRequests();
		ResponseEntity<String> responseEntity = myRestTemplate.getForEntity(uri, String.class);
		numberOfNewlyExecutedRequests = myCacheOfResponse.getNumberOfExecutedRequests() - numberOfNewlyExecutedRequests;
		logger.info("numberOfExecutedRequest since last request: new={}, count={}, teststatus={}", //
				numberOfNewlyExecutedRequests, myCacheOfResponse.getNumberOfExecutedRequests(), cacheStatus);

		if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
			throw new RuntimeException("HTTP status code is not OK: " + responseEntity.getStatusCode());
		}

		switch (cacheStatus) {
			case NOT_CACHED:
				if (1 != numberOfNewlyExecutedRequests) {
					logger.error("Cache is not working as expected, number of newly executed requests should be 1: {}", numberOfNewlyExecutedRequests);
					throw new RuntimeException("Number of newly executed requests should be 1: " + numberOfNewlyExecutedRequests);
				}
				break;
			case CACHED:
				if (0 != numberOfNewlyExecutedRequests) {
					logger.error("Cache is not working as expected, number of newly executed requests should be 0: {}", numberOfNewlyExecutedRequests);
					throw new RuntimeException("Number of newly executed requests should be 0: " + numberOfNewlyExecutedRequests);
				}
				break;
			default:
				throw new RuntimeException("Test case misconfigured");
		}
	}

	private enum Cache {
		CACHED, NOT_CACHED
	}

}