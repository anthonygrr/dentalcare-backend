package com.dentalcare.tony.api.presentation.dto.request;

public record MedicalRecordRequest(
        String diagnosis,

        String treatment
) {
}
