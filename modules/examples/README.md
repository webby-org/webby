# Webby Examples

The `modules/examples` project contains a handful of tiny entrypoints that illustrate how to work with the `webby-core` server. Each example is a standalone `main` method that you can run directly from your IDE or via the dedicated Gradle tasks (for example `./gradlew :modules:examples:runHelloExample`).

## Available entrypoints

| Class | Purpose |
| --- | --- |
| `org.webby.examples.HelloWorldExample` | Minimal GET router with `/hello` and `/health` endpoints. |
| `org.webby.examples.EchoPostExample` | Accepts a POST payload at `/echo` and returns the body along with the detected `Content-Type`. |
| `org.webby.examples.MiddlewareExample` | Shows how to add logging and authentication middleware before delegating to `/data`. |
| `org.webby.examples.HttpsExample` | Enables TLS using an existing keystore; configure `WEBBY_KEYSTORE`, `WEBBY_KEYSTORE_PASSWORD`, and optional `WEBBY_KEYSTORE_TYPE`. |

## Running the demos

- `./gradlew :modules:examples:runHelloExample`
- `./gradlew :modules:examples:runEchoExample`
- `./gradlew :modules:examples:runMiddlewareExample`
- `./gradlew :modules:examples:runHttpsExample`

All tasks accept the environment variables listed above so you can adjust ports or TLS settings per process.

Each class reads a dedicated `WEBBY_*_PORT` environment variable so you can run them at the same time without collisions. They all configure shutdown hooks and use the JDK virtual thread executor to keep the code close to real-world usage.
