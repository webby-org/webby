package org.webby.core;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple router that dispatches requests based on the HTTP method and normalized path.
 */
public final class Router implements RequestHandler {
    private final Map<RouteKey, RequestHandler> routes = new ConcurrentHashMap<>();
    private volatile RequestHandler notFoundHandler = request -> Response.text(404, "Not Found");

    /**
     * Registers a handler for the provided method and path combo.
     *
     * @param method HTTP method such as {@code GET}
     * @param path path beginning with a slash (e.g. {@code /hello})
     * @param handler handler that produces a response for the route
     * @return the current router to enable chaining
     */
    public Router route(String method, String path, RequestHandler handler) {
        String normalizedMethod = normalizeMethod(Objects.requireNonNull(method, "method"));
        String normalizedPath = normalizePath(Objects.requireNonNull(path, "path"));
        routes.put(new RouteKey(normalizedMethod, normalizedPath), Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /** Convenience for registering a {@code GET} handler. */
    public Router get(String path, RequestHandler handler) {
        return route("GET", path, handler);
    }

    /** Convenience for registering a {@code POST} handler. */
    public Router post(String path, RequestHandler handler) {
        return route("POST", path, handler);
    }

    /** Convenience for registering a {@code PUT} handler. */
    public Router put(String path, RequestHandler handler) {
        return route("PUT", path, handler);
    }

    /** Convenience for registering a {@code DELETE} handler. */
    public Router delete(String path, RequestHandler handler) {
        return route("DELETE", path, handler);
    }

    /**
     * Sets the handler used when no matching route is found.
     *
     * @param handler handler invoked for misses
     * @return current router
     */
    public Router notFound(RequestHandler handler) {
        this.notFoundHandler = Objects.requireNonNull(handler, "handler");
        return this;
    }

    @Override
    public Response handle(Request request) {
        RequestHandler handler = routes.get(new RouteKey(normalizeMethod(request.method()), normalizePath(request.target())));
        if (handler == null) {
            return notFoundHandler.handle(request);
        }
        return handler.handle(request);
    }

    private static String normalizeMethod(String method) {
        return method == null ? "" : method.toUpperCase();
    }

    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        int query = normalized.indexOf('?');
        if (query >= 0) {
            return normalized.substring(0, query);
        }
        return normalized;
    }

    private record RouteKey(String method, String path) {
    }
}
