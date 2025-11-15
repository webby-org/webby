package org.webby.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResponseTest {
    @Test
    void textFactoryCreatesUtf8PayloadWithReasonPhrase() {
        Response response = Response.text(200, "Hello");

        assertEquals(200, response.statusCode());
        assertEquals("OK", response.reasonPhrase());
        assertEquals(0, response.headers().size());
        assertArrayEquals("Hello".getBytes(StandardCharsets.UTF_8), response.body());
    }

    @Test
    void constructorDefensivelyCopiesHeadersAndBody() {
        Response response = new Response(418, "Teapot", Map.of("X-Test", "yes"), null);

        assertEquals("Teapot", response.reasonPhrase());
        assertEquals("yes", response.headers().get("X-Test"));
        assertThrows(UnsupportedOperationException.class, () -> response.headers().put("New", "value"));
        assertEquals(0, response.body().length);
    }
}
