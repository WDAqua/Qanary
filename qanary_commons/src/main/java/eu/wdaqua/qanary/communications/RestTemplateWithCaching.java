package eu.wdaqua.qanary.communications;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * a specialized RestTemplate that is caching the results, s.t., several equal
 * requests to a Web service are accelerated
 * <p>
 * see component CacheConfig for configuring the behavior of the cache
 */

@Service
@ConditionalOnMissingBean(RestTemplateWithProcessId.class)
@ConditionalOnProperty(value = "request_caching", havingValue = "true", matchIfMissing = false)
public class RestTemplateWithCaching extends RestTemplate {

    public RestTemplateWithCaching(CacheOfRestTemplateResponse myCacheResponse) {
        List<ClientHttpRequestInterceptor> interceptors = this.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<>();
        }
        interceptors.add(new RestTemplateCacheResponseInterceptor(myCacheResponse));
        this.setInterceptors(interceptors);
        logger.warn(this.getClass().getCanonicalName() + " was initialized"); // old style logger from RestTemplate
    }
}
