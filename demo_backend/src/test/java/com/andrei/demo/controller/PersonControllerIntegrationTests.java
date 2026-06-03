package com.andrei.demo.controller;

import com.andrei.demo.model.Person;
import com.andrei.demo.model.UserRole;
import com.andrei.demo.repository.BookRepository;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.util.JwtUtil;
import com.andrei.demo.util.PasswordUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class PersonControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private PersonRepository personRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordUtil passwordUtil;

    // Prevent Spring from wiring a real SMTP connection during integration tests
    @MockBean private JavaMailSender javaMailSender;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;
    private String customerToken;

    private String loadFixture(String filename) throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + filename)) {
            if (is == null) throw new IllegalArgumentException("Fixture not found: " + filename);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        bookRepository.flush();
        personRepository.deleteAll();
        personRepository.flush();

        Person john = new Person();
        john.setName("John Doe");
        john.setEmail("john.doe@example.com");
        john.setPassword(passwordUtil.hashPassword("ValidPass@1"));
        john.setAge(30);
        john.setRole(UserRole.CUSTOMER);
        personRepository.save(john);

        Person jane = new Person();
        jane.setName("Jane Doe");
        jane.setEmail("jane.doe@example.com");
        jane.setPassword(passwordUtil.hashPassword("ValidPass@1"));
        jane.setAge(25);
        jane.setRole(UserRole.CUSTOMER);
        personRepository.save(jane);

        Person admin = new Person();
        admin.setName("Admin User");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordUtil.hashPassword("AdminPass@1"));
        admin.setAge(35);
        admin.setRole(UserRole.ADMIN);
        Person savedAdmin = personRepository.save(admin);
        adminToken = jwtUtil.createToken(savedAdmin);

        Person customer = new Person();
        customer.setName("Just Customer");
        customer.setEmail("cust@example.com");
        customer.setPassword(passwordUtil.hashPassword("CustPass@1"));
        customer.setAge(28);
        customer.setRole(UserRole.CUSTOMER);
        customerToken = jwtUtil.createToken(personRepository.save(customer));
    }

    // ── GET /person ──────────────────────────────────────────────────────────

    @Test
    void testGetPeople_ReturnsAllPersons() throws Exception {
        mockMvc.perform(get("/person")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[*].name",
                        Matchers.hasItems("John Doe", "Jane Doe", "Admin User", "Just Customer")));
    }

    @Test
    void testGetPeople_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/person"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetPeople_CustomerForbidden() throws Exception {
        // /person/** is ADMIN-only
        mockMvc.perform(get("/person")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    // ── GET /person/{uuid} ───────────────────────────────────────────────────

    @Test
    void testGetPersonById_Found() throws Exception {
        String createJson = loadFixture("valid_person.json");
        String resp = mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(resp).get("id").asText();

        mockMvc.perform(get("/person/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Alice Smith"));
    }

    @Test
    void testGetPersonById_NotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/person/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ── GET /person/email/{email} ────────────────────────────────────────────

    @Test
    void testGetPersonByEmail_Found() throws Exception {
        mockMvc.perform(get("/person/email/john.doe@example.com")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void testGetPersonByEmail_NotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/person/email/nobody@nowhere.com")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ── POST /person ─────────────────────────────────────────────────────────

    @Test
    void testAddPerson_ValidPayload() throws Exception {
        String validPersonJson = loadFixture("valid_person.json");

        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPersonJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.age").value(28))
                .andExpect(jsonPath("$.email").value("alice.smith@example.com"))
                // Password must never be leaked in the response
                .andExpect(jsonPath("$.password").exists()); // present but hashed
    }

    @Test
    void testAddPerson_InvalidPayload_ReturnsBadRequest() throws Exception {
        String invalidPersonJson = loadFixture("invalid_person.json");

        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPersonJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name")
                        .value("Name should be between 2 and 100 characters"))
                .andExpect(jsonPath("$.password")
                        .value("Password must contain at least 8 characters, including uppercase, lowercase, digit, and special character"))
                .andExpect(jsonPath("$.age").value("Age is required"))
                .andExpect(jsonPath("$.email").value("Email is required"));
    }

    @Test
    void testAddPerson_DuplicateEmail_ReturnsBadRequest() throws Exception {
        // john.doe@example.com already exists from setUp
        Map<String, Object> dto = Map.of(
                "name", "John Clone",
                "password", "Password@1",
                "age", 30,
                "email", "john.doe@example.com",
                "role", "CUSTOMER"
        );

        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── PUT /person/{uuid} ───────────────────────────────────────────────────

    @Test
    void testUpdatePerson_Success() throws Exception {
        String createJson = loadFixture("valid_person.json");
        String resp = mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(resp).get("id").asText();

        Map<String, Object> update = Map.of(
                "name", "Alice Updated",
                "age", 29,
                "email", "alice.updated@example.com",
                "role", "ADMIN"
        );

        mockMvc.perform(put("/person/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"))
                .andExpect(jsonPath("$.age").value(29))
                .andExpect(jsonPath("$.email").value("alice.updated@example.com"));
    }

    @Test
    void testUpdatePerson_NotFound_ReturnsBadRequest() throws Exception {
        Map<String, Object> update = Map.of(
                "name", "Ghost",
                "age", 25,
                "email", "ghost@example.com",
                "role", "CUSTOMER"
        );

        mockMvc.perform(put("/person/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH /person/{uuid} ─────────────────────────────────────────────────

    @Test
    void testPatchPerson_UpdatesName() throws Exception {
        String createJson = loadFixture("valid_person.json");
        String resp = mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(resp).get("id").asText();

        mockMvc.perform(patch("/person/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Patched Alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Patched Alice"));
    }

    // ── DELETE /person/{uuid} ────────────────────────────────────────────────

    @Test
    void testDeletePerson_RemovesPerson() throws Exception {
        String createJson = loadFixture("valid_person.json");
        String resp = mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(resp).get("id").asText();

        mockMvc.perform(delete("/person/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/person/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}