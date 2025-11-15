package org.webby.core;

/**
 * Functional contract for server-side request handling.
 */
@FunctionalInterface
public interface RequestHandler {
    /**
     * Handles the provided request and returns a response.
     *
     * @param request request metadata and body
     * @return response to send back to the client, or {@code null} for no content
     */
    Response handle(Request request);
}
