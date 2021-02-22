package eu.wdaqua.qanary.commons.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

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

	Logger logger = LoggerFactory.getLogger(CacheConfig.class);

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

}