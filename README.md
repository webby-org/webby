# Webby

Webby is a tiny HTTP server toolkit built around a simple `Request`/`Response` model. The `webby-core` module ships a blocking server implementation that keeps dependencies to the bare JDK so it is easy to embed in tooling, CLIs, or quick experiments.

## Modules

- `webby-core` – core HTTP primitives plus the `Server` implementation.

## Usage

Add `webby-core` to your build as a local project dependency (or publish the module to your preferred repository). Once it is on the classpath you can bootstrap the server with a single handler function:

```java
import org.webby.core.Request;
import org.webby.core.RequestHandler;
import org.webby.core.Response;
import org.webby.core.Server;

public final class HelloApp {
    public static void main(String[] args) throws Exception {
        RequestHandler handler = request -> {
            if ("/health".equals(request.target())) {
                return Response.text(200, "OK");
            }
            return Response.text(200, "Hello " + request.header("X-User"));
        };

        try (Server server = new Server(8080, handler)) {
            server.start();
            System.out.println("Server started on http://localhost:8080");
            Thread.currentThread().join();
        }
    }
}
```

The handler receives a parsed `Request` and can return any `Response`. Returning `null` yields an automatic `204 No Content`, while throwing an exception results in a `500 Internal Server Error`.

## Development

Use the Gradle wrapper for all tasks:

- `./gradlew test` – run the unit suite.
- `./gradlew build` – assemble jars plus sources/javadoc.
- `./gradlew clean build` – rebuild from scratch if you suspect stale artifacts.

The project targets JDK 21.
