package qa.commons;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;

@TestConfiguration
public class RestTemplateCacheLiveTestConfiguration {
	// define here the current CaffeineCacheManager configuration
	static {
		System.setProperty("qanary.webservicecalls.cache.specs",
				"maximumSize=1000,expireAfterAccess=" + RestTemplateCacheLiveTest.MAX_TIME_SPAN_SECONDS + "s");
	}

	@Bean
	@Primary
	public CacheOfRestTemplateResponse cacheResponse() throws NoSuchMethodException, SecurityException {
		return new CacheOfRestTemplateResponse();
	}

	@Bean
	@Primary
	public RestTemplateWithCaching restTemplateWithCaching(CacheOfRestTemplateResponse myCacheResponse) {
		return new RestTemplateWithCaching(myCacheResponse);
	}

}
