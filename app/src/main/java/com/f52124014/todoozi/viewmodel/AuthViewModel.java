package com.f52124014.todoozi.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.f52124014.todoozi.repository.AuthRepository;
import com.f52124014.todoozi.util.Event;
import com.f52124014.todoozi.util.SessionManager;

/**
 * "Auth Controller" aplikasi (Modul 4 - MVVM).
 * Menangani login, register, logout dan pengecekan sesi. Pekerjaan berat (SQLite / jaringan)
 * dijalankan di background, hasilnya dikirim ke UI lewat LiveData.
 */
public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final SessionManager session;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<AuthResult>> result = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
        session = new SessionManager(application);
    }

    public LiveData<Boolean> getLoading() { return loading; }

    public LiveData<Event<AuthResult>> getResult() { return result; }

    public boolean isLoggedIn() { return session.isLoggedIn(); }

    public String getUsername() { return session.getUsername(); }

    public void login(String username, String password) {
        if (Boolean.TRUE.equals(loading.getValue())) return;   // cegah klik ganda
        loading.setValue(true);
        repository.login(username, password,
                new AuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess(int userId, String name, String token, String avatarUrl) {
                        session.saveSession(userId, name, token, avatarUrl);
                        loading.postValue(false);
                        result.postValue(new Event<>(AuthResult.success(AuthResult.TYPE_LOGIN, name)));
                    }

                    @Override
                    public void onError(String message) {
                        loading.postValue(false);
                        result.postValue(new Event<>(AuthResult.failure(AuthResult.TYPE_LOGIN, message)));
                    }
                });
    }

    public void register(String username, String password) {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        loading.setValue(true);
        repository.register(username, password,
                new AuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess(int userId, String name, String token, String avatarUrl) {
                        // Setelah daftar, pengguna diminta masuk manual (sesi tidak disimpan).
                        loading.postValue(false);
                        result.postValue(new Event<>(AuthResult.success(AuthResult.TYPE_REGISTER, name)));
                    }

                    @Override
                    public void onError(String message) {
                        loading.postValue(false);
                        result.postValue(new Event<>(AuthResult.failure(AuthResult.TYPE_REGISTER, message)));
                    }
                });
    }

    public void logout() {
        session.clear();
    }

    @Override
    protected void onCleared() {
        repository.shutdown();
    }
}
