package org.webby.core;

/**
 * Middleware that can inspect or short-circuit requests before they reach the terminal handler.
 */
@FunctionalInterface
public interface RequestMiddleware {
    /**
     * Processes an incoming request.
     *
     * @param request current request
     * @param next next middleware/handler in the chain
     * @return response to send to the client
     */
    Response handle(Request request, RequestHandler next);
}
