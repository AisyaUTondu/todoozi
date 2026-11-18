package com.f52124014.todoozi.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.f52124014.todoozi.R;
import com.f52124014.todoozi.model.DatabaseHelper;
import com.f52124014.todoozi.network.ApiConfig;
import com.f52124014.todoozi.network.ApiHelper;
import com.f52124014.todoozi.network.AuthRequest;
import com.f52124014.todoozi.network.AuthResponse;
import com.f52124014.todoozi.network.UserDto;
import com.f52124014.todoozi.util.AppConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Sumber data autentikasi.
 * USE_API = false -> memakai SQLite (di background thread).
 * USE_API = true  -> memanggil REST API lewat Retrofit (enqueue / asynchronous).
 */
public class AuthRepository {

    public interface AuthCallback {
        void onSuccess(int userId, String username, String token, String avatarUrl);

        void onError(String message);
    }

    private final Context context;
    private final DatabaseHelper db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = new DatabaseHelper(this.context);
    }

    public void login(final String username, final String password, final AuthCallback callback) {
        if (AppConfig.USE_API) {
            ApiConfig.getService(context).login(new AuthRequest(username, password))
                    .enqueue(remoteCallback(callback));
        } else {
            executor.execute(() -> {
                int userId = db.checkUser(username, password);
                if (userId > 0) {
                    callback.onSuccess(userId, username, "", "");
                } else {
                    callback.onError(context.getString(R.string.msg_login_failed));
                }
            });
        }
    }

    public void register(final String username, final String password, final AuthCallback callback) {
        if (AppConfig.USE_API) {
            ApiConfig.getService(context).register(new AuthRequest(username, password))
                    .enqueue(remoteCallback(callback));
        } else {
            executor.execute(() -> {
                long id = db.addUser(username, password);
                if (id != -1) {
                    callback.onSuccess((int) id, username, "", "");
                } else {
                    callback.onError(context.getString(R.string.msg_username_taken));
                }
            });
        }
    }

    private retrofit2.Callback<AuthResponse> remoteCallback(final AuthCallback callback) {
        return new retrofit2.Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                AuthResponse body = response.body();
                if (response.isSuccessful() && body != null && body.getUser() != null) {
                    UserDto user = body.getUser();
                    callback.onSuccess(user.getId(), user.getUsername(), body.getToken(), user.getAvatarUrl());
                } else {
                    callback.onError(ApiHelper.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                callback.onError(context.getString(R.string.err_network));
            }
        };
    }

    public void shutdown() {
        executor.shutdown();
    }
}
