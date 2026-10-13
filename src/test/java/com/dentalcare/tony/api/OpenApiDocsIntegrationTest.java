package com.dentalcare.tony.api;

import com.dentalcare.tony.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The OpenAPI docs must stay public (SecurityConfig permits
 * /v3/api-docs/** and /swagger-ui/**) and describe every resource.
 */
class OpenApiDocsIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void apiDocsArePublicAndCoverEveryResource() {
        ResponseEntity<Map> response = rest.getForEntity("/v3/api-docs", Map.class);

        assertEquals(200, response.getStatusCode().value(),
                "/v3/api-docs must not require a JWT");

        Map<?, ?> body = response.getBody();
        Map<?, ?> info = (Map<?, ?>) body.get("info");
        assertEquals("DentalCare API", info.get("title"));

        Map<?, ?> paths = (Map<?, ?>) body.get("paths");
        for (String path : new String[]{
                "/patients", "/doctors", "/appointments", "/medical-records",
                "/api/auth/login", "/api/users/me"}) {
            assertTrue(paths.containsKey(path), "missing documented path: " + path);
        }

        Map<?, ?> components = (Map<?, ?>) body.get("components");
        Map<?, ?> schemes = (Map<?, ?>) components.get("securitySchemes");
        assertTrue(schemes.containsKey("bearerAuth"), "JWT security scheme must be advertised");
    }

    @Test
    void swaggerUiIsReachableWithoutAuth() {
        ResponseEntity<String> response = rest.getForEntity("/swagger-ui.html", String.class);

        int status = response.getStatusCode().value();
        assertTrue(status >= 200 && status < 400,
                "swagger-ui must be reachable anonymously, got " + status);
    }
}
