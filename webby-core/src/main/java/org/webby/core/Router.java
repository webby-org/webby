package org.webby.core;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple router that dispatches requests based on the HTTP method and normalized path.
 */
public final class Router implements RequestHandler {
    private final Map<HttpMethod, RouteNode> routes = new ConcurrentHashMap<>();
    private volatile RequestHandler notFoundHandler = request -> Response.text(HttpStatus.NOT_FOUND, "Not Found");

    /**
     * Creates an empty router with a default {@code 404 Not Found} handler.
     */
    public Router() {
    }

    /**
     * Registers a handler for the provided method and path combo.
     *
     * @param method HTTP method such as {@code GET}
     * @param path path beginning with a slash (e.g. {@code /hello})
     * @param handler handler that produces a response for the route
     * @return the current router to enable chaining
     */
    public Router route(HttpMethod method, String path, RequestHandler handler) {
        HttpMethod normalizedMethod = Objects.requireNonNull(method, "method");
        String normalizedPath = normalizePath(Objects.requireNonNull(path, "path"));
        RouteNode node = routes.computeIfAbsent(normalizedMethod, key -> new RouteNode());
        for (String segment : split(normalizedPath)) {
            node = node.child(segment);
        }
        node.handler = Objects.requireNonNull(handler, "handler");
        return this;
    }

    /**
     * Convenience for registering a {@code GET} handler.
     *
     * @param path path to bind
     * @param handler route handler
     * @return current router
     */
    public Router get(String path, RequestHandler handler) {
        return route(HttpMethod.GET, path, handler);
    }

    /**
     * Convenience for registering a {@code POST} handler.
     *
     * @param path path to bind
     * @param handler route handler
     * @return current router
     */
    public Router post(String path, RequestHandler handler) {
        return route(HttpMethod.POST, path, handler);
    }

    /**
     * Convenience for registering a {@code PUT} handler.
     *
     * @param path path to bind
     * @param handler route handler
     * @return current router
     */
    public Router put(String path, RequestHandler handler) {
        return route(HttpMethod.PUT, path, handler);
    }

    /**
     * Convenience for registering a {@code DELETE} handler.
     *
     * @param path path to bind
     * @param handler route handler
     * @return current router
     */
    public Router delete(String path, RequestHandler handler) {
        return route(HttpMethod.DELETE, path, handler);
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
        RouteNode node = routes.get(request.method());
        if (node == null) {
            return notFoundHandler.handle(request);
        }
        for (String segment : split(normalizePath(request.target()))) {
            node = node.next(segment);
            if (node == null) {
                return notFoundHandler.handle(request);
            }
        }
        RequestHandler handler = node.handler;
        return handler == null ? notFoundHandler.handle(request) : handler.handle(request);
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

    private static String[] split(String path) {
        if ("/".equals(path)) {
            return new String[0];
        }
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        return trimmed.isEmpty() ? new String[0] : trimmed.split("/");
    }

    private static final class RouteNode {
        private final Map<String, RouteNode> children = new ConcurrentHashMap<>();
        private volatile RequestHandler handler;

        RouteNode child(String token) {
            return children.computeIfAbsent(token, key -> new RouteNode());
        }

        RouteNode next(String token) {
            return children.get(token);
        }
    }
}
