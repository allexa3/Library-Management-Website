package com.andrei.demo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenreCreateDTO {
    @NotBlank(message = "Genre name is required")
    @Size(min = 3, max = 50, message = "Genre name must be between 3 and 50 characters")
    private String name;
}