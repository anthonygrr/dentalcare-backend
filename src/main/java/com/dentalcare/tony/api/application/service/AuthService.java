package com.dentalcare.tony.api.application.service;

import com.dentalcare.tony.api.domain.entity.Permission;
import com.dentalcare.tony.api.domain.entity.Role;
import com.dentalcare.tony.api.domain.entity.User;
import com.dentalcare.tony.api.infrastructure.persistence.repository.JpaUserRepository;
import com.dentalcare.tony.api.infrastructure.security.JwtService;
import com.dentalcare.tony.api.presentation.dto.request.LoginRequest;
import com.dentalcare.tony.api.presentation.dto.request.RefreshTokenRequest;
import com.dentalcare.tony.api.presentation.dto.response.AuthResponse;
import com.dentalcare.tony.api.presentation.dto.response.UserResponse;
import com.dentalcare.tony.api.presentation.exception.InvalidCredentialsException;
import com.dentalcare.tony.api.presentation.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JpaUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {

        User user = userRepository
                .findByUsernameOrEmail(
                        request.login(),
                        request.login()
                )
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )) {
            throw new InvalidCredentialsException(
                    "Invalid credentials"
            );
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(
            RefreshTokenRequest request
    ) {

        String username =
                jwtService.extractUsername(
                        request.refreshToken()
                );

        User user = userRepository
                .findByUsernameOrEmail(
                        username,
                        username
                )
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(toUserResponse(user))
                .build();
    }

    private UserResponse toUserResponse(User user) {

        Set<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles()
                .stream()
                .flatMap(role ->
                        role.getPermissions().stream())
                .map(permission ->
                        permission.getName().name())
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .permissions(permissions)
                .build();
    }
}
