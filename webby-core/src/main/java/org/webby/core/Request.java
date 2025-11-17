package org.webby.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of an HTTP request.
 *
 * @param method HTTP method
 * @param target requested path (e.g. {@code /health})
 * @param version protocol identifier (e.g. {@code HTTP/1.1})
 * @param headers collection of request headers; keys are treated case-insensitively
 * @param body raw payload bytes, if present
 * @param pathVariables variables captured from templated route segments
 */
public record Request(
        HttpMethod method,
        String target,
        String version,
        Map<String, String> headers,
        byte[] body,
        Map<String, String> pathVariables) {
    /**
     * Canonical constructor that defensively copies mutable input.
     *
     * @throws NullPointerException if {@code headers} is {@code null}
     */
    public Request {
        headers = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(headers, "headers")));
        body = body == null ? new byte[0] : body;
        if (pathVariables == null || pathVariables.isEmpty()) {
            pathVariables = Collections.emptyMap();
        } else {
            pathVariables = Collections.unmodifiableMap(new LinkedHashMap<>(pathVariables));
        }
    }

    /**
     * Convenience constructor when no path variables are associated with the request.
     *
     * @param method HTTP method
     * @param target request target/path
     * @param version HTTP version string
     * @param headers immutable view of request headers
     * @param body raw payload bytes, may be {@code null}
     */
    public Request(HttpMethod method, String target, String version, Map<String, String> headers, byte[] body) {
        this(method, target, version, headers, body, Collections.emptyMap());
    }

    /**
     * Returns the first header value matching the supplied name, ignoring case.
     *
     * @param name header name to look up
     * @return header value or {@code null} when absent
     */
    public String header(String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the captured value for a named path variable.
     *
     * @param name variable name declared in the router (without braces)
     * @return captured value or {@code null} if not present
     */
    public String getPathVariable(String name) {
        if (name == null || pathVariables.isEmpty()) {
            return null;
        }
        return pathVariables.get(name);
    }

    /**
     * Returns a copy of this request with the supplied path variables.
     *
     * @param variables variables captured during routing
     * @return new request instance storing the provided variables
     */
    public Request withPathVariables(Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return this;
        }
        return new Request(method, target, version, headers, body, variables);
    }
}
