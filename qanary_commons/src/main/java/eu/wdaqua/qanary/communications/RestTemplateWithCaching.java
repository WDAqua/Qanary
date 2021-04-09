package eu.wdaqua.qanary.communications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

/**
 * a specialized RestTemplate that is caching the results, s.t., several equal
 * requests to a Web service are accelerated
 * 
 * see component CacheConfig for configuring the behavior of the cache
 */
public class RestTemplateWithCaching extends RestTemplate {

	public RestTemplateWithCaching(CacheOfRestTemplateResponse myCacheResponse) {
		super(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));

		List<ClientHttpRequestInterceptor> interceptors = this.getInterceptors();
		if (CollectionUtils.isEmpty(interceptors)) {
			interceptors = new ArrayList<>();
		}
		interceptors.add(new RestTemplateCacheResponseInterceptor(myCacheResponse));
		this.setInterceptors(interceptors);
		logger.warn(this.getClass().getCanonicalName() + " was initialized"); // old style logger from RestTemplate
	}
}
