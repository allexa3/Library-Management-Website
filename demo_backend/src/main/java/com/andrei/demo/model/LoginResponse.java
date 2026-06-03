package com.andrei.demo.model;

public record LoginResponse(
        Boolean success,
        String role,
        String errorMessage,
        String token
) {
    public LoginResponse(String errorMessage){
        this(false, null, errorMessage, null);
    }
    public LoginResponse(String role, String token){
        this(true, role, null, token);
    }
}
