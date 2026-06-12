package com.dentalcare.tony.api.application.service;

import com.dentalcare.tony.api.domain.entity.User;
import com.dentalcare.tony.api.infrastructure.persistence.repository.JpaUserRepository;
import com.dentalcare.tony.api.infrastructure.security.UserPrincipal;
import com.dentalcare.tony.api.presentation.dto.request.UpdatePasswordRequest;
import com.dentalcare.tony.api.presentation.dto.response.UserResponse;
import com.dentalcare.tony.api.presentation.exception.BusinessException;
import com.dentalcare.tony.api.presentation.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final JpaUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getCurrentUser() {

        User user = getAuthenticatedUser();

        return toUserResponse(user);
    }

    public void updatePassword(
            UpdatePasswordRequest request
    ) {

        User user = getAuthenticatedUser();

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPassword()
        )) {

            throw new BusinessException(
                    "Current password is incorrect"
            );
        }

        user.setPassword(
                passwordEncoder.encode(
                        request.newPassword()
                )
        );

        userRepository.save(user);
    }

    private User getAuthenticatedUser() {

        UserPrincipal principal =
                (UserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        return userRepository
                .findById(
                        principal.getUser().getId()
                )
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found"
                        ));
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
