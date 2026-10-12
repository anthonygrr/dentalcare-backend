package com.dentalcare.tony.api;

import com.dentalcare.tony.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full-stack tests: real security chain, JWT login (seeded user),
 * JPA + Specifications + unique constraints against PostgreSQL.
 */
class PatientApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private HttpHeaders authHeaders;

    @BeforeEach
    void loginAsSeededAdmin() {
        Map<String, String> credentials = Map.of("login", "rosasdr", "password", "Admin1234");
        ResponseEntity<Map> login = rest.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(credentials, jsonHeaders()),
                Map.class
        );

        assertEquals(200, login.getStatusCode().value(), "seeded user must be able to log in");

        authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.setBearerAuth((String) login.getBody().get("accessToken"));
    }

    @Test
    void rejectsUnauthenticatedRequests() {
        ResponseEntity<Map> response = rest.getForEntity("/patients", Map.class);

        assertEquals(401, response.getStatusCode().value(),
                "patients must require a JWT");
        assertEquals("Authentication required", response.getBody().get("message"),
                "401 must carry the JSON body from CustomAuthenticationEntryPoint");
    }

    @Test
    void createsReadsFiltersAndDeletesAPatient() {
        // CREATE
        ResponseEntity<Map> created = rest.exchange(
                "/patients", HttpMethod.POST,
                new HttpEntity<>(patient("Ada Lovelace", "ada@example.com", "555-0001"), authHeaders),
                Map.class
        );
        assertEquals(200, created.getStatusCode().value());
        Number id = (Number) created.getBody().get("id");
        assertNotNull(id);

        // READ by id
        ResponseEntity<Map> found = rest.exchange(
                "/patients/" + id, HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class
        );
        assertEquals(200, found.getStatusCode().value());
        assertEquals("Ada Lovelace", found.getBody().get("name"));
        assertEquals("1990-01-15", found.getBody().get("birthDate"));

        // FILTER by name
        ResponseEntity<Map> filtered = rest.exchange(
                "/patients?name=Ada", HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class
        );
        List<Map<String, Object>> content = (List<Map<String, Object>>) filtered.getBody().get("content");
        assertEquals(1, content.size(), "name filter must find Ada");

        // DELETE
        ResponseEntity<String> deleted = rest.exchange(
                "/patients/" + id, HttpMethod.DELETE, new HttpEntity<>(authHeaders), String.class
        );
        assertEquals(204, deleted.getStatusCode().value());

        // CHARACTERIZATION: missing patient currently surfaces as 500 because
        // PatientServiceImpl throws a bare IllegalArgumentException.
        // Should be 404 — see odd/tasks/test-suite.md.
        ResponseEntity<String> afterDelete = rest.exchange(
                "/patients/" + id, HttpMethod.GET, new HttpEntity<>(authHeaders), String.class
        );
        assertEquals(500, afterDelete.getStatusCode().value());
    }

    @Test
    void rejectsDuplicateEmailWith409() {
        rest.exchange("/patients", HttpMethod.POST,
                new HttpEntity<>(patient("Bob Dup", "dup@example.com", "555-0002"), authHeaders),
                Map.class);

        ResponseEntity<Map> duplicate = rest.exchange(
                "/patients", HttpMethod.POST,
                new HttpEntity<>(patient("Other Person", "dup@example.com", "555-0003"), authHeaders),
                Map.class
        );

        assertEquals(409, duplicate.getStatusCode().value());
        assertEquals("The email already exists", duplicate.getBody().get("message"));
    }

    @Test
    void paginatesThePatientList() {
        for (int i = 1; i <= 3; i++) {
            rest.exchange("/patients", HttpMethod.POST,
                    new HttpEntity<>(patient(
                            "Pagination-X" + i,
                            "page" + i + "@example.com",
                            "555-100" + i), authHeaders),
                    Map.class);
        }

        // no spaces in the filter value: TestRestTemplate would double-encode %20
        ResponseEntity<Map> page = rest.exchange(
                "/patients?name=Pagination&page=0&size=2",
                HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class
        );

        List<Map<String, Object>> content = (List<Map<String, Object>>) page.getBody().get("content");
        assertEquals(2, content.size(), "page size must be honored");
        assertEquals(2, ((Number) page.getBody().get("totalPages")).intValue(),
                "3 patients over size 2 must yield 2 pages");
    }

    private Map<String, Object> patient(String name, String email, String phone) {
        return Map.of(
                "name", name,
                "email", email,
                "phone", phone,
                "birthDate", "1990-01-15"
        );
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
