package com.andrei.demo.model;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Reset code is required")
        String code,

        @NotBlank(message = "New password is required")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {}