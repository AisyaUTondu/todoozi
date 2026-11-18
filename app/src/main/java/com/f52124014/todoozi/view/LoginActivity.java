package com.f52124014.todoozi.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.f52124014.todoozi.R;
import com.f52124014.todoozi.databinding.ActivityLoginBinding;
import com.f52124014.todoozi.util.Event;
import com.f52124014.todoozi.viewmodel.AuthResult;
import com.f52124014.todoozi.viewmodel.AuthViewModel;

/**
 * Halaman login. Mengimplementasikan interface View.OnClickListener (Modul 1)
 * dan memakai AuthViewModel untuk proses autentikasi (Modul 4).
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Auto-login: jika sesi masih tersimpan, langsung ke halaman utama
        if (authViewModel.isLoggedIn()) {
            openMain(authViewModel.getUsername());
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBarInsets(binding.rootLogin);

        binding.btnLogin.setOnClickListener(this);
        binding.tvToRegister.setOnClickListener(this);

        authViewModel.getLoading().observe(this, isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            binding.progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!loading);
        });

        authViewModel.getResult().observe(this, this::handleResult);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnLogin) {
            attemptLogin();
        } else if (id == R.id.tvToRegister) {
            startActivity(new Intent(this, RegisterActivity.class));
        }
    }

    private void attemptLogin() {
        String username = String.valueOf(binding.etUsername.getText()).trim();
        String password = String.valueOf(binding.etPassword.getText());

        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);

        boolean valid = true;
        if (username.isEmpty()) {
            binding.tilUsername.setError(getString(R.string.err_required));
            valid = false;
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.err_required));
            valid = false;
        }
        if (valid) authViewModel.login(username, password);
    }

    private void handleResult(Event<AuthResult> event) {
        AuthResult result = event.getContentIfNotHandled();
        if (result == null || result.type != AuthResult.TYPE_LOGIN) return;

        if (result.success) {
            showToast(getString(R.string.msg_login_success, result.username));
            openMain(result.username);
        } else {
            showToast(result.message);
        }
    }

    private void openMain(String username) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_USERNAME, username);   // data antar Activity lewat Intent
        startActivity(intent);
        finish();
    }
}
