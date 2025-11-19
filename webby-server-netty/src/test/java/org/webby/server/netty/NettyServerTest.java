package org.webby.server.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.webby.core.HttpStatus;
import org.webby.core.Response;
import org.webby.core.Router;

class NettyServerTest {
    private NettyServer server;
    private Thread serverThread;

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
        if (serverThread != null) {
            serverThread.join(TimeUnit.SECONDS.toMillis(5));
        }
    }

    @Test
    void servesRequestsViaNetty() throws Exception {
        Router router = new Router()
                .get("/hello", request -> Response.text(HttpStatus.OK, "Hello Netty"));

        server = new NettyServer(0);
        server.setRequestHandler(router);
        startServer();

        waitUntil(() -> server.isRunning() && server.port() > 0, Duration.ofSeconds(5));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + "/hello"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("Hello Netty", response.body());
    }

    private void startServer() {
        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "netty-server-test");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private static void waitUntil(BooleanSupplier condition, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Condition not satisfied before timeout");
            }
            Thread.sleep(50);
        }
    }
}
