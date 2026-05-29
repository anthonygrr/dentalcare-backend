package com.dentalcare.tony.api.presentation.dto.resquest;


import com.dentalcare.tony.api.domain.enums.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentUpdateRequest(

        LocalDate date,
        LocalTime time,
        String procedure,
        AppointmentStatus status,
        // 👇 solo se usan cuando COMPLETED
        String diagnosis,

        String treatment

) {}
