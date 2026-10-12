package com.dentalcare.tony.api.presentation.controller;

import com.dentalcare.tony.api.application.service.AppointmentService;
import com.dentalcare.tony.api.domain.enums.AppointmentStatus;
import com.dentalcare.tony.api.infrastructure.security.JwtAuthenticationFilter;
import com.dentalcare.tony.api.presentation.dto.response.AppointmentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice for appointments, service mocked.
 * NOTE: AppointmentController.create/update do NOT declare @Valid, so bean
 * validation on the request DTOs never fires — that gap is documented, not
 * asserted. Real security is covered by the Testcontainers integration tests.
 */
@WebMvcTest(controllers = AppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private AppointmentResponse sampleResponse() {
        return new AppointmentResponse(
                11L,
                LocalDate.of(2030, 3, 15),
                LocalTime.of(10, 0),
                "CHECKUP",
                AppointmentStatus.SCHEDULED,
                3L,
                "Ada Lovelace",
                5L,
                "Dra. Rosas"
        );
    }

    @Test
    void createsAppointmentAndMapsTheResponse() throws Exception {
        when(appointmentService.createAppointment(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2030-03-15","time":"10:00:00","procedure":"CHECKUP","patientId":3,"doctorId":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.patientName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.doctorName").value("Dra. Rosas"));
    }

    @Test
    void listsAppointmentsAsAPage() throws Exception {
        when(appointmentService.findAll(any(), any())).thenReturn(
                new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 10), 1)
        );

        mockMvc.perform(get("/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(11))
                .andExpect(jsonPath("$.content[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void deletesAppointmentWith204() throws Exception {
        mockMvc.perform(delete("/appointments/11"))
                .andExpect(status().isNoContent());

        verify(appointmentService).deleteAppointment(11L);
    }

    @Test
    void missingAppointmentCurrentlyMapsTo500() throws Exception {
        // CHARACTERIZATION: bare RuntimeException → generic handler → 500.
        // Should be 404 — see odd/tasks/test-suite.md.
        when(appointmentService.getAppointmentById(404L))
                .thenThrow(new RuntimeException("Appointment not found"));

        mockMvc.perform(get("/appointments/404"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Server Internal Error"));
    }
}
