package com.dentalcare.tony.api;

import com.dentalcare.tony.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
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
    private String accessToken;

    @BeforeEach
    void loginAsSeededAdmin() {
        Map<String, String> credentials = Map.of("login", "rosasdr", "password", "Admin1234");
        ResponseEntity<Map> login = rest.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(credentials, jsonHeaders()),
                Map.class
        );

        assertEquals(200, login.getStatusCode().value(), "seeded user must be able to log in");

        accessToken = (String) login.getBody().get("accessToken");

        authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.setBearerAuth(accessToken);
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

    @Test
    void importsPatientsFromCsvAndPersistsThem() {
        String csv = """
                name,email,phone,birthDate
                Import Alpha,import-alpha@example.com,555-3001,1990-01-15
                Import Beta,import-beta@example.com,555-3002,1991-02-16
                """;

        ResponseEntity<Map> response = importCsv(csv, accessToken);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, ((Number) response.getBody().get("total")).intValue());
        assertEquals(2, ((Number) response.getBody().get("imported")).intValue());
        assertEquals(0, ((Number) response.getBody().get("failed")).intValue());

        // both rows must be queryable afterwards
        ResponseEntity<Map> found = rest.exchange(
                "/patients?email=import-alpha@example.com",
                HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class
        );
        List<Map<String, Object>> content = (List<Map<String, Object>>) found.getBody().get("content");
        assertEquals(1, content.size(), "imported row must be persisted");
        assertEquals("Import Alpha", content.get(0).get("name"));
        assertEquals("1990-01-15", content.get(0).get("birthDate"));
    }

    @Test
    void reportsDuplicateRowsAsErrorsAndStillImportsValidOnes() {
        // seed a patient that collides with the second CSV row
        rest.exchange("/patients", HttpMethod.POST,
                new HttpEntity<>(patient("Dup Seed", "import-dup@example.com", "555-3010"), authHeaders),
                Map.class);

        String csv = """
                name,email,phone,birthDate
                Import Gamma,import-gamma@example.com,555-3011,1992-03-17
                Import Delta,import-dup@example.com,555-3012,1993-04-18
                """;

        ResponseEntity<Map> response = importCsv(csv, accessToken);

        assertEquals(200, response.getStatusCode().value(), "duplicates must not fail the whole import");
        assertEquals(2, ((Number) response.getBody().get("total")).intValue());
        assertEquals(1, ((Number) response.getBody().get("imported")).intValue());
        assertEquals(1, ((Number) response.getBody().get("failed")).intValue());

        List<Map<String, Object>> errors = (List<Map<String, Object>>) response.getBody().get("errors");
        assertEquals(1, errors.size());
        assertEquals(3, ((Number) errors.get(0).get("row")).intValue(), "duplicate sits on physical line 3");
        assertEquals("The email already exists", errors.get(0).get("message"));

        // the valid row must have been imported despite the failing one
        ResponseEntity<Map> found = rest.exchange(
                "/patients?email=import-gamma@example.com",
                HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class
        );
        List<Map<String, Object>> content = (List<Map<String, Object>>) found.getBody().get("content");
        assertEquals(1, content.size(), "valid row must survive the duplicate row");
    }

    @Test
    void reportsInvalidRowsAndStillImportsValidOnes() {
        String csv = """
                name,email,phone,birthDate
                ,missing-name@example.com,555-3030,1990-01-15
                Import Valid,import-valid@example.com,555-3031,1991-02-16
                """;

        ResponseEntity<Map> response = importCsv(csv, accessToken);

        assertEquals(200, response.getStatusCode().value(), "invalid rows must not fail the whole import");
        assertEquals(2, ((Number) response.getBody().get("total")).intValue());
        assertEquals(1, ((Number) response.getBody().get("imported")).intValue());
        assertEquals(1, ((Number) response.getBody().get("failed")).intValue());

        List<Map<String, Object>> errors = (List<Map<String, Object>>) response.getBody().get("errors");
        assertEquals(1, errors.size());
        assertEquals(2, ((Number) errors.get(0).get("row")).intValue(), "invalid row sits on physical line 2");
        assertEquals("Name is mandatory", errors.get(0).get("message"));

        // the valid row must still import (partial success)
        ResponseEntity<Map> found = rest.exchange(
                "/patients?email=import-valid@example.com",
                HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class
        );
        List<Map<String, Object>> content = (List<Map<String, Object>>) found.getBody().get("content");
        assertEquals(1, content.size(), "valid row must survive the invalid row");
    }

    @Test
    void rejectsCsvWithWrongHeaderWith400() {
        String csv = """
                foo,bar,baz,qux
                1,2,3,4
                """;

        ResponseEntity<Map> response = importCsv(csv, accessToken);

        assertEquals(400, response.getStatusCode().value(),
                "wrong header is a structurally unusable file");
        String detail = (String) response.getBody().get("detail");
        assertNotNull(detail, "400 must carry the ProblemDetail detail message");
        assertTrue(detail.contains("header"), "message must explain the header problem: " + detail);
    }

    @Test
    void rejectsUnauthenticatedCsvImport() {
        String csv = """
                name,email,phone,birthDate
                Import NoAuth,noauth@example.com,555-3020,1994-05-19
                """;

        ResponseEntity<Map> response = importCsv(csv, null);

        assertEquals(401, response.getStatusCode().value(),
                "CSV import must require a JWT like every other patients endpoint");
    }

    private ResponseEntity<Map> importCsv(String csv, String bearerToken) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "patients.csv";
            }
        });

        // fresh headers: multipart boundary content type, never the JSON authHeaders
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }

        return rest.exchange("/patients/import", HttpMethod.POST,
                new HttpEntity<>(form, headers), Map.class);
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
