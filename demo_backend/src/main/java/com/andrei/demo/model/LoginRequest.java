package com.andrei.demo.model;

public record LoginRequest(
        String email,
        String password
) {
}
