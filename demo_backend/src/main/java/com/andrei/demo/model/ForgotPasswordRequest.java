package com.andrei.demo.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "New password is required")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {}