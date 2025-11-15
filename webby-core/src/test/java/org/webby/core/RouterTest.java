package org.webby.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RouterTest {
    @Test
    void selectsHandlerUsingMethodAndPath() {
        Router router = new Router()
                .get("/hello", request -> Response.text(200, "GET"))
                .post("/hello", request -> Response.text(201, "POST"));

        Request getRequest = new Request("GET", "/hello", "HTTP/1.1", Map.of(), null);
        Request postRequest = new Request("POST", "/hello", "HTTP/1.1", Map.of(), null);

        Response getResponse = router.handle(getRequest);
        Response postResponse = router.handle(postRequest);

        assertEquals("GET", new String(getResponse.body()));
        assertEquals(200, getResponse.statusCode());
        assertEquals("POST", new String(postResponse.body()));
        assertEquals(201, postResponse.statusCode());
    }

    @Test
    void routesIgnoreQueryString() {
        Router router = new Router().get("/items", request -> Response.text(200, "OK"));

        Request request = new Request("GET", "/items?limit=10", "HTTP/1.1", Map.of(), null);

        assertEquals("OK", new String(router.handle(request).body()));
    }

    @Test
    void notFoundHandlerIsCustomizable() {
        Router router = new Router()
                .notFound(request -> Response.text(418, "Missing"));

        Request request = new Request("GET", "/unknown", "HTTP/1.1", Map.of(), null);

        Response response = router.handle(request);
        assertEquals(418, response.statusCode());
        assertEquals("Missing", new String(response.body()));
    }
}
