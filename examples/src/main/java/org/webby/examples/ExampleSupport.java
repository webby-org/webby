package org.webby.examples;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import org.webby.core.Request;
import org.webby.core.RequestHandler;
import org.webby.core.RequestMiddleware;
import org.webby.server.jetty.JettyServer;
import org.webby.server.netty.NettyServer;

/** Utility helpers shared across the runnable examples. */
final class ExampleSupport {
    private static final String TRANSPORT_ENV = "WEBBY_TRANSPORT";

    private ExampleSupport() {
    }

    static ExampleServer newServer(int port, RequestHandler handler) {
        Transport transport = resolveTransport();
        return switch (transport) {
            case JETTY -> wrap(new JettyServer(port), handler, transport);
            case NETTY -> wrap(new NettyServer(port), handler, transport);
        };
    }

    static Transport resolveTransport() {
        String raw = System.getenv(TRANSPORT_ENV);
        if (raw == null || raw.isBlank()) {
            return Transport.JETTY;
        }
        return raw.equalsIgnoreCase("netty") ? Transport.NETTY : Transport.JETTY;
    }

    private static ExampleServer wrap(JettyServer server, RequestHandler handler, Transport transport) {
        server.setRequestHandler(handler);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        return new ExampleServer() {
            @Override
            public void start() throws Exception {
                server.start();
            }

            @Override
            public void addMiddleware(RequestMiddleware middleware) {
                server.addMiddleware(middleware);
            }

            @Override
            public void enableTls(SSLContext context) {
                server.enableTls(context);
            }

            @Override
            public void close() {
                server.close();
            }

            @Override
            public Transport transport() {
                return transport;
            }
        };
    }

    private static ExampleServer wrap(NettyServer server, RequestHandler handler, Transport transport) {
        server.setRequestHandler(handler);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        return new ExampleServer() {
            @Override
            public void start() throws Exception {
                server.start();
            }

            @Override
            public void addMiddleware(RequestMiddleware middleware) {
                server.addMiddleware(middleware);
            }

            @Override
            public void enableTls(SSLContext context) {
                server.enableTls(context);
            }

            @Override
            public void close() {
                server.close();
            }

            @Override
            public Transport transport() {
                return transport;
            }
        };
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

    interface ExampleServer extends AutoCloseable {
        void start() throws Exception;

        void addMiddleware(RequestMiddleware middleware);

        void enableTls(SSLContext context);

        Transport transport();
    }

    enum Transport {
        JETTY("Jetty"),
        NETTY("Netty");

        private final String displayName;

        Transport(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return displayName;
        }
    }
}
