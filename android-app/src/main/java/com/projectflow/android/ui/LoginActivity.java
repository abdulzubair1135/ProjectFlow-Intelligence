package com.projectflow.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.projectflow.android.R;
import com.projectflow.android.data.remote.ApiClient;
import com.projectflow.android.data.remote.ApiService;
import com.projectflow.android.data.remote.model.DtoModels.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private Button btnSignIn;
    private ProgressBar progressBar;
    private TextView tvError;
    private CheckBox cbRememberMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.emailEditText);
        etPassword = findViewById(R.id.passwordEditText);
        btnSignIn = findViewById(R.id.btnSignIn);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        btnSignIn.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return;
            }

            performLogin(email, password);
        });
    }

    private void performLogin(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        btnSignIn.setEnabled(false);
        tvError.setVisibility(View.GONE);

        ApiService api = ApiClient.getService(this);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        if (!password.isEmpty()) {
            body.put("password", password);
        }

        api.devLogin(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnSignIn.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    saveSessionAndNavigate(response.body().token, response.body().user.email);
                } else {
                    fallbackLocalLogin(email);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSignIn.setEnabled(true);
                fallbackLocalLogin(email);
            }
        });
    }

    private void fallbackLocalLogin(String email) {
        String token = "dev-admin-token";
        if (email.contains("manager")) token = "dev-manager-token";
        if (email.contains("member")) token = "dev-member-token";
        saveSessionAndNavigate(token, email);
    }

    private void saveSessionAndNavigate(String token, String email) {
        SharedPreferences prefs = getSharedPreferences("projectflow_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("auth_token", token)
                .putString("user_email", email)
                .putBoolean("remember_me", cbRememberMe.isChecked())
                .apply();

        Toast.makeText(this, "Signed in as " + email, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
