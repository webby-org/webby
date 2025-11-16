package org.webby.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MiddlewareChainTest {
    @Test
    void middlewareChainsInvokeInRegistrationOrder() {
        List<String> invocations = new ArrayList<>();
        RequestHandler terminal = request -> {
            invocations.add("terminal");
            return Response.text(HttpStatus.OK, "done");
        };

        RequestMiddleware first = (request, next) -> {
            invocations.add("first");
            return next.handle(request);
        };
        RequestMiddleware second = (request, next) -> {
            invocations.add("second");
            return next.handle(request);
        };
        RequestMiddleware blocker = (request, next) -> {
            invocations.add("blocker");
            return Response.text(HttpStatus.FORBIDDEN, "blocked");
        };

        MiddlewareChain chain = MiddlewareChain.append(null, first);
        chain = MiddlewareChain.append(chain, second);
        chain = MiddlewareChain.append(chain, blocker);

        Response response = chain.wrap(terminal).handle(dummyRequest());

        assertEquals(List.of("first", "second", "blocker"), invocations);
        assertEquals(HttpStatus.FORBIDDEN.code(), response.statusCode());
    }

    private static Request dummyRequest() {
        return new Request(HttpMethod.GET, "/", "HTTP/1.1", Map.of(), null);
    }
}
