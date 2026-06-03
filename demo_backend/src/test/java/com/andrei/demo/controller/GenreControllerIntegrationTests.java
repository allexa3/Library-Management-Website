package com.andrei.demo.controller;

import com.andrei.demo.model.Person;
import com.andrei.demo.model.UserRole;
import com.andrei.demo.repository.BookRepository;
import com.andrei.demo.repository.GenreRepository;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.util.JwtUtil;
import com.andrei.demo.util.PasswordUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class GenreControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private GenreRepository genreRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordUtil passwordUtil;

    // Prevent Spring from wiring a real SMTP connection during integration tests
    @MockBean private JavaMailSender javaMailSender;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        bookRepository.flush();
        genreRepository.deleteAll();
        genreRepository.flush();
        personRepository.deleteAll();
        personRepository.flush();

        Person admin = new Person();
        admin.setName("Admin");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordUtil.hashPassword("Admin@123!"));
        admin.setAge(30);
        admin.setRole(UserRole.ADMIN);
        adminToken = jwtUtil.createToken(personRepository.save(admin));

        Person customer = new Person();
        customer.setName("Customer");
        customer.setEmail("customer@test.com");
        customer.setPassword(passwordUtil.hashPassword("Customer@1!"));
        customer.setAge(22);
        customer.setRole(UserRole.CUSTOMER);
        customerToken = jwtUtil.createToken(personRepository.save(customer));
    }

    // ── GET /genre ───────────────────────────────────────────────────────────

    @Test
    void testGetAllGenres_EmptyInitially() throws Exception {
        mockMvc.perform(get("/genre")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetAllGenres_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/genre"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAllGenres_CustomerForbidden() throws Exception {
        // /genre/** is ADMIN-only
        mockMvc.perform(get("/genre")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    // ── POST /genre ──────────────────────────────────────────────────────────

    @Test
    void testCreateGenre_ValidPayload() throws Exception {
        Map<String, String> dto = Map.of("name", "Science Fiction");

        mockMvc.perform(post("/genre")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Science Fiction"));
    }

    @Test
    void testCreateGenre_DuplicateName_ReturnsBadRequest() throws Exception {
        Map<String, String> dto = Map.of("name", "Fantasy");

        mockMvc.perform(post("/genre")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/genre")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testCreateGenre_NameTooShort_ReturnsBadRequest() throws Exception {
        Map<String, String> dto = Map.of("name", "ab"); // min is 3

        mockMvc.perform(post("/genre")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    // ── GET /genre/{id} ──────────────────────────────────────────────────────

    @Test
    void testGetById_Found() throws Exception {
        Map<String, String> dto = Map.of("name", "Mystery");
        String resp = mockMvc.perform(post("/genre")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();

        String genreId = objectMapper.readTree(resp).get("id").asText();

        mockMvc.perform(get("/genre/" + genreId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mystery"));
    }

    @Test
    void testGetById_NotFound_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/genre/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /genre/{id} ──────────────────────────────────────────────────────

    @Test
    void testUpdateGenre_Success() throws Exception {
        Map<String, String> createDto = Map.of("name", "OldName");
        String createResponse = mockMvc.perform(post("/genre")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andReturn().getResponse().getContentAsString();

        String genreId = objectMapper.readTree(createResponse).get("id").asText();

        Map<String, String> updateDto = Map.of("name", "NewName");
        mockMvc.perform(put("/genre/" + genreId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"));
    }

    // ── PATCH /genre/{id} ────────────────────────────────────────────────────

    @Test
    void testPatchGenre_UpdatesName() throws Exception {
        Map<String, String> createDto = Map.of("name", "PatchMe");
        String resp = mockMvc.perform(post("/genre")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andReturn().getResponse().getContentAsString();

        String genreId = objectMapper.readTree(resp).get("id").asText();

        Map<String, String> patchDto = Map.of("name", "Patched");
        mockMvc.perform(patch("/genre/" + genreId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Patched"));
    }

    // ── DELETE /genre/{id} ───────────────────────────────────────────────────

    @Test
    void testDeleteGenre_RemovesGenre() throws Exception {
        Map<String, String> dto = Map.of("name", "ToDelete");
        String resp = mockMvc.perform(post("/genre")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn().getResponse().getContentAsString();

        String genreId = objectMapper.readTree(resp).get("id").asText();

        mockMvc.perform(delete("/genre/" + genreId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/genre")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.length()").value(0));
    }
}