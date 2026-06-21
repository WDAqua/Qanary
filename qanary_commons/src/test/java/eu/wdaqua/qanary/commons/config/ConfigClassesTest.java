package eu.wdaqua.qanary.commons.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import com.github.benmanes.caffeine.cache.Caffeine;

class ConfigClassesTest {

    @Test
    @SuppressWarnings("rawtypes")
    void cacheConfigBuildsCacheManagerWithExplicitSpec() {
        CacheConfig cacheConfig = new CacheConfig();
        Caffeine caffeine = cacheConfig.caffeineConfig();
        assertNotNull(caffeine);

        CacheManager manager = cacheConfig.cacheManager(caffeine, "maximumSize=10,expireAfterAccess=60s");
        assertNotNull(manager);
        // requesting a cache by the configured name materialises it
        assertNotNull(manager.getCache(CacheConfig.CACHENAME));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void cacheConfigFallsBackToDefaultSpecWhenNull() {
        CacheConfig cacheConfig = new CacheConfig();
        Caffeine caffeine = cacheConfig.caffeineConfig();
        CacheManager manager = cacheConfig.cacheManager(caffeine, null);
        assertNotNull(manager);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void cacheConfigFallsBackToDefaultSpecWhenBlank() {
        // a blank spec must use the default (the old 'spec == ""' identity check
        // was unreliable and could pass "" straight to Caffeine)
        CacheConfig cacheConfig = new CacheConfig();
        Caffeine caffeine = cacheConfig.caffeineConfig();
        CacheManager manager = cacheConfig.cacheManager(caffeine, "   ");
        assertNotNull(manager);
        assertNotNull(manager.getCache(CacheConfig.CACHENAME));
    }

    @Test
    void qanaryConfigurationServiceAndHostUriRoundTrip() {
        URI service = URI.create("http://localhost:8080/service");
        URI host = URI.create("http://localhost:8080");
        QanaryConfiguration.setServiceUri(service);
        QanaryConfiguration.setHostUri(host);
        assertEquals(service, QanaryConfiguration.getServiceUri());
        assertEquals(host, QanaryConfiguration.getHostUri());
        // a few of the path constants are stable parts of the Qanary protocol
        assertEquals("/sparql", QanaryConfiguration.sparql);
        assertEquals("urn:qanary#endpoint", QanaryConfiguration.endpointKey);
    }

    @Test
    void restClientConfigCanBeInstantiated() {
        // configuration class currently only declares beans (commented out);
        // ensure it remains instantiable as a Spring @Configuration
        assertNotNull(new RestClientConfig());
    }
}
