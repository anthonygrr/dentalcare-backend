package com.dentalcare.tony.api.presentation.dto.response;

import com.dentalcare.tony.api.domain.enums.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentResponse(
        Long id,

        LocalDate date,

        LocalTime time,

        String procedure,

        AppointmentStatus status,

        Long patientId,

        String patientName,

        Long doctorId,

        String doctorName
) {
}
