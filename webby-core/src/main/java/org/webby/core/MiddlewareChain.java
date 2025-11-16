package org.webby.core;

final class MiddlewareChain {
    private final RequestMiddleware head;
    private final MiddlewareChain tail;

    private MiddlewareChain(RequestMiddleware head, MiddlewareChain tail) {
        this.head = head;
        this.tail = tail;
    }

    static MiddlewareChain append(MiddlewareChain chain, RequestMiddleware middleware) {
        if (chain == null) {
            return new MiddlewareChain(middleware, null);
        }
        return new MiddlewareChain(chain.head, append(chain.tail, middleware));
    }

    RequestHandler wrap(RequestHandler terminal) {
        if (head == null) {
            return terminal;
        }
        RequestHandler next = tail == null ? terminal : tail.wrap(terminal);
        return request -> head.handle(request, next);
    }
}
