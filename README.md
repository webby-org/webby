# Webby

Webby is a tiny HTTP server toolkit built around a simple `Request`/`Response` model. The `webby-core` module ships a blocking server implementation that keeps dependencies to the bare JDK so it is easy to embed in tooling, CLIs, or quick experiments.

## Modules

- `webby-core` – core HTTP primitives such as `Request`, `Response`, and `Router`.
- `examples` – runnable demo entrypoints that showcase GET/POST routing, middleware, and TLS setup.
- `webby-server` – native blocking server that depends only on the JDK.
- `webby-server-jetty` – Jetty-backed server adapter that reuses the same `Request`/`Response` contract.
- `webby-server-netty` – Netty-backed server adapter sharing the same primitives.

## Usage

Add `webby-core` plus the adapter of your choice (`webby-server-jetty` or `webby-server-netty`) to your build. Once they are on the classpath you can bootstrap a server with a single handler function.

Jetty adapter:

```java
import org.webby.core.HttpStatus;
import org.webby.core.Response;
import org.webby.core.Router;
import org.webby.server.jetty.JettyServer;

public final class HelloApp {
    public static void main(String[] args) throws Exception {
        Router router = new Router()
                .get("/health", request -> Response.text(HttpStatus.OK, "OK"))
                .get("/hello", request -> Response.text(HttpStatus.OK, "Hello " + request.header("X-User")))
                .notFound(request -> Response.text(HttpStatus.NOT_FOUND, "Try /hello"));

        JettyServer server = new JettyServer(8080);
        server.setRequestHandler(router);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));

        System.out.println("Jetty server started on http://localhost:8080");
        server.start(); // blocks until server.close() (e.g., via Ctrl+C)
    }
}
```

Netty adapter:

```java
import org.webby.core.HttpStatus;
import org.webby.core.Response;
import org.webby.core.Router;
import org.webby.server.netty.NettyServer;

public final class HelloApp {
    public static void main(String[] args) throws Exception {
        Router router = new Router()
                .get("/health", request -> Response.text(HttpStatus.OK, "OK"))
                .get("/hello", request -> Response.text(HttpStatus.OK, "Hello " + request.header("X-User")))
                .notFound(request -> Response.text(HttpStatus.NOT_FOUND, "Try /hello"));

        NettyServer server = new NettyServer(8080);
        server.setRequestHandler(router);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));

        System.out.println("Netty server started on http://localhost:8080");
        server.start();
    }
}
```

Prefer the original blocking server that depends only on the JDK? Add `webby-server` to your build and use `org.webby.core.Server`, which now lives in that module.

The `examples` project defaults to the Jetty adapter, but you can set `WEBBY_TRANSPORT=netty` to run the sample applications on top of Netty instead.

The router (or any `RequestHandler`) receives a parsed `Request` and can return any `Response`. Returning `null` yields an automatic `204 No Content`, while throwing an exception results in a `500 Internal Server Error`. When you need servlet-container features or Netty's event-loop, apply the `webby-server-jetty` or `webby-server-netty` subproject and use the corresponding adapter class for the same API surface on top of those runtimes.

### Middleware

`Server` supports middleware layers that can inspect or short-circuit requests before they reach the terminal handler:

```java
server.addMiddleware((request, next) -> {
    if (request.header("X-Block") != null) {
        return Response.text(HttpStatus.FORBIDDEN, "blocked");
    }
    return next.handle(request);
});
```

Middlewares execute in registration order and can return a custom `Response` or delegate to `next.handle(request)` to keep processing.

### TLS

`Server` can terminate TLS if provided with an `SSLContext` that contains your certificates:

```java
SSLContext sslContext = SSLContext.getInstance("TLS");
sslContext.init(kmf.getKeyManagers(), null, null);

Server server = new Server(8443);
server.setRequestHandler(router);
server.setExecutorService(Executors.newVirtualThreadPerTaskExecutor());
server.enableTls(sslContext);
server.start(); // blocks like the non-TLS example
```

The context setup mirrors any standard Java TLS configuration (load your keystore into a `KeyManagerFactory`, optionally wire a `TrustManagerFactory`, then call `enableTls`). Because `start()` blocks, install a shutdown hook or launch the server on a dedicated thread if the calling thread must continue doing other work.

## Development

Use the Gradle wrapper for all tasks:

- `./gradlew test` – run the unit suite.
- `./gradlew build` – assemble jars plus sources/javadoc.
- `./gradlew clean build` – rebuild from scratch if you suspect stale artifacts.

The project compiles with Java 25 and targets Java 17.
