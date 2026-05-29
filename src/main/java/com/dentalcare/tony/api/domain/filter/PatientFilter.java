package com.dentalcare.tony.api.domain.filter;

import java.time.LocalDate;

public record PatientFilter(
        String name,
        String email,
        String phone,
        LocalDate birthDateFrom,
        LocalDate birthDateTo
) {}