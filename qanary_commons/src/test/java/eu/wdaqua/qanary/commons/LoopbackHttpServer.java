package eu.wdaqua.qanary.commons;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

/**
 * Minimal HTTP server bound to the loopback interface on a free port. It lets
 * tests exercise code that fetches question data with a {@link
 * org.springframework.web.client.RestTemplate} without any external network
 * access: each registered path answers with a fixed body, and the requests it
 * received are recorded.
 */
final class LoopbackHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, String> lastRequestBodies = new ConcurrentHashMap<>();

    LoopbackHttpServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        this.server.start();
    }

    /**
     * answer every request to exactly this path with HTTP 200 and the given body
     * (an empty body is sent without content)
     */
    LoopbackHttpServer respond(String path, String contentType, byte[] body) {
        requestCounts.put(path, new AtomicInteger());
        server.createContext(path, exchange -> {
            if (!exchange.getRequestURI().getPath().equals(path)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            requestCounts.get(path).incrementAndGet();
            lastRequestBodies.put(path,
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.getResponseHeaders().add("Content-Type", contentType);
            if (body.length == 0) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        return this;
    }

    LoopbackHttpServer respond(String path, String contentType, String body) {
        return respond(path, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    /** base URL without trailing slash, e.g. http://127.0.0.1:41234 */
    String baseUrl() {
        return "http://" + server.getAddress().getAddress().getHostAddress() + ":" + server.getAddress().getPort();
    }

    String url(String path) {
        return baseUrl() + path;
    }

    int requestCount(String path) {
        return requestCounts.get(path).get();
    }

    String lastRequestBody(String path) {
        return lastRequestBodies.get(path);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
