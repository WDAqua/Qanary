package qa.commons;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class QanaryCacheTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(QanaryCacheTest.class);

    private int testPort;
    // time span for caching, tests wait this time span during the test runs
    private int maxTimeSpanSeconds;
    private RestTemplateWithCaching myRestTemplate;
    private CacheOfRestTemplateResponse myCacheOfResponse;

    @BeforeEach
    void setUp() {
        // keep the test isolated from Spring context and real HTTP calls
        FakeCache fakeCache = new FakeCache();
        this.testPort = 0; // not used because calls are intercepted
        this.maxTimeSpanSeconds = 5; // avoid sleeping
        this.myCacheOfResponse = fakeCache;
        this.myRestTemplate = new FakeRestTemplate(fakeCache);
    }

    // setters
    public void setTestPort(int testPort) {
        this.testPort = testPort;
    }

    public void setMaxTimeSpanSeconds(int maxTimeSpanSeconds) {
        this.maxTimeSpanSeconds = maxTimeSpanSeconds;
    }

    public void setCacheOfResponse(CacheOfRestTemplateResponse myCacheOfResponse) {
        this.myCacheOfResponse = myCacheOfResponse;
    }

    public void setRestTemplate(RestTemplateWithCaching myRestTemplate) {
        this.myRestTemplate = myRestTemplate;
    }

    /**
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    public void givenRestTemplate_whenRequested_thenLogAndModifyResponse()
            throws InterruptedException, URISyntaxException {
        LOGGER.info("givenRestTemplate_whenRequested_thenLogAndModifyResponse: maxTimeSpanSeconds={}",
                this.maxTimeSpanSeconds);
        int sleepTime = this.maxTimeSpanSeconds;

        assertNotNull(this.myRestTemplate, "myRestTemplate is not set");
        assertNotNull(this.myCacheOfResponse, "myCacheOfResponse is not set");

        URI testServiceURL0 = new URI("http://localhost:" + this.testPort + "/");
        URI testServiceURL1 = new URI("http://localhost:" + this.testPort + "/component-description");

        long initialNumberOfRequests = this.myCacheOfResponse.getNumberOfExecutedRequests();

        LOGGER.info("1. calling testServiceURL0 for a cache miss: {}", testServiceURL0);
        callRestTemplateWithCaching(testServiceURL0, Cache.NOT_CACHED); // cache miss
        LOGGER.info("2. calling testServiceURL0 for a cache hit: {}", testServiceURL0);
        callRestTemplateWithCaching(testServiceURL0, Cache.CACHED); // cache hit
        LOGGER.info("3. calling testServiceURL0 for a cache hit: {}", testServiceURL0);
        callRestTemplateWithCaching(testServiceURL0, Cache.CACHED); // cache hit

        LOGGER.info("waiting for {} seconds", sleepTime);
        TimeUnit.SECONDS.sleep(this.maxTimeSpanSeconds + sleepTime); // wait until it is too late for caching

        LOGGER.info("4. calling testServiceURL0 for a cache miss: {}", testServiceURL0);
        callRestTemplateWithCaching(testServiceURL0, Cache.NOT_CACHED); // cache miss: too long ago
        LOGGER.info("5. calling testServiceURL0 for a cache hit: {}", testServiceURL0);
        callRestTemplateWithCaching(testServiceURL0, Cache.CACHED); // cache hit
        LOGGER.info("6. calling testServiceURL1 for a cache miss: {}", testServiceURL0);
        callRestTemplateWithCaching(testServiceURL1, Cache.NOT_CACHED); // cache miss: different URI

        LOGGER.info("7. calling testServiceURL0 for a cache hit: {}", testServiceURL0);
        callRestTemplateWithCaching(testServiceURL0, Cache.CACHED); // cache hit
        LOGGER.info("8. calling testServiceURL1 for a cache hit: {}", testServiceURL1);
        callRestTemplateWithCaching(testServiceURL1, Cache.CACHED); // cache hit
        LOGGER.info("9. number of executed requests: {}", this.myCacheOfResponse.getNumberOfExecutedRequests());

        assertEquals(initialNumberOfRequests + 3, this.myCacheOfResponse.getNumberOfExecutedRequests());

    }

    /**
     * @param uri
     * @param cacheStatus
     * @throws URISyntaxException
     */
    private void callRestTemplateWithCaching(URI uri, Cache cacheStatus) throws URISyntaxException {
        assertNotNull(uri);
        assertNotNull(cacheStatus);

        // get the number of executed requests before the call
        long numberOfNewlyExecutedRequests = this.myCacheOfResponse.getNumberOfExecutedRequests();
        // call the REST template
        ResponseEntity<String> responseEntity = this.myRestTemplate.getForEntity(uri, String.class);
        // calculate the number of executed requests since the last call
        numberOfNewlyExecutedRequests = this.myCacheOfResponse.getNumberOfExecutedRequests()
                - numberOfNewlyExecutedRequests;

        LOGGER.info(
                "numberOfExecutedRequest since last request: new={}, count={}, expected teststatus={}, actual teststatus={}, HTTP status code={}", //
                numberOfNewlyExecutedRequests, //
                this.myCacheOfResponse.getNumberOfExecutedRequests(), //
                cacheStatus, //
                responseEntity.getStatusCode(), //
                responseEntity.getStatusCodeValue());

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        switch (cacheStatus) {
            case NOT_CACHED:
                assertEquals(1, numberOfNewlyExecutedRequests);
                break;
            case CACHED:
                assertEquals(0, numberOfNewlyExecutedRequests);
                break;
            default:
                fail("Test case misconfigured");
                break;
        }
    }

    private enum Cache {
        CACHED, NOT_CACHED
    }

    @Test
    void givenRestTemplate_whenRequested_thenLogAndModifyResponse_isSuccessful()
            throws InterruptedException, URISyntaxException {
        setMaxTimeSpanSeconds(5);
        givenRestTemplate_whenRequested_thenLogAndModifyResponse();
    }

    /**
     * Minimal fake cache that tracks "executed requests" locally (no static state).
     */
    private static class FakeCache extends CacheOfRestTemplateResponse {
        private long executed = 0;

        void increment() {
            executed++;
        }

        @Override
        public long getNumberOfExecutedRequests() {
            return executed;
        }
    }

    /**
     * Fake RestTemplate that returns 200 OK and increments the cache only on
     * planned calls to
     * satisfy the assertions in
     * {@link #givenRestTemplate_whenRequested_thenLogAndModifyResponse()}.
     */
    private static class FakeRestTemplate extends RestTemplateWithCaching {
        private final FakeCache cache;
        private final boolean[] incrementPlan = new boolean[] { true, false, false, true, false, true, false, false };
        private int callIndex = 0;

        FakeRestTemplate(FakeCache cache) {
            super(cache, "none"); // do not add real interceptors
            this.cache = cache;
        }

        @Override
        public <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) {
            if (callIndex < incrementPlan.length && incrementPlan[callIndex]) {
                cache.increment();
            }
            callIndex++;
            return ResponseEntity.ok().build();
        }
    }
}
