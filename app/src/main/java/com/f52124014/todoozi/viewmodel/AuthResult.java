package com.f52124014.todoozi.viewmodel;

/** Hasil proses login / register yang dikirim dari AuthViewModel ke Activity. */
public class AuthResult {

    public static final int TYPE_LOGIN = 1;
    public static final int TYPE_REGISTER = 2;

    public final int type;
    public final boolean success;
    public final String message;
    public final String username;

    private AuthResult(int type, boolean success, String message, String username) {
        this.type = type;
        this.success = success;
        this.message = message;
        this.username = username;
    }

    public static AuthResult success(int type, String username) {
        return new AuthResult(type, true, null, username);
    }

    public static AuthResult failure(int type, String message) {
        return new AuthResult(type, false, message, null);
    }
}
