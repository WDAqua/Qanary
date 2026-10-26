package eu.wdaqua.qanary.commons.triplestoreconnectors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateAction;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A minimal SPARQL 1.1 Protocol endpoint for tests: a JDK {@link HttpServer} on the
 * loopback interface, backed by an in-memory Jena {@link Dataset}. It answers
 * SELECT and ASK with SPARQL JSON results, CONSTRUCT with Turtle, executes
 * updates, and records every query and update it received, so that tests can
 * check what a connector actually sent over the wire. No network access beyond
 * 127.0.0.1 is involved.
 */
final class LocalSparqlEndpoint implements AutoCloseable {

    private final HttpServer server;
    private final Dataset dataset = DatasetFactory.create();
    private final List<String> queries = new CopyOnWriteArrayList<>();
    private final List<String> updates = new CopyOnWriteArrayList<>();
    private volatile boolean rejectUpdates;

    LocalSparqlEndpoint() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/sparql", this::handle);
        server.start();
    }

    /** the URL of the SPARQL service (queries and updates) */
    URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/sparql");
    }

    /** the data behind the endpoint, for arranging and checking test data */
    Dataset dataset() {
        return dataset;
    }

    /** every query received, in order */
    List<String> receivedQueries() {
        return queries;
    }

    /** every update received, in order (also rejected ones) */
    List<String> receivedUpdates() {
        return updates;
    }

    /** let the endpoint answer every following update with HTTP 500 */
    void rejectUpdates() {
        rejectUpdates = true;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            Map<String, String> params = parameters(exchange);
            if (params.containsKey("update")) {
                String update = params.get("update");
                updates.add(update);
                if (rejectUpdates) {
                    reply(exchange, 500, "text/plain", "updates are rejected".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                UpdateAction.parseExecute(update, dataset);
                reply(exchange, 200, "text/plain", new byte[0]);
            } else if (params.containsKey("query")) {
                queries.add(params.get("query"));
                answerQuery(exchange, params.get("query"));
            } else {
                reply(exchange, 400, "text/plain", "neither query nor update".getBytes(StandardCharsets.UTF_8));
            }
        } catch (RuntimeException e) {
            reply(exchange, 400, "text/plain", String.valueOf(e.getMessage()).getBytes(StandardCharsets.UTF_8));
        } finally {
            exchange.close();
        }
    }

    private void answerQuery(HttpExchange exchange, String text) throws IOException {
        Query query = QueryFactory.create(text);
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try (QueryExecution execution = QueryExecutionFactory.create(query, dataset)) {
            if (query.isSelectType()) {
                ResultSetFormatter.outputAsJSON(body, execution.execSelect());
                reply(exchange, 200, "application/sparql-results+json", body.toByteArray());
            } else if (query.isAskType()) {
                ResultSetFormatter.outputAsJSON(body, execution.execAsk());
                reply(exchange, 200, "application/sparql-results+json", body.toByteArray());
            } else {
                RDFDataMgr.write(body, execution.execConstruct(), Lang.TURTLE);
                reply(exchange, 200, "text/turtle", body.toByteArray());
            }
        }
    }

    /** the protocol parameters, from the query string, a form body or a direct body */
    private static Map<String, String> parameters(HttpExchange exchange) throws IOException {
        Map<String, String> params = new HashMap<>(decodeForm(exchange.getRequestURI().getRawQuery()));
        if ("POST".equals(exchange.getRequestMethod())) {
            String contentType = String.valueOf(exchange.getRequestHeaders().getFirst("Content-Type"));
            String body;
            try (InputStream in = exchange.getRequestBody()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (contentType.startsWith("application/sparql-update")) {
                params.put("update", body);
            } else if (contentType.startsWith("application/sparql-query")) {
                params.put("query", body);
            } else {
                params.putAll(decodeForm(body));
            }
        }
        return params;
    }

    private static Map<String, String> decodeForm(String form) {
        Map<String, String> params = new HashMap<>();
        if (form == null || form.isEmpty()) {
            return params;
        }
        for (String pair : form.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    private static void reply(HttpExchange exchange, int status, String contentType, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
    }
}
