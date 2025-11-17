package org.webby.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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

    @Test
    void subRoutersScopePaths() {
        Router router = new Router();
        Router api = router.subRouterAtPath("/api/v1");
        Router admin = api.subRouterAtPath("/admin");
        assertSame(router, router.subRouterAtPath("/"));
        assertNotSame(router, api);

        api.get("/users/{id}", request -> Response.text(HttpStatus.OK, request.getPathVariable("id")));
        admin.get("/stats", request -> Response.text(HttpStatus.OK, "stats"));

        Response usersResponse = router.handle(new Request(HttpMethod.GET, "/api/v1/users/7", "HTTP/1.1", Map.of(), null));
        Response adminResponse = router.handle(new Request(HttpMethod.GET, "/api/v1/admin/stats", "HTTP/1.1", Map.of(), null));
        Response missResponse = router.handle(new Request(HttpMethod.GET, "/users/7", "HTTP/1.1", Map.of(), null));

        assertEquals("7", new String(usersResponse.body()));
        assertEquals(200, usersResponse.statusCode());
        assertEquals("stats", new String(adminResponse.body()));
        assertEquals(200, adminResponse.statusCode());
        assertEquals(404, missResponse.statusCode());

        // Sub-router can also act as the handler directly.
        Response directResponse = api.handle(new Request(HttpMethod.GET, "/api/v1/users/9", "HTTP/1.1", Map.of(), null));
        assertEquals("9", new String(directResponse.body()));
    }
}
