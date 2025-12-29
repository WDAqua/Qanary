package eu.wdaqua.qanary.communications;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This RestTemplate extends the RestTemplateWithCaching to provide either logging, caching or both via a RestTemplate.
 * It implements an interceptor to provide cross-component logging by appending the processId that leads to the cross-component
 * call to the API-Request header.
 * It can be used by setting the rest.template.setting to either both (caching and logging) or logging.
 * The creation of the correct bean is controlled by the RestTemplateConfiguration
 */
public class RestTemplateWithProcessId extends RestTemplateWithCaching {

    public RestTemplateWithProcessId(String restTemplateSetting, CacheOfRestTemplateResponse cacheOfRestTemplateResponse) {
        super(cacheOfRestTemplateResponse, restTemplateSetting);

        List<ClientHttpRequestInterceptor> interceptors = this.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<>();
        }
        interceptors.add(new ExplainabilityRequestInterceptor());
        this.setInterceptors(interceptors);
        logger.warn(this.getClass().getCanonicalName() + " was initialized"); // old style logger from RestTemplate
    }

}
