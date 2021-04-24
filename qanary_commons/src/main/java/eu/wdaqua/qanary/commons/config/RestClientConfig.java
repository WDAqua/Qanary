package eu.wdaqua.qanary.commons.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;

/**
 * provides beans of RestTemplateWithCaching and CacheOfRestTemplateResponse,
 * s.t., RestTemplateWithCaching is configured to use the cache
 */
@Configuration
public class RestClientConfig {

	Logger logger = LoggerFactory.getLogger(RestClientConfig.class);

	@Bean
	public CacheOfRestTemplateResponse cacheResponse() throws NoSuchMethodException, SecurityException {
		return new CacheOfRestTemplateResponse();
	}

	@Bean
	public RestTemplateWithCaching restTemplateWithCaching(CacheOfRestTemplateResponse myCacheResponse) {
		return new RestTemplateWithCaching(myCacheResponse);
	}

}
