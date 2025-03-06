package eu.wdaqua.qanary.communications;

import eu.wdaqua.qanary.explainability.annotations.LoggingAspectPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ExplainabilityRequestInterceptor implements ClientHttpRequestInterceptor {

    private Logger logger = LoggerFactory.getLogger(ExplainabilityRequestInterceptor.class);

    public ExplainabilityRequestInterceptor() {
        logger.info("ExplainabilityRequestInterceptor: initialized");
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String processId = LoggingAspectPipeline.getMethodStack().peek().toString();
        if (processId == null) {
            logger.error("Couldn't get processId from stack, skip logging");
            return execution.execute(request, body);
        }
        logger.info("ExplainabilityRequestInterceptor: processId: " + processId);
        request.getHeaders().add("processId", processId);
        return execution.execute(request, body);
    }
}
