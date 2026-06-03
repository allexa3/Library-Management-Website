package com.andrei.demo.controller;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Book;
import com.andrei.demo.model.BookCreateDTO;
import com.andrei.demo.service.BookService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/books")
@AllArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping("/{id}")
    public Book getById(@PathVariable UUID id) throws ValidationException {
        return bookService.getById(id);
    }

    @GetMapping
    public List<Book> getAll() {
        return bookService.getAll();
    }

    @PostMapping
    public Book create(@RequestBody @Valid BookCreateDTO dto) throws ValidationException {
        return bookService.create(dto);
    }

    @PatchMapping("/{id}")
    public Book patch(@PathVariable UUID id, @RequestBody Map<String, Object> updates) throws ValidationException {
        return bookService.patch(id, updates);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        bookService.delete(id);
    }

    @PutMapping("/{id}")
    public Book update(@PathVariable UUID id, @RequestBody @Valid BookCreateDTO dto) throws ValidationException {
        return bookService.update(id, dto);
    }

    @PostMapping("/{id}/borrow")
    public Book borrow(@PathVariable UUID id, Authentication authentication) throws ValidationException {
        String userId = (String) authentication.getPrincipal();
        return bookService.borrowBook(id, UUID.fromString(userId));
    }
}