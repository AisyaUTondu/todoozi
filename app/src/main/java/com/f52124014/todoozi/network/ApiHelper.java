package com.f52124014.todoozi.network;

import com.google.gson.Gson;

import retrofit2.Response;

public final class ApiHelper {

    private ApiHelper() {}

    /** Ambil pesan error dari body JSON server, atau kode HTTP jika tidak ada. */
    public static String errorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                MessageResponse parsed = new Gson().fromJson(raw, MessageResponse.class);
                if (parsed != null && parsed.getMessage() != null && !parsed.getMessage().isEmpty()) {
                    return parsed.getMessage();
                }
            }
        } catch (Exception ignored) {
            // abaikan, pakai pesan default di bawah
        }
        return "Server error (" + response.code() + ")";
    }
}
