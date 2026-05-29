package com.dentalcare.tony.api.presentation.dto.response;

import java.time.LocalDate;

public record PatientResponse (
        Long id,
        String name,
        String email,
        String phone,
        LocalDate birthDate
){

}
