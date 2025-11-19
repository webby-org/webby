package org.webby.examples;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.webby.core.HttpStatus;
import org.webby.core.Response;
import org.webby.core.Router;
import org.webby.examples.ExampleSupport.ExampleServer;

/** Demonstrates handling POST requests and reading the body. */
public final class EchoPostExample {
    private EchoPostExample() {
    }

    /**
     * Launches an echo server that reads POST payloads and responds with the body and content type.
     *
     * @param args ignored
     * @throws Exception if the server cannot be started
     */
    public static void main(String[] args) throws Exception {
        int port = ExampleSupport.port("WEBBY_ECHO_PORT", 8081);
        Router router = new Router()
                .post("/echo", request -> {
                    String payload = ExampleSupport.bodyAsString(request);
                    String contentType = request.header("Content-Type");
                    String response = "Received (" + (contentType == null ? "unknown" : contentType) + "): " + payload;
                    return new Response(HttpStatus.OK,
                            Map.of("Content-Type", "text/plain; charset=UTF-8"),
                            response.getBytes(StandardCharsets.UTF_8));
                })
                .notFound(request -> Response.text(HttpStatus.NOT_FOUND, "POST data to /echo"));

        ExampleServer server = ExampleSupport.newServer(port, router);
        System.out.println("EchoPostExample (" + server.transport().displayName() + ") listening on http://localhost:" + port);
        server.start();
    }
}
