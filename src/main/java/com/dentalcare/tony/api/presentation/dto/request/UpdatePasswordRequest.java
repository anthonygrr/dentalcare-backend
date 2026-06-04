package com.dentalcare.tony.api.presentation.dto.request;

public record UpdatePasswordRequest(
        String currentPassword,
        String newPassword
) {
}
