package com.f52124014.todoozi.network;

/** Balasan server: { "message": "...", "token": "...", "user": { id, username, avatar_url } } */
public class AuthResponse {
    private String message;
    private String token;
    private UserDto user;

    public String getMessage() { return message; }
    public String getToken() { return token; }
    public UserDto getUser() { return user; }
}
