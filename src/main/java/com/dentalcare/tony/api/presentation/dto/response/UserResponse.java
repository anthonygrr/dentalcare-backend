package com.dentalcare.tony.api.presentation.dto.response;

import lombok.Builder;

import java.util.Set;

@Builder
public record UserResponse(
        Long id,
        String fullName,
        String username,
        String email,
        Set<String> roles,
        Set<String> permissions

) {}
