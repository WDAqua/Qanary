package eu.wdaqua.qanary.communications;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

/**
 * A component that does not answer must not block the pipeline forever if a read timeout is configured
 * (qanary.rest.read-timeout-ms).
 */
class RestTemplateTimeoutTest {

    @Test
    void readTimeoutEndsACallToAServiceThatDoesNotAnswer() throws Exception {
        try (ServerSocket silent = new ServerSocket(0)) {
            List<Socket> accepted = new ArrayList<>();
            Thread acceptor = new Thread(() -> {
                try {
                    accepted.add(silent.accept()); // accept, but never answer
                } catch (Exception ignored) {
                    // closed at the end of the test
                }
            });
            acceptor.start();
            RestTemplateWithCaching restTemplate = new RestTemplateWithCaching(null, "none", Duration.ofSeconds(2),
                    Duration.ofMillis(300));
            long start = System.nanoTime();
            assertThrows(ResourceAccessException.class,
                    () -> restTemplate.getForObject("http://127.0.0.1:" + silent.getLocalPort() + "/", String.class));
            assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 5000);
            for (Socket socket : accepted) {
                socket.close();
            }
        }
    }

    @Test
    void withoutTimeoutsTheTemplateIsCreatedAsBefore() {
        RestTemplateWithCaching restTemplate = new RestTemplateWithCaching(null, "none");
        assertTrue(restTemplate.getInterceptors().isEmpty());
    }
}
