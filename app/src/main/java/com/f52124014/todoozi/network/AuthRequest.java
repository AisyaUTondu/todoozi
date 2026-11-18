package com.f52124014.todoozi.network;

/** Body JSON untuk POST /auth/login dan /auth/register. */
public class AuthRequest {
    private final String username;
    private final String password;

    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
