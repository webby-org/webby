package org.webby.examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.webby.core.HttpStatus;
import org.webby.core.Response;
import org.webby.core.Router;
import org.webby.core.Server;

/** Demonstrates enabling TLS by wiring a pre-existing keystore. */
public final class HttpsExample {
    private HttpsExample() {
    }

    /**
     * Spawns an HTTPS server using the keystore described by WEBBY_KEYSTORE* environment variables.
     *
     * @param args ignored
     * @throws Exception if TLS setup or server start fails
     */
    public static void main(String[] args) throws Exception {
        int port = ExampleSupport.port("WEBBY_TLS_PORT", 8443);
        Router router = new Router()
                .get("/hello", request -> Response.text(HttpStatus.OK, "Hello over HTTPS"))
                .notFound(request -> Response.text(HttpStatus.NOT_FOUND, "Use https://localhost:" + port + "/hello"));

        Server server = ExampleSupport.newServer(port, router);
        server.enableTls(buildSslContext());

        System.out.println("HttpsExample listening on https://localhost:" + port);
        server.start();
    }

    private static SSLContext buildSslContext() throws Exception {
        String keystorePath = ExampleSupport.requiredEnv("WEBBY_KEYSTORE");
        char[] password = ExampleSupport.requiredEnv("WEBBY_KEYSTORE_PASSWORD").toCharArray();
        String type = System.getenv().getOrDefault("WEBBY_KEYSTORE_TYPE", "PKCS12");
        KeyStore keyStore = KeyStore.getInstance(type);
        Path path = Path.of(keystorePath);
        if (!Files.exists(path)) {
            throw new IOException("Keystore not found at " + path.toAbsolutePath());
        }
        try (var input = Files.newInputStream(path)) {
            keyStore.load(input, password);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }
}
