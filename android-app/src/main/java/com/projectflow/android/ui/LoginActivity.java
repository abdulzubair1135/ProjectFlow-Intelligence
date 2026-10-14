package com.projectflow.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
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
    private Button btnSignIn, btnQuickAdmin, btnQuickManager, btnQuickMember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.emailEditText);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnQuickAdmin = findViewById(R.id.btnQuickAdmin);
        btnQuickManager = findViewById(R.id.btnQuickManager);
        btnQuickMember = findViewById(R.id.btnQuickMember);

        btnSignIn.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            if (email.isEmpty()) email = "admin@projectflow.io";
            performLogin(email);
        });

        btnQuickAdmin.setOnClickListener(v -> performLogin("admin@projectflow.io"));
        btnQuickManager.setOnClickListener(v -> performLogin("manager@projectflow.io"));
        btnQuickMember.setOnClickListener(v -> performLogin("member@projectflow.io"));
    }

    private void performLogin(String email) {
        ApiService api = ApiClient.getService(this);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        api.devLogin(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveSessionAndNavigate(response.body().token, response.body().user.email);
                } else {
                    // Fallback to offline dev login if server not yet started
                    fallbackLocalLogin(email);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Offline fallback login so the app is always functional
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
                .apply();

        Toast.makeText(this, "Signed in as " + email, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
