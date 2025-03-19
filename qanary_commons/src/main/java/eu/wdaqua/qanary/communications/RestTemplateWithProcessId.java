package eu.wdaqua.qanary.communications;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(value = "explainability_logging", matchIfMissing = true)
public class RestTemplateWithExplainability extends RestTemplateWithCaching {

    public RestTemplateWithExplainability() {
        super(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));

        List<ClientHttpRequestInterceptor> interceptors = this.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<>();
        }
        interceptors.add(new ExplainabilityRequestInterceptor());
        this.setInterceptors(interceptors);
        logger.warn(this.getClass().getCanonicalName() + " was initialized"); // old style logger from RestTemplate
    }

}
