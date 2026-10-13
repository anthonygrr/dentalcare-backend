package com.dentalcare.tony.api.presentation.controller;

import com.dentalcare.tony.api.application.service.AuthService;
import com.dentalcare.tony.api.presentation.dto.request.LoginRequest;
import com.dentalcare.tony.api.presentation.dto.request.RefreshTokenRequest;
import com.dentalcare.tony.api.presentation.dto.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody RefreshTokenRequest request
    ) {

        return ResponseEntity.ok(
                authService.refreshToken(request)
        );
    }
}
