package eu.wdaqua.qanary.communications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(RestTemplateWithCaching.class);

    public RestTemplateWithCaching(CacheOfRestTemplateResponse myCacheResponse, String restTemplateSetting) {
        // buffer responses so they can be read multiple times (needed when caching)
        ClientHttpRequestFactory currentFactory = this.getRequestFactory();
        this.setRequestFactory(new BufferingClientHttpRequestFactory(currentFactory));

        List<ClientHttpRequestInterceptor> interceptors = this.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            LOGGER.debug("no interceptors found, creating new list");
            interceptors = new ArrayList<>();
        }

        // Add caching interceptor only if caching is wanted, otherwise this class is
        // only used to extend RestTemplateWithProcessId
        if (Objects.equals(restTemplateSetting, "both") || Objects.equals(restTemplateSetting, "caching")) {
            LOGGER.debug("adding RestTemplateCacheResponseInterceptor to interceptors: restTemplateSetting={}",
                    restTemplateSetting);
            interceptors.add(new RestTemplateCacheResponseInterceptor(myCacheResponse)); // TODO: Only on property
            this.setInterceptors(interceptors);
        } else {
            LOGGER.debug("no interceptors added, setting interceptors: restTemplateSetting={}", restTemplateSetting);
            this.setInterceptors(interceptors);
        }
        LOGGER.warn("{} was initialized", this.getClass().getCanonicalName());
    }
}
