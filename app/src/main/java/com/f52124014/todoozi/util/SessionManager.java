package com.f52124014.todoozi.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Menyimpan sesi login (user id, username, token API) di SharedPreferences. */
public class SessionManager {

    private static final String PREF_NAME = "UserSession";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_AVATAR = "avatar_url";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(int userId, String username, String token, String avatarUrl) {
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_TOKEN, token == null ? "" : token)
                .putString(KEY_AVATAR, avatarUrl == null ? "" : avatarUrl)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false) && getUserId() > 0;
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, 0);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public String getAvatarUrl() {
        return prefs.getString(KEY_AVATAR, "");
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
