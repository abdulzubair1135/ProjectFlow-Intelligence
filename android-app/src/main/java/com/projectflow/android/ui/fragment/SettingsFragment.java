package com.projectflow.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.projectflow.android.R;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        TextView btnLight = v.findViewById(R.id.btnThemeLight);
        TextView btnDark = v.findViewById(R.id.btnThemeDark);
        TextView btnSystem = v.findViewById(R.id.btnThemeSystem);

        btnLight.setOnClickListener(view -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(requireContext(), "Light Mode activated", Toast.LENGTH_SHORT).show();
        });

        btnDark.setOnClickListener(view -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Toast.makeText(requireContext(), "Dark Mode activated (Screen 16)", Toast.LENGTH_SHORT).show();
        });

        btnSystem.setOnClickListener(view -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            Toast.makeText(requireContext(), "System Mode activated", Toast.LENGTH_SHORT).show();
        });

        return v;
    }
}
