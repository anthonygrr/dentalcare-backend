package com.dentalcare.tony.api.presentation.controller;

import com.dentalcare.tony.api.application.service.UserService;
import com.dentalcare.tony.api.presentation.dto.request.UpdatePasswordRequest;
import com.dentalcare.tony.api.presentation.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {

        return ResponseEntity.ok(
                userService.getCurrentUser()
        );
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @RequestBody UpdatePasswordRequest request
    ) {

        userService.updatePassword(request);

        return ResponseEntity.noContent().build();
    }
}
