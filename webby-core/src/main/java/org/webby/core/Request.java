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
 */
public record Request(HttpMethod method, String target, String version, Map<String, String> headers, byte[] body) {
    /**
     * Canonical constructor that defensively copies mutable input.
     *
     * @throws NullPointerException if {@code headers} is {@code null}
     */
    public Request {
        headers = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(headers, "headers")));
        body = body == null ? new byte[0] : body;
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
}
