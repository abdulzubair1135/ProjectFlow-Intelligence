package com.projectflow.android;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ProjectFlowApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Enforce light mode as the default on app startup
        SharedPreferences prefs = getSharedPreferences("projectflow_prefs", Context.MODE_PRIVATE);
        String theme = prefs.getString("app_theme", "light");

        if ("dark".equalsIgnoreCase(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if ("system".equalsIgnoreCase(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            // Default to Light Mode!
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
