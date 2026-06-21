package eu.wdaqua.qanary.communications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import eu.wdaqua.qanary.explainability.aspects.QanaryAspect;

class CommunicationsTest {

    @AfterEach
    void clearCallStack() {
        QanaryAspect.getCallStack().clear();
    }

    @Test
    void cacheOfRestTemplateResponseTracksExecutedRequestCount() throws IOException {
        CacheOfRestTemplateResponse cache = new CacheOfRestTemplateResponse();
        long before = cache.getNumberOfExecutedRequests();

        // a cache miss delegates to the execution and increments the counter
        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        byte[] body = new byte[] {1, 2, 3};
        when(execution.execute(eq(request), any())).thenReturn(response);

        ClientHttpResponse result = cache.getResponse(42, request, body, execution);
        assertSame(response, result);
        assertTrue(cache.getNumberOfExecutedRequests() > before);
    }

    @Test
    void cacheResponseInterceptorDelegatesToCache() throws IOException {
        CacheOfRestTemplateResponse cache = mock(CacheOfRestTemplateResponse.class);
        RestTemplateCacheResponseInterceptor interceptor = new RestTemplateCacheResponseInterceptor(cache);

        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(java.net.URI.create("http://localhost/x"));
        when(request.getMethod()).thenReturn(org.springframework.http.HttpMethod.GET);
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        byte[] body = "payload".getBytes();
        when(cache.getResponse(org.mockito.ArgumentMatchers.anyInt(), eq(request), eq(body), eq(execution)))
                .thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, body, execution);
        assertSame(response, result);
    }

    @Test
    void processIdTemplateRegistersExplainabilityInterceptor() {
        RestTemplateWithProcessId template = new RestTemplateWithProcessId("logging", null);
        assertFalse(template.getInterceptors().isEmpty());
        boolean hasExplainability = template.getInterceptors().stream()
                .anyMatch(i -> i instanceof ExplainabilityRequestInterceptor);
        assertTrue(hasExplainability);
    }

    @Test
    void restTemplateConfigurationProducesTemplatesForEachSetting() {
        RestTemplateConfiguration config = new RestTemplateConfiguration();
        assertNotNull(config.restTemplateWithProcessId());
        assertNotNull(config.cacheOfRestTemplateWithProcessId());
        assertNotNull(config.cacheOfRestTemplateWithCaching());
    }

    @Test
    void explainabilityInterceptorAddsProcessIdHeaderFromCallStack() throws IOException {
        QanaryAspect.getCallStack().push("process-42");

        ExplainabilityRequestInterceptor interceptor = new ExplainabilityRequestInterceptor();
        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        byte[] body = new byte[0];
        when(execution.execute(eq(request), any())).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        assertSame(response, result);
        assertEquals("process-42", headers.getFirst("processId"));
    }

    @Test
    void explainabilityInterceptorWithEmptyCallStackSkipsHeaderWithoutThrowing() throws IOException {
        // empty call stack (no surrounding Qanary process): must not throw
        // EmptyStackException; the request proceeds without the processId header
        QanaryAspect.getCallStack().clear();

        ExplainabilityRequestInterceptor interceptor = new ExplainabilityRequestInterceptor();
        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        byte[] body = new byte[0];
        when(execution.execute(eq(request), any())).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        assertSame(response, result);
        assertNull(headers.getFirst("processId"));
    }
}
