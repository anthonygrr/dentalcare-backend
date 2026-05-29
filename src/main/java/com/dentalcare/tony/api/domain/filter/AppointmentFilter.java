package com.dentalcare.tony.api.domain.filter;

import com.dentalcare.tony.api.domain.enums.AppointmentStatus;

import java.time.LocalDate;

public record AppointmentFilter(AppointmentStatus status, LocalDate date, String procedure, String name) {

}
