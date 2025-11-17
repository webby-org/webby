package org.webby.examples;

import java.time.Instant;
import org.webby.core.HttpStatus;
import org.webby.core.RequestMiddleware;
import org.webby.core.Response;
import org.webby.core.Router;
import org.webby.core.Server;

/** Demonstrates middleware for logging and simple auth. */
public final class MiddlewareExample {
    private MiddlewareExample() {
    }

    /**
     * Runs a server that demonstrates logging and auth middleware before reaching the terminal handler.
     *
     * @param args ignored
     * @throws Exception if the server cannot be started
     */
    public static void main(String[] args) throws Exception {
        int port = ExampleSupport.port("WEBBY_MIDDLEWARE_PORT", 8082);
        Router router = new Router()
                .get("/data", request -> Response.text(HttpStatus.OK, "sensitive content"))
                .notFound(request -> Response.text(HttpStatus.NOT_FOUND, "Try GET /data"));

        Server server = ExampleSupport.newServer(port, router);
        server.addMiddleware(loggingMiddleware());
        server.addMiddleware(authMiddleware());

        System.out.println("MiddlewareExample listening on http://localhost:" + port);
        server.start();
    }

    private static RequestMiddleware loggingMiddleware() {
        return (request, next) -> {
            Instant start = Instant.now();
            Response response = next.handle(request);
            long millis = Instant.now().toEpochMilli() - start.toEpochMilli();
            System.out.printf("%s %s -> %d (%dms)%n", request.method(), request.target(), response.statusCode(), millis);
            return response;
        };
    }

    private static RequestMiddleware authMiddleware() {
        return (request, next) -> {
            String token = request.header("X-Auth-Token");
            if (token == null || token.isBlank()) {
                return Response.text(HttpStatus.UNAUTHORIZED, "Provide X-Auth-Token header");
            }
            return next.handle(request);
        };
    }
}
