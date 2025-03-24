package qa.commons;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;

@TestConfiguration
public class RestTemplateCacheLiveTestConfiguration {

	@Bean
	@Primary
	public CacheOfRestTemplateResponse cacheResponse() throws NoSuchMethodException, SecurityException {
		return new CacheOfRestTemplateResponse();
	}

	@Bean
	@Primary
	public RestTemplateWithCaching restTemplateWithCaching(CacheOfRestTemplateResponse myCacheResponse) {
		return new RestTemplateWithCaching(myCacheResponse, null);
	}

}
