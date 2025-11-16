package org.webby.examples;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.webby.core.Request;
import org.webby.core.RequestHandler;
import org.webby.core.Server;

/** Utility helpers shared across the runnable examples. */
final class ExampleSupport {
    private ExampleSupport() {
    }

    static Server newServer(int port, RequestHandler handler) {
        Server server = new Server(port);
        server.setRequestHandler(handler);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutorService(executor);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        return server;
    }

    static int port(String envVar, int fallback) {
        String raw = System.getenv(envVar);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static Optional<String> queryParam(Request request, String key) {
        String target = request.target();
        int queryStart = target.indexOf('?');
        if (queryStart < 0 || queryStart == target.length() - 1) {
            return Optional.empty();
        }
        String query = target.substring(queryStart + 1);
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equal = pair.indexOf('=');
            String name = equal >= 0 ? pair.substring(0, equal) : pair;
            if (!name.equals(key)) {
                continue;
            }
            String value = equal >= 0 && equal < pair.length() - 1 ? pair.substring(equal + 1) : "";
            return Optional.of(URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return Optional.empty();
    }

    static String bodyAsString(Request request) {
        return new String(request.body(), StandardCharsets.UTF_8);
    }

    static String requiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + key + " must be set");
        }
        return value;
    }
}
