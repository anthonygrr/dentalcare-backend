package com.dentalcare.tony.api.presentation.dto.resquest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record PatientCreationRequest(
        @NotBlank(message = "Name is mandatory")
        String name,

         @NotBlank(message = "Email is mandatory")
         @Email(message = "Email not valid")
         @Pattern(
                 regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                 message = "Email must include a valid domain (e.g. .com)"
         )
         String email,

         @NotBlank(message = "Phone number is mandatory")
         String phone,

         @NotNull(message = "Birth Date is mandatory")
         LocalDate birthDate
) {
}
