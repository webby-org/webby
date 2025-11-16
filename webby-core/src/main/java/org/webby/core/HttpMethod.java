package org.webby.core;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * HTTP methods defined by RFC 9110.
 */
public enum HttpMethod {
    /** Transfer a representation of the target resource. */
    GET,
    /** Same semantics as GET but without a response body. */
    HEAD,
    /** Submit an enclosed representation to be processed. */
    POST,
    /** Replace the target resource with the payload. */
    PUT,
    /** Remove the target resource. */
    DELETE,
    /** Establish a tunnel to the server identified by the target resource. */
    CONNECT,
    /** Describe communication options for the target resource. */
    OPTIONS,
    /** Perform a message loop-back test along the path. */
    TRACE,
    /** Apply partial modifications to the target resource. */
    PATCH;

    private static final Map<String, HttpMethod> LOOKUP = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(Enum::name, method -> method));

    /**
     * Attempts to resolve the supplied token to a known HTTP method.
     *
     * @param token case-insensitive HTTP method token
     * @return the matching method or {@code null} if the token is unknown
     */
    public static HttpMethod fromToken(String token) {
        if (token == null) {
            return null;
        }
        return LOOKUP.get(token.toUpperCase(Locale.ROOT));
    }
}
