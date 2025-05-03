package eu.wdaqua.qanary.communications;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * a specialized RestTemplate that is caching the results, s.t., several equal
 * requests to a Web service are accelerated
 * <p>
 * see component CacheConfig for configuring the behavior of the cache
 */

public class RestTemplateWithCaching extends RestTemplate {

    public RestTemplateWithCaching(CacheOfRestTemplateResponse myCacheResponse, String restTemplateSetting) {
        List<ClientHttpRequestInterceptor> interceptors = this.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<>();
        } // Add caching interceptor only if caching is wanted, otherwise this class is only used to extend RestTemplateWithProcessId
        if (Objects.equals(restTemplateSetting, "both") || Objects.equals(restTemplateSetting, "caching")) {
            interceptors.add(new RestTemplateCacheResponseInterceptor(myCacheResponse)); // TODO: Only on property
            this.setInterceptors(interceptors);
        } else {
            this.setInterceptors(interceptors);
        }
        logger.warn(this.getClass().getCanonicalName() + " was initialized"); // old style logger from RestTemplate
    }
}
