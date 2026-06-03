package com.andrei.demo.controller;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.ForgotPasswordRequest;
import com.andrei.demo.model.ResetPasswordRequest;
import com.andrei.demo.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/password")
@AllArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) throws ValidationException {

        if (!request.newPassword().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Passwords do not match."));
        }

        passwordResetService.initiateReset(request.email());
        return ResponseEntity.ok(Map.of("message",
                "A reset code has been sent to your email address."));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) throws ValidationException {

        passwordResetService.completeReset(
                request.email(),
                request.code(),
                request.newPassword(),
                request.confirmPassword()
        );
        return ResponseEntity.ok(Map.of("message",
                "Password has been successfully reset. You may now log in."));
    }
}