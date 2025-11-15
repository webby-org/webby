package org.webby.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ServerTest {
    @Test
    void handleGetRequestReturnsHandlerResponse() throws Exception {
        AtomicReference<Request> captured = new AtomicReference<>();
        int port = nextPort();
        Server server = new Server(port, request -> {
            captured.set(request);
            return Response.text(200, "hello");
        });
        server.start();
        awaitServer(port);

        try {
            String requestHead = "GET /hello HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "X-Test: Ping\r\n\r\n";
            String rawResponse = sendHttpRequest(port, requestHead);

            assertTrue(rawResponse.startsWith("HTTP/1.1 200 OK"));
            assertTrue(rawResponse.contains("Content-Length: 5"));
            assertTrue(rawResponse.contains("Connection: close"));
            assertEquals("hello", responseBody(rawResponse));

            Request request = captured.get();
            assertNotNull(request);
            assertEquals("GET", request.method());
            assertEquals("/hello", request.target());
            assertEquals("HTTP/1.1", request.version());
            assertEquals("Ping", request.header("x-test"));
        } finally {
            server.close();
        }
    }

    @Test
    void postRequestDeliversBodyToHandler() throws Exception {
        AtomicReference<Request> captured = new AtomicReference<>();
        int port = nextPort();
        Server server = new Server(port, request -> {
            captured.set(request);
            return Response.text(201, "created");
        });
        server.start();
        awaitServer(port);

        try {
            String body = "name=webby";
            String requestHead = "POST /submit HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Content-Length: " + body.length() + "\r\n\r\n";

            String rawResponse = sendHttpRequest(port, requestHead, body.getBytes(StandardCharsets.UTF_8));

            assertTrue(rawResponse.startsWith("HTTP/1.1 201 Created"));
            assertEquals("created", responseBody(rawResponse));

            Request received = captured.get();
            assertNotNull(received);
            assertEquals("POST", received.method());
            assertEquals("name=webby", new String(received.body(), StandardCharsets.UTF_8));
        } finally {
            server.close();
        }
    }

    @Test
    void nullHandlerResponseDefaultsToNoContent() throws Exception {
        int port = nextPort();
        Server server = new Server(port, request -> null);
        server.start();
        awaitServer(port);

        try {
            String rawResponse = sendHttpRequest(port, "GET /empty HTTP/1.1\r\n"
                    + "Host: localhost\r\n\r\n");

            assertTrue(rawResponse.startsWith("HTTP/1.1 204 No Content"));
            assertEquals("", responseBody(rawResponse));
        } finally {
            server.close();
        }
    }

    @Test
    void handlerExceptionReturnsInternalServerError() throws Exception {
        int port = nextPort();
        Server server = new Server(port, request -> {
            throw new IllegalStateException("boom");
        });
        server.start();
        awaitServer(port);

        try {
            String rawResponse = sendHttpRequest(port, "GET /fail HTTP/1.1\r\n"
                    + "Host: localhost\r\n\r\n");

            assertTrue(rawResponse.startsWith("HTTP/1.1 500 Internal Server Error"));
            assertEquals("Internal Server Error", responseBody(rawResponse));
        } finally {
            server.close();
        }
    }

    private static int nextPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void awaitServer(int port) throws IOException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (true) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 200);
                return;
            } catch (IOException ex) {
                if (System.nanoTime() > deadline) {
                    throw new IOException("Server did not start in time", ex);
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for server", interrupted);
                }
            }
        }
    }

    private static String sendHttpRequest(int port, String requestHead) throws IOException {
        return sendHttpRequest(port, requestHead, new byte[0]);
    }

    private static String sendHttpRequest(int port, String requestHead, byte[] body) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 1000);
            socket.setSoTimeout(2000);
            OutputStream out = socket.getOutputStream();
            out.write(requestHead.getBytes(StandardCharsets.UTF_8));
            if (body.length > 0) {
                out.write(body);
            }
            out.flush();
            socket.shutdownOutput();

            InputStream in = socket.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    private static String responseBody(String response) {
        int idx = response.indexOf("\r\n\r\n");
        if (idx == -1) {
            return response;
        }
        return response.substring(idx + 4);
    }
}
