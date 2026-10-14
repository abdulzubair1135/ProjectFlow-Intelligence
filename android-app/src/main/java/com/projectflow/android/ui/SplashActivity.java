package com.projectflow.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.projectflow.android.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("projectflow_prefs", Context.MODE_PRIVATE);
            String token = prefs.getString("auth_token", null);
            boolean onboardingCompleted = prefs.getBoolean("onboarding_completed", false);

            Intent intent;
            if (token != null && !token.isEmpty()) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else if (!onboardingCompleted) {
                intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, 1200);
    }
}
