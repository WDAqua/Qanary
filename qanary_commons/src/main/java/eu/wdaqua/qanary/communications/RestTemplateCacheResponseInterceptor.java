package eu.wdaqua.qanary.communications;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;

/**
 * this ClientHttpRequestInterceptor is controlling the behavior of the
 * RestTemplateWithCaching component
 */
@Service
public class RestTemplateCacheResponseInterceptor implements ClientHttpRequestInterceptor {
	private Logger logger = LoggerFactory.getLogger(RestTemplateCacheResponseInterceptor.class);
	private CacheOfRestTemplateResponse myCacheResponse;

	public RestTemplateCacheResponseInterceptor(CacheOfRestTemplateResponse myCacheResponse) {
		this.myCacheResponse = myCacheResponse;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {

		int hashCode = Objects.hash( //
				request.getURI().hashCode(), //
				request.getMethodValue().hashCode(), //
				request.getHeaders().toString().hashCode(), //
				(new String(body, StandardCharsets.UTF_8)).hashCode() //
		);
		ClientHttpResponse response = myCacheResponse.getResponse(hashCode, request, body, execution);
		logger.debug("response received: {}", response);
		return response;
	}
}
