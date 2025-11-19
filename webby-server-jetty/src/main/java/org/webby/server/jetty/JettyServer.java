package org.webby.server.jetty;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.Closeable;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.webby.core.HttpMethod;
import org.webby.core.HttpStatus;
import org.webby.core.RequestHandler;
import org.webby.core.RequestMiddleware;
import org.webby.core.Response;

/**
 * Alternative server implementation backed by Jetty that still operates on Webby's {@link org.webby.core.Request} and
 * {@link Response} types.
 */
public final class JettyServer implements Closeable {
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

    private static final class JettyHandler extends AbstractHandler {
        private final RequestHandler handler;

        JettyHandler(RequestHandler handler) {
            this.handler = handler;
        }

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException {
            baseRequest.setHandled(true);
            Response serverResponse;
            try {
                serverResponse = invokeHandler(request);
            } catch (Exception e) {
                serverResponse = Response.text(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
            }
            writeResponse(response, serverResponse);
        }

        private Response invokeHandler(HttpServletRequest request) throws IOException {
            HttpMethod method = HttpMethod.fromToken(request.getMethod());
            if (method == null) {
                return Response.text(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");
            }
            org.webby.core.Request webbyRequest = adaptRequest(request, method);
            Response result = handler.handle(webbyRequest);
            return Objects.requireNonNullElseGet(result, () -> Response.text(HttpStatus.NO_CONTENT, ""));
        }

        private static org.webby.core.Request adaptRequest(HttpServletRequest request, HttpMethod method)
                throws IOException {
            String rawTarget = buildTarget(request);
            Map<String, String> headers = extractHeaders(request);
            byte[] body = request.getInputStream().readAllBytes();
            return new org.webby.core.Request(method, rawTarget, "HTTP/1.1", headers, body);
        }

        private static String buildTarget(HttpServletRequest request) {
            String uri = request.getRequestURI();
            String query = request.getQueryString();
            return query == null ? uri : uri + "?" + query;
        }

        private static Map<String, String> extractHeaders(HttpServletRequest request) {
            Map<String, String> headers = new LinkedHashMap<>();
            Enumeration<String> names = request.getHeaderNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                headers.put(name, request.getHeader(name));
            }
            return headers;
        }

        private static void writeResponse(HttpServletResponse response, Response payload) throws IOException {
            response.setStatus(payload.statusCode());
            payload.headers().forEach(response::setHeader);
            byte[] body = payload.body();
            if (body.length > 0) {
                response.getOutputStream().write(body);
            }
            if (!payload.headers().containsKey("Content-Type")) {
                response.setContentType("text/plain; charset=UTF-8");
            }
            response.setContentLength(body.length);
        }
    }
}
