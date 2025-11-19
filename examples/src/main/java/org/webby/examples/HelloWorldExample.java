package org.webby.examples;

import org.webby.core.HttpStatus;
import org.webby.core.Response;
import org.webby.core.Router;
import org.webby.examples.ExampleSupport.ExampleServer;

/** Demonstrates a minimal GET router. */
public final class HelloWorldExample {
    private HelloWorldExample() {
    }

    /**
     * Boots a minimal GET-only server exposing "/hello" and "/health".
     *
     * @param args ignored
     * @throws Exception if the server cannot be started
     */
    public static void main(String[] args) throws Exception {
        int port = ExampleSupport.port("WEBBY_HELLO_PORT", 8080);
        Router router = new Router()
                .get("/health", request -> Response.text(HttpStatus.OK, "OK"))
                .get("/hello", request -> {
                    String name = ExampleSupport.queryParam(request, "name").orElse("Webby");
                    return Response.text(HttpStatus.OK, "Hello " + name + "!");
                })
                .notFound(request -> Response.text(HttpStatus.NOT_FOUND, "Try /hello?name=Webby"));

        ExampleServer server = ExampleSupport.newServer(port, router);
        System.out.println("HelloWorldExample (" + server.transport().displayName() + ") listening on http://localhost:" + port);
        server.start();
    }
}
