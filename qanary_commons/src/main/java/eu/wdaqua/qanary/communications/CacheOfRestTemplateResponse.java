package eu.wdaqua.qanary.communications;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.config.CacheConfig;

/**
 * cache implementation for RestTemplateWithCaching, it also provides the number
 * of actually executed requests (i.e., the number of requests that could not be
 * cached)
 */
@Component
public class CacheOfRestTemplateResponse {
	private static Logger logger = LoggerFactory.getLogger(CacheOfRestTemplateResponse.class);
	private static long numberOfExecutedRequests = 0;

	@Cacheable(value = CacheConfig.CACHENAME, key = "#hashCode")
	public ClientHttpResponse getResponse(int hashCode, HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		logger.info("CacheResponse.getResponse: cache miss --> send request, hascode={}", hashCode);
		CacheOfRestTemplateResponse.numberOfExecutedRequests++;
		return execution.execute(request, body);
	}

	public long getNumberOfExecutedRequests() {
		return CacheOfRestTemplateResponse.numberOfExecutedRequests;
	}
}
