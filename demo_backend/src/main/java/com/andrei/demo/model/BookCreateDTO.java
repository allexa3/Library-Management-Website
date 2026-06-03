package com.andrei.demo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class BookCreateDTO {
    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^(978|979)[0-9]{10}$", message = "Invalid ISBN-13 format")
    private String isbn;

    private UUID personId;

    private String authorName;

    @Size(min = 1, message = "At least one genre must be selected")
    private List<UUID> genreIds;
}