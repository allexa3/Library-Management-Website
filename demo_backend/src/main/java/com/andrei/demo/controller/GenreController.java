package com.andrei.demo.controller;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Genre;
import com.andrei.demo.model.GenreCreateDTO;
import com.andrei.demo.service.GenreService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/genre")
@AllArgsConstructor
public class GenreController {
    private final GenreService genreService;

    @GetMapping
    public List<Genre> getAll() { return genreService.getAll(); }

    @GetMapping("/{id}")
    public Genre getById(@PathVariable UUID id) throws ValidationException {
        return genreService.getById(id);
    }

    @PostMapping
    public Genre create(@RequestBody GenreCreateDTO dto) throws ValidationException {
        return genreService.create(dto);
    }

    @PatchMapping("/{id}")
    public Genre patch(@PathVariable UUID id, @RequestBody Map<String, Object> updates) throws ValidationException {
        return genreService.patch(id, updates);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) { genreService.delete(id); }

    @PutMapping("/{id}")
    public Genre update(@PathVariable UUID id, @RequestBody GenreCreateDTO dto) throws ValidationException {
        return genreService.update(id, dto);
    }
}