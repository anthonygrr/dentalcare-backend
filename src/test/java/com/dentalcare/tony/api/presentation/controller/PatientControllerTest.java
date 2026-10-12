package com.dentalcare.tony.api.presentation.controller;

import com.dentalcare.tony.api.application.service.PatientService;
import com.dentalcare.tony.api.infrastructure.security.JwtAuthenticationFilter;
import com.dentalcare.tony.api.presentation.dto.response.PatientResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice: Spring MVC + validation + GlobalExceptionHandler only,
 * service is mocked. The JWT filter is mocked out — real security is
 * covered by the Testcontainers integration tests.
 */
@WebMvcTest(controllers = PatientController.class)
@AutoConfigureMockMvc(addFilters = false)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createsPatientAndMapsTheResponse() throws Exception {
        when(patientService.createPatient(any())).thenReturn(
                new PatientResponse(7L, "Ada Lovelace", "ada@example.com", "555-0001", LocalDate.of(1990, 1, 15))
        );

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada Lovelace","email":"ada@example.com","phone":"555-0001","birthDate":"1990-01-15"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.birthDate").value("1990-01-15"));
    }

    @Test
    void rejectsInvalidPatientWith400AndFieldErrors() throws Exception {
        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'phone')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'birthDate')]").exists());
    }

    @Test
    void missingPatientCurrentlyMapsTo500() throws Exception {
        // CHARACTERIZATION: PatientServiceImpl throws a bare IllegalArgumentException,
        // which the generic handler turns into 500. Should be 404 — see odd/tasks/test-suite.md.
        when(patientService.getPatientById(404L))
                .thenThrow(new IllegalArgumentException("patient not found"));

        mockMvc.perform(get("/patients/404"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Server Internal Error"));
    }
}
