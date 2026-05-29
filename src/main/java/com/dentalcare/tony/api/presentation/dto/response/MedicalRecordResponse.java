package com.dentalcare.tony.api.presentation.dto.response;

import com.dentalcare.tony.api.domain.enums.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record MedicalRecordResponse(
        Long id,
        LocalDate date,

        String diagnosis,

        String treatment,

        // patient
        Long patientId,

        String patientName,

        // appointment
        Long appointmentId,

        LocalDate appointmentDate,

        LocalTime appointmentTime,

        String procedure,

        AppointmentStatus appointmentStatus,

        // doctor
        String doctorName
) {
}
