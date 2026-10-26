package eu.wdaqua.qanary.web;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Minimal HTTP server on 127.0.0.1 (ephemeral port) standing in for the web
 * resources a Qanary pipeline reads while answering a question:
 * <ul>
 * <li>{@code GET /question/{id}} and {@code GET /question/{id}/raw}: the stored
 * question (JSON pointing to the raw data) and its text, as served by
 * {@link QanaryQuestionController};</li>
 * <li>{@code POST /startquestionansweringwithtextquestion}: the pipeline's own
 * start endpoint as called by {@link QanaryGerbilController}; the received form
 * fields are recorded and a configurable JSON answer is returned.</li>
 * </ul>
 */
final class LocalQanaryHttpStub implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, String> questionTexts = new ConcurrentHashMap<>();
    private final List<Map<String, String>> receivedStartRequests = new CopyOnWriteArrayList<>();
    private volatile String startResponseJson = "{}";

    LocalQanaryHttpStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/question/", this::serveQuestion);
        server.createContext("/startquestionansweringwithtextquestion", this::serveStartRequest);
        server.start();
    }

    int port() {
        return server.getAddress().getPort();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + port();
    }

    /**
     * register a question text; returns the question URI the pipeline would create
     */
    URI addQuestion(String id, String text) {
        questionTexts.put(id, text);
        return URI.create(baseUrl() + "/question/" + id);
    }

    void respondToStartRequestsWith(String json) {
        this.startResponseJson = json;
    }

    List<Map<String, String>> receivedStartRequests() {
        return receivedStartRequests;
    }

    private void serveQuestion(HttpExchange exchange) throws IOException {
        String[] segments = exchange.getRequestURI().getPath().split("/");
        // "", "question", id[, "raw"]
        String id = segments.length > 2 ? segments[2] : "";
        String text = questionTexts.get(id);
        if (text == null) {
            reply(exchange, 404, "text/plain", "unknown question " + id);
        } else if (segments.length > 3 && "raw".equals(segments[3])) {
            reply(exchange, 200, "text/plain", text);
        } else {
            reply(exchange, 200, "application/json", "{\"raw\": \"" + baseUrl() + "/question/" + id + "/raw\"}");
        }
    }

    private void serveStartRequest(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            if (!pair.isEmpty()) {
                String[] keyValue = pair.split("=", 2);
                form.put(URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8),
                        keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "");
            }
        }
        receivedStartRequests.add(form);
        reply(exchange, 201, "application/json", startResponseJson);
    }

    private static void reply(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
