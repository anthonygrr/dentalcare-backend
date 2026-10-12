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
 * Full-stack appointment flow: doctor + patient setup per test,
 * double-booking rule, status/date filters and the auto-created
 * medical record when an appointment is COMPLETED.
 */
class AppointmentApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private HttpHeaders authHeaders;
    private Long patientId;
    private Long doctorId;

    @BeforeEach
    void setUpFreshDoctorAndPatient() {
        // login (seeded admin)
        ResponseEntity<Map> login = rest.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(Map.of("login", "rosasdr", "password", "Admin1234"), jsonHeaders()),
                Map.class
        );
        assertEquals(200, login.getStatusCode().value());
        authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.setBearerAuth((String) login.getBody().get("accessToken"));

        // unique doctor + patient per test → no cross-test collisions
        String unique = String.valueOf(System.nanoTime());

        ResponseEntity<Map> doctor = rest.exchange(
                "/doctors", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "Dra. " + unique,
                        "email", "dr" + unique + "@example.com",
                        "phone", "phone-" + unique,
                        "address", "Av. Siempreviva 742",
                        "specialty", "DENTIST"), authHeaders),
                Map.class
        );
        assertEquals(200, doctor.getStatusCode().value());
        doctorId = ((Number) doctor.getBody().get("id")).longValue();

        ResponseEntity<Map> patient = rest.exchange(
                "/patients", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "Paciente " + unique,
                        "email", "pac" + unique + "@example.com",
                        "phone", "tel-" + unique,
                        "birthDate", "1988-07-04"), authHeaders),
                Map.class
        );
        assertEquals(200, patient.getStatusCode().value());
        patientId = ((Number) patient.getBody().get("id")).longValue();
    }

    @Test
    void createsAppointmentWithScheduledStatus() {
        ResponseEntity<Map> created = rest.exchange(
                "/appointments", HttpMethod.POST,
                new HttpEntity<>(appointment("2030-06-04", "10:00:00", "CHECKUP"), authHeaders),
                Map.class
        );

        assertEquals(200, created.getStatusCode().value());
        assertEquals("SCHEDULED", created.getBody().get("status"), "new appointments must start SCHEDULED");
        assertEquals("CHECKUP", created.getBody().get("procedure"));
        assertEquals(patientId.longValue(), ((Number) created.getBody().get("patientId")).longValue());
        assertEquals(doctorId.longValue(), ((Number) created.getBody().get("doctorId")).longValue());
        assertNotNull(created.getBody().get("patientName"));
        assertNotNull(created.getBody().get("doctorName"));
    }

    @Test
    void filtersAppointmentsByStatusAndDate() {
        ResponseEntity<Map> created = rest.exchange(
                "/appointments", HttpMethod.POST,
                new HttpEntity<>(appointment("2030-06-01", "11:00:00", "CLEANING"), authHeaders),
                Map.class
        );
        Number appointmentId = (Number) created.getBody().get("id");

        ResponseEntity<Map> filtered = rest.exchange(
                "/appointments?status=SCHEDULED&date=2030-06-01",
                HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class
        );

        assertEquals(200, filtered.getStatusCode().value());
        List<Map<String, Object>> content = (List<Map<String, Object>>) filtered.getBody().get("content");
        assertTrue(content.stream().anyMatch(a ->
                        ((Number) a.get("id")).longValue() == appointmentId.longValue()),
                "the created appointment must appear under its status+date filter");
    }

    @Test
    void rejectsDoctorDoubleBooking() {
        ResponseEntity<Map> first = rest.exchange(
                "/appointments", HttpMethod.POST,
                new HttpEntity<>(appointment("2030-06-02", "09:00:00", "EXTRACTION"), authHeaders),
                Map.class
        );
        assertEquals(200, first.getStatusCode().value());

        // CHARACTERIZATION: the conflict is thrown as a bare RuntimeException,
        // so the generic handler turns it into a 500.
        // Should be 400 "Business Error" — see odd/tasks/test-suite.md.
        ResponseEntity<Map> conflict = rest.exchange(
                "/appointments", HttpMethod.POST,
                new HttpEntity<>(appointment("2030-06-02", "09:00:00", "CHECKUP"), authHeaders),
                Map.class
        );

        assertEquals(500, conflict.getStatusCode().value());
        assertEquals("Server Internal Error", conflict.getBody().get("message"));
    }

    @Test
    void completingAppointmentCreatesMedicalRecord() {
        ResponseEntity<Map> created = rest.exchange(
                "/appointments", HttpMethod.POST,
                new HttpEntity<>(appointment("2030-06-03", "14:00:00", "EXTRACTION"), authHeaders),
                Map.class
        );
        Number appointmentId = (Number) created.getBody().get("id");

        ResponseEntity<Map> updated = rest.exchange(
                "/appointments/" + appointmentId, HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "status", "COMPLETED",
                        "diagnosis", "DeepCaries-34",
                        "treatment", "RootCanal"), authHeaders),
                Map.class
        );
        assertEquals(200, updated.getStatusCode().value());
        assertEquals("COMPLETED", updated.getBody().get("status"));

        ResponseEntity<Map> records = rest.exchange(
                "/medical-records?diagnosis=DeepCaries-34",
                HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class
        );

        assertEquals(200, records.getStatusCode().value());
        List<Map<String, Object>> content = (List<Map<String, Object>>) records.getBody().get("content");
        assertTrue(content.stream().anyMatch(r ->
                        ((Number) r.get("appointmentId")).longValue() == appointmentId.longValue()),
                "COMPLETED appointments must auto-create their medical record");
    }

    @Test
    void unknownAppointmentIdIsCurrentlyA500() {
        // CHARACTERIZATION: bare RuntimeException → 500, should be 404 — see odd/tasks/test-suite.md.
        ResponseEntity<Map> response = rest.exchange(
                "/appointments/999999",
                HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class
        );
        assertEquals(500, response.getStatusCode().value());
    }

    private Map<String, Object> appointment(String date, String time, String procedure) {
        return Map.of(
                "date", date,
                "time", time,
                "procedure", procedure,
                "patientId", patientId,
                "doctorId", doctorId
        );
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
