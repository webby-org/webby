package org.webby.core;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable HTTP response payload.
 *
 * @param status HTTP status code and reason phrase
 * @param headers headers to emit with the response
 * @param body raw response payload
 */
public record Response(HttpStatus status, Map<String, String> headers, byte[] body) {
    /**
     * Canonical constructor defensively copying the headers and body arrays.
     *
     * @throws NullPointerException if {@code status} or {@code headers} is {@code null}
     */
    public Response {
        headers = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(headers, "headers")));
        body = body == null ? new byte[0] : body;
    }

    /**
     * Creates a UTF-8 encoded text response and infers a reason phrase for the code.
     *
     * @param status HTTP status
     * @param body body content, may be {@code null}
     * @return response containing the supplied text
     */
    public static Response text(HttpStatus status, String body) {
        byte[] payload = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        return new Response(status, Collections.emptyMap(), payload);
    }

    /**
     * Returns the numeric HTTP status code associated with this response.
     *
     * @return numeric HTTP status code.
     */
    public int statusCode() {
        return status.code();
    }

    /**
     * Returns the RFC reason phrase provided by the {@link HttpStatus}.
     *
     * @return RFC reason phrase for the current status.
     */
    public String reasonPhrase() {
        return status.reasonPhrase();
    }
}
