package qa.commons;

import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.communications.RestTemplateWithCaching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class QanaryCacheTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(QanaryCacheTest.class);

    private final int testPort;
    // time span for caching, tests wait this time span during the test runs
    private final int maxTimeSpanSeconds;
    private final RestTemplateWithCaching myRestTemplate;
    private final CacheOfRestTemplateResponse myCacheOfResponse;

    public QanaryCacheTest(
            int testPort, //
            int maxTimeSpanSeconds, //
            @Autowired RestTemplateWithCaching myRestTemplate, //
            @Autowired CacheOfRestTemplateResponse myCacheOfResponse //
    ) {
        this.testPort = testPort;
        this.maxTimeSpanSeconds = maxTimeSpanSeconds;
        this.myRestTemplate = myRestTemplate;
        this.myCacheOfResponse = myCacheOfResponse;
    }

    /**
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    public void givenRestTemplate_whenRequested_thenLogAndModifyResponse() throws InterruptedException, URISyntaxException {

        assertNotNull(this.myRestTemplate);
        assertNotNull(this.myCacheOfResponse);

        URI testServiceURL0 = new URI("http://localhost:" + this.testPort + "/");
        URI testServiceURL1 = new URI("http://localhost:" + this.testPort + "/component-description");

        long initialNumberOfRequests = this.myCacheOfResponse.getNumberOfExecutedRequests();

        callRestTemplateWithCaching(testServiceURL0, Cache.NOT_CACHED); // cache miss
        callRestTemplateWithCaching(testServiceURL0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(testServiceURL0, Cache.CACHED); // cache hit

        TimeUnit.SECONDS.sleep(this.maxTimeSpanSeconds + 5); // wait until it is too late for caching

        callRestTemplateWithCaching(testServiceURL0, Cache.NOT_CACHED); // cache miss: too long ago
        callRestTemplateWithCaching(testServiceURL0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(testServiceURL1, Cache.NOT_CACHED); // cache miss: different URI
        callRestTemplateWithCaching(testServiceURL0, Cache.CACHED); // cache hit
        callRestTemplateWithCaching(testServiceURL1, Cache.CACHED); // cache hit

        assertEquals(initialNumberOfRequests + 3, this.myCacheOfResponse.getNumberOfExecutedRequests());

    }

    /**
     * @param uri
     * @param cacheStatus
     * @throws URISyntaxException
     */
    private void callRestTemplateWithCaching(URI uri, Cache cacheStatus) throws URISyntaxException {
        long numberOfNewlyExecutedRequests = this.myCacheOfResponse.getNumberOfExecutedRequests();
        ResponseEntity<String> responseEntity = this.myRestTemplate.getForEntity(uri, String.class);
        numberOfNewlyExecutedRequests = this.myCacheOfResponse.getNumberOfExecutedRequests() - numberOfNewlyExecutedRequests;
        this.LOGGER.info("numberOfExecutedRequest since last request: new={}, count={}, teststatus={}", //
                numberOfNewlyExecutedRequests, this.myCacheOfResponse.getNumberOfExecutedRequests(), cacheStatus);

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
}

