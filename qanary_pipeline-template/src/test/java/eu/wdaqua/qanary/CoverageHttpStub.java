package eu.wdaqua.qanary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Minimal HTTP server on a free loopback port for tests. It answers every
 * request with the response registered for the request path (404 otherwise)
 * and records the requests, so tests can check what a Qanary pipeline sends to
 * components or services without any external system.
 */
public class CoverageHttpStub implements AutoCloseable {

    /** a request received by the stub */
    public record RecordedRequest(String method, String path, String contentType, String accept, String body) {
    }

    private record Response(int status, String contentType, String body) {
    }

    private final HttpServer server;
    private final Map<String, Response> responses = new ConcurrentHashMap<>();
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();

    public CoverageHttpStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    /** answer requests to {@code path} with the given status, content type and body */
    public CoverageHttpStub respond(String path, int status, String contentType, String body) {
        responses.put(path, new Response(status, contentType, body));
        return this;
    }

    /** base URL without trailing slash, e.g. http://127.0.0.1:41234 */
    public String url() {
        return "http://127.0.0.1:" + port();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public List<RecordedRequest> requests() {
        return requests;
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String path = exchange.getRequestURI().getPath();
        requests.add(new RecordedRequest(exchange.getRequestMethod(), path,
                exchange.getRequestHeaders().getFirst("Content-Type"),
                exchange.getRequestHeaders().getFirst("Accept"), body));

        Response response = responses.getOrDefault(path, new Response(404, "text/plain", "not found: " + path));
        byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", response.contentType());
        exchange.sendResponseHeaders(response.status(), bytes.length == 0 ? -1 : bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
