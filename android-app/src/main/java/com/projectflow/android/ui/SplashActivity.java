package com.projectflow.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.projectflow.android.R;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private VideoView videoViewSplash;
    private View rootSplashLayout;
    private LinearLayout llFallbackLogo;
    private TextView btnSkip;
    private boolean hasNavigated = false;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Elegant light status bar matching limestone mineral theme #F4F2E9
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#F4F2E9"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        setContentView(R.layout.activity_splash);

        videoViewSplash = findViewById(R.id.videoViewSplash);
        rootSplashLayout = findViewById(R.id.rootSplashLayout);
        llFallbackLogo = findViewById(R.id.llFallbackLogo);
        btnSkip = findViewById(R.id.btnSkip);

        // Tap to skip
        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> navigateNext());
        }
        if (rootSplashLayout != null) {
            rootSplashLayout.setOnClickListener(v -> navigateNext());
        }

        // Safety fallback timeout (11.5 seconds) in case of any media driver freeze
        timeoutRunnable = this::navigateNext;
        timeoutHandler.postDelayed(timeoutRunnable, 11500);

        playStartupVideo();
    }

    private void playStartupVideo() {
        try {
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.splash_video);
            videoViewSplash.setVideoURI(videoUri);

            videoViewSplash.setOnPreparedListener(mp -> {
                Log.d(TAG, "Startup video prepared, playing...");
                mp.setLooping(false);
                videoViewSplash.start();
            });

            videoViewSplash.setOnCompletionListener(mp -> {
                Log.d(TAG, "Startup video playback completed.");
                navigateNext();
            });

            videoViewSplash.setOnErrorListener((mp, what, extra) -> {
                Log.w(TAG, "VideoView playback error: " + what + ", " + extra + " -> using fallback");
                showFallbackAndProceed();
                return true;
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize VideoView", e);
            showFallbackAndProceed();
        }
    }

    private void showFallbackAndProceed() {
        if (llFallbackLogo != null) {
            llFallbackLogo.setVisibility(View.VISIBLE);
        }
        if (videoViewSplash != null) {
            videoViewSplash.setVisibility(View.GONE);
        }
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, 1200);
    }

    private synchronized void navigateNext() {
        if (hasNavigated) {
            return;
        }
        hasNavigated = true;
        timeoutHandler.removeCallbacksAndMessages(null);

        try {
            if (videoViewSplash != null && videoViewSplash.isPlaying()) {
                videoViewSplash.stopPlayback();
            }
        } catch (Exception ignored) {}

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
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeoutHandler.removeCallbacksAndMessages(null);
        try {
            if (videoViewSplash != null) {
                videoViewSplash.stopPlayback();
            }
        } catch (Exception ignored) {}
    }
}
