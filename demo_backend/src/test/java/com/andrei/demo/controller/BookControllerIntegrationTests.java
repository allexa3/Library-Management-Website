package com.andrei.demo.controller;

import com.andrei.demo.model.Genre;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class BookControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private BookRepository bookRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordUtil passwordUtil;

    // Prevent Spring from wiring a real SMTP connection during integration tests
    @MockBean private JavaMailSender javaMailSender;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;
    private String customerToken;
    private UUID personId;
    private UUID genreId;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        bookRepository.flush();
        personRepository.deleteAll();
        personRepository.flush();
        genreRepository.deleteAll();
        genreRepository.flush();

        Person admin = new Person();
        admin.setName("Test Admin");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordUtil.hashPassword("Admin@1234!"));
        admin.setAge(30);
        admin.setRole(UserRole.ADMIN);
        Person savedAdmin = personRepository.save(admin);
        personId = savedAdmin.getId();
        adminToken = jwtUtil.createToken(savedAdmin);

        Person customer = new Person();
        customer.setName("Test Customer");
        customer.setEmail("customer@example.com");
        customer.setPassword(passwordUtil.hashPassword("Customer@1!"));
        customer.setAge(25);
        customer.setRole(UserRole.CUSTOMER);
        Person savedCustomer = personRepository.save(customer);
        customerToken = jwtUtil.createToken(savedCustomer);

        Genre genre = new Genre();
        genre.setName("Test Genre");
        genreId = genreRepository.save(genre).getId();
    }

    // ── GET /books ───────────────────────────────────────────────────────────

    @Test
    void testGetAllBooks_EmptyInitially() throws Exception {
        mockMvc.perform(get("/books")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetAllBooks_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /books ──────────────────────────────────────────────────────────

    @Test
    void testCreateBook_ValidPayload() throws Exception {
        Map<String, Object> dto = Map.of(
                "title", "The Clean Coder",
                "authorName", "Robert Martin",
                "isbn", "9780137081073",
                "personId", personId.toString(),
                "genreIds", new String[]{genreId.toString()}
        );

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("The Clean Coder"))
                .andExpect(jsonPath("$.isbn").value("9780137081073"))
                .andExpect(jsonPath("$.borrowedBy.id").value(personId.toString()));
    }

    @Test
    void testCreateBook_WithoutBorrower_IsAvailable() throws Exception {
        Map<String, Object> dto = Map.of(
                "title", "Available Book",
                "authorName", "Author",
                "isbn", "9780132350884",
                "genreIds", new String[]{genreId.toString()}
        );

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.borrowedBy").doesNotExist());
    }

    @Test
    void testCreateBook_MissingTitle_ReturnsBadRequest() throws Exception {
        Map<String, Object> dto = Map.of(
                "authorName", "Author",
                "isbn", "9780137081073",
                "genreIds", new String[]{genreId.toString()}
        );

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void testCreateBook_InvalidIsbn_ReturnsBadRequest() throws Exception {
        Map<String, Object> dto = Map.of(
                "title", "Bad ISBN Book",
                "authorName", "Author",
                "isbn", "1234567890", // not a valid ISBN-13
                "genreIds", new String[]{genreId.toString()}
        );

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isbn").exists());
    }

    @Test
    void testCreateBook_DuplicateIsbn_ReturnsBadRequest() throws Exception {
        Map<String, Object> dto = Map.of(
                "title", "Book One",
                "authorName", "Author",
                "isbn", "9780137081073",
                "genreIds", new String[]{genreId.toString()}
        );

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        Map<String, Object> dto2 = Map.of(
                "title", "Book Two",
                "authorName", "Author",
                "isbn", "9780137081073", // same ISBN
                "genreIds", new String[]{genreId.toString()}
        );

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testCreateBook_BorrowLimitExceeded_ReturnsBadRequest() throws Exception {
        for (int i = 0; i < 3; i++) {
            Map<String, Object> dto = Map.of(
                    "title", "Borrowed Book " + i,
                    "authorName", "Author",
                    "isbn", "978000000000" + i,
                    "personId", personId.toString(),
                    "genreIds", new String[]{genreId.toString()}
            );
            mockMvc.perform(post("/books")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());
        }

        Map<String, Object> fourth = Map.of(
                "title", "One Too Many",
                "authorName", "Author",
                "isbn", "9780000000004",
                "personId", personId.toString(),
                "genreIds", new String[]{genreId.toString()}
        );

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fourth)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("You have reached the maximum limit of 3 borrowed books."));
    }

    // ── POST /books/{id}/borrow ──────────────────────────────────────────────

    @Test
    void testBorrowBook_Success() throws Exception {
        // Create an unassigned book
        Map<String, Object> dto = Map.of(
                "title", "Borrowable Book",
                "authorName", "Author",
                "isbn", "9780132350884",
                "genreIds", new String[]{genreId.toString()}
        );

        String response = mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String bookId = objectMapper.readTree(response).get("id").asText();

        // Customer borrows it
        mockMvc.perform(post("/books/" + bookId + "/borrow")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.borrowedBy").exists());
    }

    @Test
    void testBorrowBook_AlreadyBorrowed_ReturnsBadRequest() throws Exception {
        // Create a book already borrowed by the admin
        Map<String, Object> dto = Map.of(
                "title", "Already Borrowed",
                "authorName", "Author",
                "isbn", "9780132350884",
                "personId", personId.toString(),
                "genreIds", new String[]{genreId.toString()}
        );

        String response = mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String bookId = objectMapper.readTree(response).get("id").asText();

        // Customer tries to borrow — should fail
        mockMvc.perform(post("/books/" + bookId + "/borrow")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("This book is already borrowed."));
    }

    // ── PUT /books/{id} ──────────────────────────────────────────────────────

    @Test
    void testUpdateBook_Success() throws Exception {
        Map<String, Object> create = Map.of(
                "title", "Original Title",
                "authorName", "Author",
                "isbn", "9780132350884",
                "genreIds", new String[]{genreId.toString()}
        );

        String resp = mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andReturn().getResponse().getContentAsString();

        String bookId = objectMapper.readTree(resp).get("id").asText();

        Map<String, Object> update = Map.of(
                "title", "Updated Title",
                "authorName", "New Author",
                "isbn", "9780132350884",
                "genreIds", new String[]{genreId.toString()}
        );

        mockMvc.perform(put("/books/" + bookId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.authorName").value("New Author"));
    }

    @Test
    void testUpdateBook_BorrowLimitExceeded_ReturnsBadRequest() throws Exception {
        for (int i = 0; i < 3; i++) {
            Map<String, Object> dto = Map.of(
                    "title", "Book " + i,
                    "authorName", "Author",
                    "isbn", "978111111111" + i,
                    "personId", personId.toString(),
                    "genreIds", new String[]{genreId.toString()}
            );
            mockMvc.perform(post("/books")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());
        }

        Map<String, Object> unassigned = Map.of(
                "title", "Unassigned Book",
                "authorName", "Author",
                "isbn", "9781111111114",
                "genreIds", new String[]{genreId.toString()}
        );

        String unassignedJson = mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unassigned)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String unassignedId = objectMapper.readTree(unassignedJson).get("id").asText();

        Map<String, Object> updatePayload = Map.of(
                "title", "Now Assigned",
                "authorName", "Author",
                "isbn", "9781111111114",
                "personId", personId.toString(),
                "genreIds", new String[]{genreId.toString()}
        );

        mockMvc.perform(put("/books/" + unassignedId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("You have reached the maximum limit of 3 borrowed books."));
    }

    // ── DELETE /books/{id} ───────────────────────────────────────────────────

    @Test
    void testDeleteBook_RemovesBook() throws Exception {
        Map<String, Object> dto = Map.of(
                "title", "Delete Me",
                "authorName", "Author",
                "isbn", "9780132350884",
                "genreIds", new String[]{genreId.toString()}
        );

        String responseJson = mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String bookId = objectMapper.readTree(responseJson).get("id").asText();

        mockMvc.perform(delete("/books/" + bookId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/books")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── role-based access ────────────────────────────────────────────────────

    @Test
    void testCustomerCanGetBooks() throws Exception {
        mockMvc.perform(get("/books")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }
}