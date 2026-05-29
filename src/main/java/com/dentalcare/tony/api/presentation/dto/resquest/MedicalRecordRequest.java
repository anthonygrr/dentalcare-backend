package com.dentalcare.tony.api.presentation.dto.resquest;

public record MedicalRecordRequest(
        String diagnosis,

        String treatment
) {
}
