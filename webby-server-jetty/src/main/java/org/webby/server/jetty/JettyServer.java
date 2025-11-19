package org.webby.server.jetty;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.webby.core.*;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;

/**
 * Alternative server implementation backed by Jetty that still operates on Webby's {@link org.webby.core.Request} and
 * {@link Response} types.
 */
public final class JettyServer implements AbstractServer {
    private final int port;
    private RequestHandler requestHandler;
    private MiddlewareNode middlewareChain;
    private volatile Server jetty;
    private volatile ServerConnector connector;
    private SSLContext sslContext;

    /**
     * Creates a Jetty-backed server bound to the given port.
     *
     * @param port listening port ({@code 0} selects a random port)
     */
    public JettyServer(int port) {
        this.port = port;
    }

    private void throwIfRunning() {
        if (isRunning()) {
            throw new IllegalStateException("Server is running");
        }
    }

    /**
     * Sets the handler that processes requests.
     *
     * @param handler request handler
     */
    public void setRequestHandler(RequestHandler handler) {
        throwIfRunning();
        this.requestHandler = Objects.requireNonNull(handler, "handler");
    }

    /**
     * Installs middleware that wraps the terminal handler.
     *
     * @param middleware middleware to append
     */
    public void addMiddleware(RequestMiddleware middleware) {
        throwIfRunning();
        Objects.requireNonNull(middleware, "middleware");
        middlewareChain = MiddlewareNode.append(middlewareChain, middleware);
    }

    /**
     * Enables TLS support using the supplied context.
     *
     * @param sslContext SSL context configured with certificates
     */
    public void enableTls(SSLContext sslContext) {
        throwIfRunning();
        this.sslContext = Objects.requireNonNull(sslContext, "sslContext");
    }

    /**
     * Starts Jetty and blocks until {@link #stop()} is invoked.
     *
     * @throws Exception if Jetty fails to start
     */
    public void start() throws Exception {
        RequestHandler handler = this.requestHandler;
        if (handler == null) {
            throw new IllegalStateException("Request handler must be configured before starting");
        }
        RequestHandler finalHandler = middlewareChain == null ? handler : middlewareChain.wrap(handler);
        Server server = new Server();
        ServerConnector serverConnector = createConnector(server);
        server.setConnectors(new Connector[]{serverConnector});
        server.setHandler(new JettyHandler(finalHandler));
        this.jetty = server;
        this.connector = serverConnector;
        server.start();
        try {
            server.join();
        } finally {
            stop();
        }
    }

    private ServerConnector createConnector(Server server) {
        HttpConfiguration configuration = new HttpConfiguration();
        ServerConnector serverConnector;
        if (sslContext != null) {
            SslContextFactory.Server sslFactory = new SslContextFactory.Server();
            sslFactory.setSslContext(sslContext);
            serverConnector = new ServerConnector(server, sslFactory, new HttpConnectionFactory(configuration));
        } else {
            serverConnector = new ServerConnector(server, new HttpConnectionFactory(configuration));
        }
        serverConnector.setPort(port);
        return serverConnector;
    }

    /**
     * Stops the Jetty server.
     */
    public synchronized void stop() {
        Server activeServer = jetty;
        if (activeServer == null) {
            return;
        }
        try {
            activeServer.stop();
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop Jetty server", e);
        } finally {
            try {
                activeServer.destroy();
            } finally {
                jetty = null;
                connector = null;
            }
        }
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * Returns {@code true} when Jetty has been started.
     */
    public boolean isRunning() {
        Server activeServer = jetty;
        return activeServer != null && activeServer.isRunning();
    }

    /**
     * Returns the currently bound port. When {@code 0} was provided in the constructor this value reflects the port
     * chosen by the OS after startup.
     *
     * @return bound port or the requested port before startup
     */
    public int port() {
        ServerConnector activeConnector = connector;
        if (activeConnector == null) {
            return port;
        }
        int localPort = activeConnector.getLocalPort();
        return localPort <= 0 ? port : localPort;
    }

    private static final class MiddlewareNode {
        private final RequestMiddleware middleware;
        private final MiddlewareNode next;

        private MiddlewareNode(RequestMiddleware middleware, MiddlewareNode next) {
            this.middleware = middleware;
            this.next = next;
        }

        static MiddlewareNode append(MiddlewareNode chain, RequestMiddleware middleware) {
            if (chain == null) {
                return new MiddlewareNode(middleware, null);
            }
            return new MiddlewareNode(chain.middleware, append(chain.next, middleware));
        }

        RequestHandler wrap(RequestHandler terminal) {
            RequestHandler nextHandler = next == null ? terminal : next.wrap(terminal);
            return request -> middleware.handle(request, nextHandler);
        }
    }

    private static final class JettyHandler extends Handler.Abstract {
        private final RequestHandler handler;

        JettyHandler(RequestHandler handler) {
            this.handler = handler;
        }

        @Override
        public boolean handle(Request jettyRequest, Response jettyResponse, Callback callback) throws Exception {
            org.webby.core.Response serverResponse;
            try {
                serverResponse = invokeHandler(jettyRequest);
            } catch (Exception e) {
                serverResponse = org.webby.core.Response.text(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
            }
            try {
                writeResponse(jettyResponse, serverResponse, callback);
            } catch (Exception failure) {
                callback.failed(failure);
                throw failure;
            }
            return true;
        }

        private org.webby.core.Response invokeHandler(Request request) throws IOException {
            HttpMethod method = HttpMethod.fromToken(request.getMethod());
            if (method == null) {
                return org.webby.core.Response.text(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");
            }
            org.webby.core.Request webbyRequest = adaptRequest(request, method);
            org.webby.core.Response result = handler.handle(webbyRequest);
            return Objects.requireNonNullElseGet(result, () -> org.webby.core.Response.text(HttpStatus.NO_CONTENT, ""));
        }

        private static org.webby.core.Request adaptRequest(Request request, HttpMethod method) throws IOException {
            String rawTarget = request.getHttpURI().getPathQuery();
            Map<String, String> headers = extractHeaders(request.getHeaders());
            byte[] body = readBody(request);
            return new org.webby.core.Request(method, rawTarget, "HTTP/1.1", headers, body);
        }

        private static Map<String, String> extractHeaders(HttpFields fields) {
            Map<String, String> headers = new LinkedHashMap<>();
            for (HttpField field : fields) {
                headers.put(field.getName(), field.getValue());
            }
            return headers;
        }

        private static byte[] readBody(Request request) throws IOException {
            try (InputStream input = Request.asInputStream(request)) {
                return input.readAllBytes();
            }
        }

        private static void writeResponse(
                Response jettyResponse,
                org.webby.core.Response payload,
                Callback callback) {
            jettyResponse.setStatus(payload.statusCode());
            HttpFields.Mutable headers = jettyResponse.getHeaders();
            payload.headers().forEach(headers::put);
            if (!payload.headers().containsKey("Content-Type")) {
                headers.put("Content-Type", "text/plain; charset=UTF-8");
            }
            byte[] body = payload.body();
            headers.put("Content-Length", Integer.toString(body.length));
            ByteBuffer buffer = ByteBuffer.wrap(body);
            jettyResponse.write(true, buffer, callback);
        }
    }
}
