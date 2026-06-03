package com.andrei.demo.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetCode(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Your Password Reset Code");
            message.setText(
                    "You requested a password reset.\n\n" +
                            "Your reset code is: " + code + "\n\n" +
                            "This code expires in 15 minutes.\n\n" +
                            "If you did not request this, please ignore this email."
            );
            mailSender.send(message);
            log.info("Password reset code sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send reset email. Please try again later.");
        }
    }

    public void sendPasswordChangedConfirmation(String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Your Password Was Changed");
            message.setText(
                    "This is a confirmation that your password was recently updated.\n\n" +
                            "If you did not make this change, please contact support immediately."
            );
            mailSender.send(message);
            log.info("Password change confirmation sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }
}