package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Genre;
import com.andrei.demo.model.GenreCreateDTO;
import com.andrei.demo.repository.GenreRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class GenreService {
    private final GenreRepository genreRepository;

    public List<Genre> getAll() {
        return genreRepository.findAll();
    }

    public Genre getById(UUID id) throws ValidationException {
        return genreRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Genre with id " + id + " not found"));
    }

    public Genre create(GenreCreateDTO dto) throws ValidationException {
        if (genreRepository.findByName(dto.getName()).isPresent()) {
            throw new ValidationException("Genre with name '" + dto.getName() + "' already exists.");
        }

        Genre genre = new Genre();
        genre.setName(dto.getName());
        return genreRepository.save(genre);
    }

    public Genre update(UUID id, GenreCreateDTO dto) throws ValidationException {
        Genre existingGenre = getById(id);

        genreRepository.findByName(dto.getName()).ifPresent(found -> {
            if (!found.getId().equals(id)) {
                throw new RuntimeException("Genre with name '" + dto.getName() + "' already exists.");
            }
        });

        existingGenre.setName(dto.getName());
        return genreRepository.save(existingGenre);
    }

    public Genre patch(UUID id, Map<String, Object> updates) throws ValidationException {
        Genre genre = getById(id);

        if (updates.containsKey("name")) {
            String newName = (String) updates.get("name");
            genreRepository.findByName(newName).ifPresent(found -> {
                if (!found.getId().equals(id)) {
                    throw new RuntimeException("Genre with name '" + newName + "' already exists.");
                }
            });
            genre.setName(newName);
        }

        return genreRepository.save(genre);
    }

    public void delete(UUID id) {
        genreRepository.deleteById(id);
    }
}