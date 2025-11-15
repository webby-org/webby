package org.webby.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequestTest {
    @Test
    void headerLookupIsCaseInsensitive() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "localhost");
        headers.put("X-Test", "demo");

        Request request = new Request(HttpMethod.GET, "/", "HTTP/1.1", headers, "body".getBytes());

        assertEquals("demo", request.header("x-test"));
        assertEquals("localhost", request.header("HOST"));
    }

    @Test
    void constructorDefensivelyCopiesHeadersAndBody() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Header", "value");
        Request request = new Request(HttpMethod.GET, "/", "HTTP/1.1", headers, null);

        headers.put("Header", "mutated");
        assertEquals("value", request.headers().get("Header"));
        assertThrows(UnsupportedOperationException.class, () -> request.headers().put("New", "1"));
        assertArrayEquals(new byte[0], request.body());
    }
}
