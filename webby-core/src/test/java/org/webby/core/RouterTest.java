package org.webby.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RouterTest {
    @Test
    void selectsHandlerUsingMethodAndPath() {
        Router router = new Router()
                .get("/hello", request -> Response.text(HttpStatus.OK, "GET"))
                .post("/hello", request -> Response.text(HttpStatus.CREATED, "POST"));

        Request getRequest = new Request(HttpMethod.GET, "/hello", "HTTP/1.1", Map.of(), null);
        Request postRequest = new Request(HttpMethod.POST, "/hello", "HTTP/1.1", Map.of(), null);

        Response getResponse = router.handle(getRequest);
        Response postResponse = router.handle(postRequest);

        assertEquals("GET", new String(getResponse.body()));
        assertEquals(200, getResponse.statusCode());
        assertEquals("POST", new String(postResponse.body()));
        assertEquals(201, postResponse.statusCode());
    }

    @Test
    void routesIgnoreQueryString() {
        Router router = new Router().get("/items", request -> Response.text(HttpStatus.OK, "OK"));

        Request request = new Request(HttpMethod.GET, "/items?limit=10", "HTTP/1.1", Map.of(), null);

        assertEquals("OK", new String(router.handle(request).body()));
    }

    @Test
    void notFoundHandlerIsCustomizable() {
        Router router = new Router()
                .notFound(request -> Response.text(HttpStatus.IM_A_TEAPOT, "Missing"));

        Request request = new Request(HttpMethod.GET, "/unknown", "HTTP/1.1", Map.of(), null);

        Response response = router.handle(request);
        assertEquals(418, response.statusCode());
        assertEquals("Missing", new String(response.body()));
    }

    @Test
    void capturesPathVariablesFromTemplates() {
        Router router = new Router()
                .get("/users/{userId}/posts/{postId}", request -> {
                    assertEquals("42", request.getPathVariable("userId"));
                    assertEquals("99", request.getPathVariable("postId"));
                    assertNull(request.getPathVariable("missing"));
                    return Response.text(HttpStatus.OK, request.getPathVariable("postId"));
                });

        Request request = new Request(HttpMethod.GET, "/users/42/posts/99", "HTTP/1.1", Map.of(), null);
        Response response = router.handle(request);

        assertEquals(200, response.statusCode());
        assertEquals("99", new String(response.body()));
    }
}
