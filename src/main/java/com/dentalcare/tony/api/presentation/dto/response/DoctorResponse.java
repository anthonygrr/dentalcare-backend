package com.dentalcare.tony.api.presentation.dto.response;

import com.dentalcare.tony.api.domain.enums.Specialty;

public record DoctorResponse(
         Long id,
         String name,
         String email,
         String phone,
         String address,
         Specialty specialty
) {
}
