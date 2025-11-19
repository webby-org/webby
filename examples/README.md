# Webby Examples

The `examples` project contains a handful of tiny entrypoints that illustrate how to work with `webby-core` plus the Jetty/Netty server adapters. Each example is a standalone `main` method that you can run directly from your IDE or via the dedicated Gradle tasks (for example `./gradlew :examples:runHelloExample`). Set `WEBBY_TRANSPORT=netty` to boot the demos on the Netty adapter; otherwise they default to Jetty.

## Available entrypoints

| Class | Purpose |
| --- | --- |
| `org.webby.examples.HelloWorldExample` | Minimal GET router with `/hello` and `/health` endpoints. |
| `org.webby.examples.EchoPostExample` | Accepts a POST payload at `/echo` and returns the body along with the detected `Content-Type`. |
| `org.webby.examples.MiddlewareExample` | Shows how to add logging and authentication middleware before delegating to `/data`. |
| `org.webby.examples.HttpsExample` | Enables TLS using an existing keystore; configure `WEBBY_KEYSTORE`, `WEBBY_KEYSTORE_PASSWORD`, and optional `WEBBY_KEYSTORE_TYPE`. |

## Running the demos

- `./gradlew :examples:runHelloExample`
- `./gradlew :examples:runEchoExample`
- `./gradlew :examples:runMiddlewareExample`
- `./gradlew :examples:runHttpsExample`

All tasks accept the environment variables listed above so you can adjust ports or TLS settings per process. `WEBBY_TRANSPORT` controls whether the Jetty (`jetty`, default) or Netty (`netty`) adapter is used.

Each class reads a dedicated `WEBBY_*_PORT` environment variable so you can run them at the same time without collisions. They all configure shutdown hooks so the server shuts down cleanly when the process exits.
