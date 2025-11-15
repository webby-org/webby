package org.webby.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Minimal multithreaded HTTP server backed by {@link ServerSocket}.
 */
public final class Server implements Closeable {
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);

    private final int port;
    private final RequestHandler requestHandler;
    private final ExecutorService workers;

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    /**
     * Creates a new server bound to the specified port.
     *
     * @param port listening port
     * @param requestHandler handler invoked for each parsed request
     */
    public Server(int port, RequestHandler requestHandler) {
        this.port = port;
        this.requestHandler = Objects.requireNonNull(requestHandler, "requestHandler");
        this.workers = Executors.newCachedThreadPool(new WorkerFactory());
    }

    /**
     * Starts the server and begins accepting connections.
     *
     * @throws IOException if the socket cannot be bound
     */
    public synchronized void start() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(port));
        running = true;
        acceptThread = new Thread(this::acceptLoop, "webby-acceptor");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /** Stops accepting new connections and shuts down worker threads. */
    public synchronized void stop() {
        running = false;
        closeQuietly(serverSocket);
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
        workers.shutdown();
        try {
            if (!workers.awaitTermination(SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * Indicates if the accept loop is currently running.
     *
     * @return {@code true} when the server is accepting requests
     */
    public boolean isRunning() {
        return running;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                client.setTcpNoDelay(true);
                workers.submit(() -> handleClient(client));
            } catch (SocketException socketClosed) {
                if (running) {
                    socketClosed.printStackTrace();
                }
                break;
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (Socket client = socket;
             InputStream in = new BufferedInputStream(client.getInputStream());
             OutputStream out = new BufferedOutputStream(client.getOutputStream())) {
            Request request = parseRequest(in);
            if (request == null) {
                return;
            }
            Response response;
            try {
                response = Objects.requireNonNullElseGet(requestHandler.handle(request), () -> Response.text(HttpStatus.NO_CONTENT, ""));
            } catch (Exception ex) {
                response = Response.text(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
            }
            writeResponse(out, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Request parseRequest(InputStream input) throws IOException {
        String requestLine = readLine(input);
        if (requestLine == null || requestLine.isEmpty()) {
            return null;
        }
        String[] parts = requestLine.split(" ");
        if (parts.length < 3) {
            return null;
        }
        HttpMethod method = HttpMethod.fromToken(parts[0]);
        if (method == null) {
            return null;
        }
        String target = parts[1];
        String version = parts[2];

        Map<String, String> headers = new LinkedHashMap<>();
        String line;
        while ((line = readLine(input)) != null && !line.isEmpty()) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                String name = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                headers.put(name, value);
            }
        }

        int contentLength = headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("Content-Length"))
                .findFirst()
                .map(entry -> parseInt(entry.getValue()))
                .orElse(0);

        byte[] body = readBody(input, contentLength);
        return new Request(method, target, version, Collections.unmodifiableMap(headers), body);
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static byte[] readBody(InputStream input, int contentLength) throws IOException {
        if (contentLength <= 0) {
            return new byte[0];
        }
        byte[] body = new byte[contentLength];
        int offset = 0;
        while (offset < contentLength) {
            int read = input.read(body, offset, contentLength - offset);
            if (read == -1) {
                break;
            }
            offset += read;
        }
        if (offset < contentLength) {
            byte[] truncated = new byte[offset];
            System.arraycopy(body, 0, truncated, 0, offset);
            return truncated;
        }
        return body;
    }

    private static void writeResponse(OutputStream out, Response response) throws IOException {
        byte[] body = response.body();
        Map<String, String> headers = new LinkedHashMap<>(response.headers());
        headers.putIfAbsent("Content-Length", String.valueOf(body.length));
        headers.putIfAbsent("Content-Type", "text/plain; charset=UTF-8");
        headers.put("Connection", "close");

        StringBuilder head = new StringBuilder()
                .append("HTTP/1.1 ")
                .append(response.statusCode())
                .append(' ')
                .append(response.reasonPhrase())
                .append("\r\n");
        headers.forEach((key, value) -> head.append(key).append(": ").append(value).append("\r\n"));
        head.append("\r\n");
        out.write(head.toString().getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    private static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        boolean readAny = false;
        while (true) {
            int read = input.read();
            if (read == -1) {
                break;
            }
            readAny = true;
            if (read == '\n') {
                break;
            }
            if (read != '\r') {
                buffer.write(read);
            }
        }
        if (!readAny && buffer.size() == 0) {
            return null;
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static void closeQuietly(ServerSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // ignored
            }
        }
    }

    private static final class WorkerFactory implements ThreadFactory {
        private int counter;

        @Override
        public synchronized Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "webby-worker-" + counter++);
            thread.setDaemon(true);
            return thread;
        }
    }
}
