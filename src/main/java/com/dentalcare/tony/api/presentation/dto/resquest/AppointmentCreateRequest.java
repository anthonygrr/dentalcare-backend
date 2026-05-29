package com.dentalcare.tony.api.presentation.dto.resquest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentCreateRequest(

        @NotNull
        LocalDate date,

        @NotNull
        LocalTime time,

        @NotBlank
        String procedure,

        @NotNull
        Long patientId,

        @NotNull
        Long doctorId

) {}