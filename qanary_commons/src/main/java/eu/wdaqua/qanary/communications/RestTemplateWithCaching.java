package eu.wdaqua.qanary.communications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Service;
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

@Service
@ConditionalOnMissingBean(RestTemplateWithProcessId.class) // RestTemplateWithProcessId extends this RestTemplate
public class RestTemplateWithCaching extends RestTemplate {

    public RestTemplateWithCaching(CacheOfRestTemplateResponse myCacheResponse, @Value("${rest.template.setting}") String restTemplateSetting) {
        List<ClientHttpRequestInterceptor> interceptors = this.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<>();
        } // A = Both, B = CacheRestTemplate
        if (Objects.equals(restTemplateSetting, "A") || Objects.equals(restTemplateSetting, "B") || restTemplateSetting == null) { // Null for default component behavior
            interceptors.add(new RestTemplateCacheResponseInterceptor(myCacheResponse)); // TODO: Only on property
            this.setInterceptors(interceptors);
        } else {
            this.setInterceptors(interceptors);
        }
        logger.warn(this.getClass().getCanonicalName() + " was initialized"); // old style logger from RestTemplate
    }
}
