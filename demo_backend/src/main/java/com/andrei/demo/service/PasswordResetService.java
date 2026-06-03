package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.PasswordResetToken;
import com.andrei.demo.model.Person;
import com.andrei.demo.repository.PasswordResetTokenRepository;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.util.PasswordUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final PersonRepository personRepository;
    private final PasswordUtil passwordUtil;
    private final EmailService emailService;

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 15;

    @Transactional
    public void initiateReset(String email) throws ValidationException {
        personRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException("No account found with email: " + email));

        // Remove any existing tokens for this email
        tokenRepository.deleteByEmail(email);

        String code = generateCode();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(code);
        resetToken.setEmail(email);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        emailService.sendPasswordResetCode(email, code);
        log.info("Password reset initiated for email: {}", email);
    }

    @Transactional
    public void completeReset(String email, String code, String newPassword, String confirmPassword)
            throws ValidationException {

        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match.");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(code)
                .orElseThrow(() -> new ValidationException("Invalid or expired reset code."));

        if (!resetToken.getEmail().equals(email)) {
            throw new ValidationException("Invalid reset code for this email.");
        }

        if (resetToken.isUsed()) {
            throw new ValidationException("This reset code has already been used.");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Reset code has expired. Please request a new one.");
        }

        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException("No account found with email: " + email));

        person.setPassword(passwordUtil.hashPassword(newPassword));
        personRepository.save(person);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password successfully reset for email: {}", email);

        emailService.sendPasswordChangedConfirmation(email);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}