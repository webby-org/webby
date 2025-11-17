# Webby

Webby is a tiny HTTP server toolkit built around a simple `Request`/`Response` model. The `webby-core` module ships a blocking server implementation that keeps dependencies to the bare JDK so it is easy to embed in tooling, CLIs, or quick experiments.

## Modules

- `webby-core` – core HTTP primitives plus the `Server` and `Router` implementations.
- `modules/examples` – runnable demo entrypoints that showcase GET/POST routing, middleware, and TLS setup.

## Usage

Add `webby-core` to your build as a local project dependency (or publish the module to your preferred repository). Once it is on the classpath you can bootstrap the server with a single handler function:

```java
import java.util.concurrent.Executors;

import org.webby.core.HttpStatus;
import org.webby.core.Response;
import org.webby.core.Router;
import org.webby.core.Server;

public final class HelloApp {
    public static void main(String[] args) throws Exception {
        Router router = new Router()
                .get("/health", request -> Response.text(HttpStatus.OK, "OK"))
                .get("/hello", request -> Response.text(HttpStatus.OK, "Hello " + request.header("X-User")))
                .notFound(request -> Response.text(HttpStatus.NOT_FOUND, "Try /hello"));

        Server server = new Server(8080);
        server.setRequestHandler(router);
        server.setExecutorService(Executors.newVirtualThreadPerTaskExecutor());
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));

        System.out.println("Server started on http://localhost:8080");
        server.start(); // blocks until server.close() (e.g., via Ctrl+C)
    }
}
```

The router (or any `RequestHandler`) receives a parsed `Request` and can return any `Response`. Returning `null` yields an automatic `204 No Content`, while throwing an exception results in a `500 Internal Server Error`.

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
