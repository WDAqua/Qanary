package eu.wdaqua.qanary.business;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.net.URI;

import org.junit.jupiter.api.Test;

/**
 * The class is {@code @Deprecated} and most endpoint rewrites are commented out,
 * so every endpoint accessor currently returns the URI unchanged regardless of
 * the {@code stardog5} flag. These tests pin that behaviour for both flag states.
 */
class TriplestoreEndpointIdentifierTest {

    private TriplestoreEndpointIdentifier withStardog5(boolean value) throws Exception {
        TriplestoreEndpointIdentifier identifier = new TriplestoreEndpointIdentifier();
        Field f = TriplestoreEndpointIdentifier.class.getDeclaredField("stardog5");
        f.setAccessible(true);
        f.setBoolean(identifier, value);
        return identifier;
    }

    @Test
    void endpointsAreUnchangedWhenStardog5Disabled() throws Exception {
        TriplestoreEndpointIdentifier id = withStardog5(false);
        URI uri = URI.create("http://localhost:8890/sparql");
        assertEquals(uri, id.getSelectEndpoint(uri));
        assertEquals(uri, id.getAskEndpoint(uri));
        assertEquals(uri, id.getUpdateEndpoint(uri));
        assertEquals(uri, id.getCreateEndpoint(uri));
        assertEquals(uri, id.getLoadEndpoint(uri));
    }

    @Test
    void endpointsAreUnchangedWhenStardog5Enabled() throws Exception {
        TriplestoreEndpointIdentifier id = withStardog5(true);
        URI uri = URI.create("http://localhost:8890/sparql");
        assertEquals(uri, id.getSelectEndpoint(uri));
        assertEquals(uri, id.getAskEndpoint(uri));
        assertEquals(uri, id.getUpdateEndpoint(uri));
        assertEquals(uri, id.getCreateEndpoint(uri));
        assertEquals(uri, id.getLoadEndpoint(uri));
    }
}
