package com.andrei.demo.service;

import com.andrei.demo.model.LoginResponse;
import com.andrei.demo.model.Person;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.util.JwtUtil;
import com.andrei.demo.util.PasswordUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class SecurityService {
    private final PersonRepository personRepository;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;

    public LoginResponse login(String email, String password) {
        log.info("=== LOGIN ATTEMPT ===");
        log.info("Email: {}", email);
        log.info("Password provided (length): {}", password != null ? password.length() : "null");

        Optional<Person> maybePerson = personRepository.findByEmail(email);

        if (maybePerson.isEmpty()) {
            log.warn("LOGIN FAILED: No person found with email '{}'", email);
            return new LoginResponse(
                    "Person with email " + email + " not found"
            );
        }

        Person person = maybePerson.get();
        log.info("Person found: name={}, role={}", person.getName(), person.getRole());

        String storedPassword = person.getPassword();
        log.info("Stored password starts with: {}",
                storedPassword != null && storedPassword.length() > 10
                        ? storedPassword.substring(0, 10) + "..."
                        : storedPassword);

        boolean isBcryptHash = storedPassword != null && storedPassword.startsWith("$2");
        log.info("Stored password is BCrypt hash: {}", isBcryptHash);

        if (!isBcryptHash) {
            log.error("PLAIN TEXT PASSWORD DETECTED for email '{}'. " +
                    "The stored password is not a BCrypt hash. " +
                    "Call POST /dev/reset-all-passwords to fix this.", email);
            return new LoginResponse(
                    "Account setup error: password is not properly hashed. " +
                            "Please contact an administrator."
            );
        }

        boolean passwordMatches = passwordUtil.checkPassword(password, storedPassword);
        log.info("Password matches: {}", passwordMatches);

        if (passwordMatches) {
            String token = jwtUtil.createToken(person);
            String role = person.getRole() != null ? person.getRole().name() : "CUSTOMER";
            log.info("LOGIN SUCCESS: email={}, role={}", email, role);
            return new LoginResponse(role, token);
        } else {
            log.warn("LOGIN FAILED: Incorrect password for email '{}'", email);
            return new LoginResponse("Incorrect password");
        }
    }
}