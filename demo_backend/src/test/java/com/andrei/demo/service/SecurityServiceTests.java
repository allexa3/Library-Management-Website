package com.andrei.demo.service;

import com.andrei.demo.model.LoginResponse;
import com.andrei.demo.model.Person;
import com.andrei.demo.model.UserRole;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.util.JwtUtil;
import com.andrei.demo.util.PasswordUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityServiceTests {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PasswordUtil passwordUtil;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private SecurityService securityService;

    private AutoCloseable closeable;

    // Fixed credentials — plain-text password used as the login input,
    // stored hash is what the DB would hold after BCrypt encoding.
    private static final String PLAIN_PASSWORD = "Password123!";
    private static final String BCRYPT_HASH =
            "$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOnu";

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private Person buildPerson(String email, String storedPassword, UserRole role) {
        Person p = new Person();
        p.setEmail(email);
        p.setPassword(storedPassword);
        p.setRole(role);
        return p;
    }

    // ── login success ────────────────────────────────────────────────────────

    @Test
    void testLogin_Success_Admin() {
        String email = "admin@example.com";
        String token = "jwt-token-123";
        Person person = buildPerson(email, BCRYPT_HASH, UserRole.ADMIN);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(passwordUtil.checkPassword(PLAIN_PASSWORD, BCRYPT_HASH)).thenReturn(true);
        when(jwtUtil.createToken(person)).thenReturn(token);

        LoginResponse result = securityService.login(email, PLAIN_PASSWORD);

        assertTrue(result.success());
        assertEquals("ADMIN", result.role());
        assertEquals(token, result.token());
        assertNull(result.errorMessage());
        verify(personRepository).findByEmail(email);
        verify(passwordUtil).checkPassword(PLAIN_PASSWORD, BCRYPT_HASH);
        verify(jwtUtil).createToken(person);
    }

    @Test
    void testLogin_Success_Customer() {
        String email = "customer@example.com";
        String token = "jwt-customer-token";
        Person person = buildPerson(email, BCRYPT_HASH, UserRole.CUSTOMER);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(passwordUtil.checkPassword(PLAIN_PASSWORD, BCRYPT_HASH)).thenReturn(true);
        when(jwtUtil.createToken(person)).thenReturn(token);

        LoginResponse result = securityService.login(email, PLAIN_PASSWORD);

        assertTrue(result.success());
        assertEquals("CUSTOMER", result.role());
        assertEquals(token, result.token());
    }

    @Test
    void testLogin_Success_NullRole_DefaultsToCustomer() {
        // The service defaults to "CUSTOMER" when role is null
        String email = "norole@example.com";
        String token = "jwt-token";
        Person person = buildPerson(email, BCRYPT_HASH, null);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(passwordUtil.checkPassword(PLAIN_PASSWORD, BCRYPT_HASH)).thenReturn(true);
        when(jwtUtil.createToken(person)).thenReturn(token);

        LoginResponse result = securityService.login(email, PLAIN_PASSWORD);

        assertTrue(result.success());
        assertEquals("CUSTOMER", result.role());
    }

    // ── login failure — wrong password ───────────────────────────────────────

    @Test
    void testLogin_IncorrectPassword_ReturnsFailed() {
        String email = "test@example.com";
        String wrongPassword = "WrongPass@99";
        Person person = buildPerson(email, BCRYPT_HASH, UserRole.ADMIN);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(passwordUtil.checkPassword(wrongPassword, BCRYPT_HASH)).thenReturn(false);

        LoginResponse result = securityService.login(email, wrongPassword);

        assertFalse(result.success());
        assertEquals("Incorrect password", result.errorMessage());
        assertNull(result.token());
        assertNull(result.role());
        verify(jwtUtil, never()).createToken(any());
    }

    // ── login failure — email not found ──────────────────────────────────────

    @Test
    void testLogin_EmailNotFound_ReturnsFailed() {
        String email = "ghost@example.com";

        when(personRepository.findByEmail(email)).thenReturn(Optional.empty());

        LoginResponse result = securityService.login(email, PLAIN_PASSWORD);

        assertFalse(result.success());
        assertEquals("Person with email " + email + " not found", result.errorMessage());
        assertNull(result.token());
        verifyNoInteractions(passwordUtil, jwtUtil);
    }

    // ── login failure — plain-text password in DB ────────────────────────────

    @Test
    void testLogin_PlainTextPasswordStored_ReturnsFailed() {
        // The service explicitly checks for a BCrypt hash (starts with "$2")
        // and rejects login if the stored password is plain text.
        String email = "plain@example.com";
        Person person = buildPerson(email, "notAHash", UserRole.CUSTOMER);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));

        LoginResponse result = securityService.login(email, PLAIN_PASSWORD);

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("password is not properly hashed") ||
                result.errorMessage().contains("Account setup error"));
        verifyNoInteractions(jwtUtil);
        // passwordUtil.checkPassword should never be called when the hash is invalid
        verify(passwordUtil, never()).checkPassword(any(), any());
    }
}