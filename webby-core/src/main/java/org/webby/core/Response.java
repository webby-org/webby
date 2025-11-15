package org.webby.core;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable HTTP response payload.
 *
 * @param statusCode HTTP status code
 * @param reasonPhrase phrase paired with the status code
 * @param headers headers to emit with the response
 * @param body raw response payload
 */
public record Response(int statusCode, String reasonPhrase, Map<String, String> headers, byte[] body) {
    /**
     * Canonical constructor defensively copying the headers and body arrays.
     *
     * @throws NullPointerException if {@code headers} is {@code null}
     */
    public Response {
        headers = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(headers, "headers")));
        body = body == null ? new byte[0] : body;
    }

    /**
     * Creates a UTF-8 encoded text response and infers a reason phrase for the code.
     *
     * @param statusCode HTTP status code
     * @param body body content, may be {@code null}
     * @return response containing the supplied text
     */
    public static Response text(int statusCode, String body) {
        byte[] payload = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        return new Response(statusCode, defaultReason(statusCode), Collections.emptyMap(), payload);
    }

    private static String defaultReason(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 202 -> "Accepted";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "HTTP";
        };
    }
}
