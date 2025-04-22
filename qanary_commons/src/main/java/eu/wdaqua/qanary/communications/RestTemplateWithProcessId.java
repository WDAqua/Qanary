package eu.wdaqua.qanary.communications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * By default, this RestTemplate extension is disabled and only activated when its property is set to 'true'
 * It implements an interceptor to provide cross-component logging by appending the processId that leads to the cross-component
 * call to the API-Request header.
 */
@Service
@ConditionalOnExpression("'${rest.template.setting}' == 'A' or '${rest.template.setting}' == 'C'")
// A = Both, C = ProcessIdRestTemplate
public class RestTemplateWithProcessId extends RestTemplateWithCaching {

    public RestTemplateWithProcessId(@Value("${rest.template.setting}") String restTemplateSetting) {
        super(new CacheOfRestTemplateResponse(), restTemplateSetting);

        List<ClientHttpRequestInterceptor> interceptors = this.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<>();
        }
        interceptors.add(new ExplainabilityRequestInterceptor());
        this.setInterceptors(interceptors);
        logger.warn(this.getClass().getCanonicalName() + " was initialized"); // old style logger from RestTemplate
    }

}
