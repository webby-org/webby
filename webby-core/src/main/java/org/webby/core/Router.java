package org.webby.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple router that dispatches requests based on the HTTP method and normalized path.
 */
public final class Router implements RequestHandler {
    private final Map<HttpMethod, RouteNode> routes;
    private final String[] baseSegments;
    private final Router root;
    private volatile RequestHandler notFoundHandler;

    /**
     * Creates an empty router with a default {@code 404 Not Found} handler.
     */
    public Router() {
        this(new ConcurrentHashMap<>(), new String[0], null);
    }

    private Router(Map<HttpMethod, RouteNode> routes, String[] baseSegments, Router root) {
        this.routes = routes;
        this.baseSegments = baseSegments;
        this.root = root == null ? this : root;
        if (root == null) {
            this.notFoundHandler = request -> Response.text(HttpStatus.NOT_FOUND, "Not Found");
        }
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
        for (String segment : baseSegments) {
            node = node.child(segment);
        }
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
        root.notFoundHandler = Objects.requireNonNull(handler, "handler");
        return this;
    }

    /**
     * Creates a nested router that automatically applies the supplied path prefix to all registered routes.
     *
     * @param path prefix where the sub-router should mount (e.g. {@code /api})
     * @return router scoped to the supplied path
     */
    public Router subRouterAtPath(String path) {
        String normalized = normalizePath(Objects.requireNonNull(path, "path"));
        if ("/".equals(normalized)) {
            return this;
        }
        String[] segments = split(normalized);
        if (segments.length == 0) {
            return this;
        }
        String[] combined = combineSegments(baseSegments, segments);
        return new Router(routes, combined, root);
    }

    @Override
    public Response handle(Request request) {
        RouteNode node = routes.get(request.method());
        if (node == null) {
            return root.notFoundHandler.handle(request);
        }
        Map<String, String> pathVariables = new LinkedHashMap<>();
        for (String segment : split(normalizePath(request.target()))) {
            RouteNode nextNode = node.next(segment, pathVariables);
            if (nextNode == null) {
                return root.notFoundHandler.handle(request);
            }
            node = nextNode;
        }
        RequestHandler handler = node.handler;
        if (handler == null) {
            return root.notFoundHandler.handle(request);
        }
        Request effectiveRequest = pathVariables.isEmpty() ? request : request.withPathVariables(pathVariables);
        return handler.handle(effectiveRequest);
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

    private static String[] combineSegments(String[] base, String[] addition) {
        String[] combined = new String[base.length + addition.length];
        System.arraycopy(base, 0, combined, 0, base.length);
        System.arraycopy(addition, 0, combined, base.length, addition.length);
        return combined;
    }

    private static final class RouteNode {
        private final Map<String, RouteNode> literals = new ConcurrentHashMap<>();
        private volatile RouteNode variableChild;
        private volatile String variableName;
        private volatile RequestHandler handler;

        RouteNode child(String token) {
            if (isVariableSegment(token)) {
                String name = extractVariableName(token);
                RouteNode child = variableChild;
                if (child == null) {
                    child = new RouteNode();
                    variableChild = child;
                }
                child.variableName = name;
                return child;
            }
            return literals.computeIfAbsent(token, key -> new RouteNode());
        }

        RouteNode next(String token, Map<String, String> variables) {
            RouteNode literal = literals.get(token);
            if (literal != null) {
                return literal;
            }
            RouteNode variable = variableChild;
            if (variable != null) {
                variables.put(variable.variableName, token);
                return variable;
            }
            return null;
        }
    }

    private static boolean isVariableSegment(String segment) {
        return segment.startsWith("{") && segment.endsWith("}") && segment.length() > 2;
    }

    private static String extractVariableName(String segment) {
        String name = segment.substring(1, segment.length() - 1);
        if (name.isBlank()) {
            throw new IllegalArgumentException("Path variable name must not be blank");
        }
        return name;
    }
}
