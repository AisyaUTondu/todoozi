package com.f52124014.todoozi.view;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.f52124014.todoozi.R;
import com.f52124014.todoozi.databinding.ActivityRegisterBinding;
import com.f52124014.todoozi.util.Event;
import com.f52124014.todoozi.viewmodel.AuthResult;
import com.f52124014.todoozi.viewmodel.AuthViewModel;

public class RegisterActivity extends BaseActivity implements View.OnClickListener {

    private ActivityRegisterBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBarInsets(binding.rootRegister);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnRegister.setOnClickListener(this);
        binding.tvToLogin.setOnClickListener(this);

        authViewModel.getLoading().observe(this, isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            binding.progressRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnRegister.setEnabled(!loading);
        });

        authViewModel.getResult().observe(this, this::handleResult);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnRegister) {
            attemptRegister();
        } else if (id == R.id.tvToLogin) {
            finish();
        }
    }

    private void attemptRegister() {
        String username = String.valueOf(binding.etUsername.getText()).trim();
        String password = String.valueOf(binding.etPassword.getText());
        String confirm = String.valueOf(binding.etConfirm.getText());

        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirm.setError(null);

        boolean valid = true;
        if (username.length() < 3) {
            binding.tilUsername.setError(getString(R.string.err_username_short));
            valid = false;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.err_password_short));
            valid = false;
        }
        if (!password.equals(confirm)) {
            binding.tilConfirm.setError(getString(R.string.err_password_mismatch));
            valid = false;
        }
        if (valid) authViewModel.register(username, password);
    }

    private void handleResult(Event<AuthResult> event) {
        AuthResult result = event.getContentIfNotHandled();
        if (result == null || result.type != AuthResult.TYPE_REGISTER) return;

        if (result.success) {
            showToast(R.string.msg_register_success);
            finish();   // kembali ke halaman login
        } else {
            showToast(result.message);
        }
    }
}
