package com.projectflow.android.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;
import com.projectflow.android.R;
import com.projectflow.android.data.remote.model.DtoModels.UserDto;
import com.projectflow.android.data.repository.ProjectFlowRepository;
import com.projectflow.android.distribution.ApkDistributionManager;
import com.projectflow.android.ui.LoginActivity;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail, tvProfileRole, tvProfileInitial;
    private View btnEditProfile;
    private Button btnLogout;
    private TextView btnThemeLight, btnThemeDark, btnThemeSystem;
    private ProjectFlowRepository repository;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new ProjectFlowRepository(requireContext());
        prefs = requireContext().getSharedPreferences("projectflow_prefs", Context.MODE_PRIVATE);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileRole = view.findViewById(R.id.tvProfileRole);
        tvProfileInitial = view.findViewById(R.id.tvProfileInitial);

        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnLogout = view.findViewById(R.id.btnLogout);

        btnThemeLight = view.findViewById(R.id.btnThemeLight);
        btnThemeDark = view.findViewById(R.id.btnThemeDark);
        btnThemeSystem = view.findViewById(R.id.btnThemeSystem);

        loadUserData();

        btnEditProfile.setOnClickListener(v -> showEditNameDialog());

        // Theme switchers
        btnThemeLight.setOnClickListener(v -> setThemeMode(AppCompatDelegate.MODE_NIGHT_NO, "light"));
        btnThemeDark.setOnClickListener(v -> setThemeMode(AppCompatDelegate.MODE_NIGHT_YES, "dark"));
        btnThemeSystem.setOnClickListener(v -> setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, "system"));

        highlightActiveTheme();

        // Application Settings inside Profile
        View layoutProfileShareApk = view.findViewById(R.id.layoutProfileShareApk);
        Button btnProfileShareApk = view.findViewById(R.id.btnProfileShareApk);
        Button btnProfileSyncNow = view.findViewById(R.id.btnProfileSyncNow);
        SwitchCompat switchProfileNotifications = view.findViewById(R.id.switchProfileNotifications);

        View.OnClickListener openShare = v -> {
            Toast.makeText(requireContext(), "Preparing ProjectFlow APK for native sharing...", Toast.LENGTH_SHORT).show();
            ApkDistributionManager.prepareAndShareApk(requireActivity(), null, new ApkDistributionManager.ShareCallback() {
                @Override public void onProgress(String status) {}
                @Override public void onReady() {}
                @Override public void onError(String message) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Share failed: " + message, Toast.LENGTH_SHORT).show());
                    }
                }
            });
        };
        if (layoutProfileShareApk != null) layoutProfileShareApk.setOnClickListener(openShare);
        if (btnProfileShareApk != null) btnProfileShareApk.setOnClickListener(openShare);

        if (btnProfileSyncNow != null) {
            btnProfileSyncNow.setOnClickListener(v -> {
                repository.refreshWorkspaces();
                Toast.makeText(requireContext(), "Offline database synchronized with backend", Toast.LENGTH_SHORT).show();
            });
        }

        if (switchProfileNotifications != null) {
            boolean enabled = prefs.getBoolean("alerts_enabled", true);
            switchProfileNotifications.setChecked(enabled);
            switchProfileNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("alerts_enabled", isChecked).apply();
                Toast.makeText(requireContext(), isChecked ? "Deadline & Risk alerts enabled" : "Alerts muted", Toast.LENGTH_SHORT).show();
            });
        }

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Sign Out")
                    .setMessage("Are you sure you want to log out of ProjectFlow Intelligence on this device?")
                    .setPositiveButton("Sign Out", (dialog, which) -> {
                        prefs.edit()
                                .remove("auth_token")
                                .remove("user_email")
                                .remove("user_name")
                                .remove("user_display_name")
                                .remove("user_role")
                                .apply();
                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadUserData() {
        String email = prefs.getString("user_email", "admin@projectflow.io");
        String defaultName = email.contains("@") ? email.split("@")[0] : "ProjectFlow User";
        String name = prefs.getString("user_name", prefs.getString("user_display_name", defaultName));
        String role = prefs.getString("user_role", "ADMINISTRATOR");

        tvProfileName.setText(name);
        tvProfileEmail.setText(email);
        tvProfileRole.setText(role);

        if (!name.isEmpty()) {
            tvProfileInitial.setText(name.substring(0, 1).toUpperCase());
        }
    }

    private void showEditNameDialog() {
        EditText input = new EditText(requireContext());
        input.setText(tvProfileName.getText());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(requireContext())
                .setTitle("Update Display Name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        tvProfileName.setText(newName);
                        tvProfileInitial.setText(newName.substring(0, 1).toUpperCase());
                        prefs.edit().putString("user_name", newName).apply();

                        repository.updateProfile(newName, new ProjectFlowRepository.DataCallback<UserDto>() {
                            @Override
                            public void onData(UserDto data) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                                    );
                                }
                            }
                            @Override
                            public void onError(String message) {}
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setThemeMode(int mode, String name) {
        prefs.edit().putString("app_theme", name).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
        highlightActiveTheme();
    }

    private void highlightActiveTheme() {
        String activeTheme = prefs.getString("app_theme", "light");
        btnThemeLight.setBackgroundResource("light".equals(activeTheme) ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        btnThemeLight.setTextColor(ContextCompat.getColor(requireContext(), "light".equals(activeTheme) ? R.color.white : R.color.text_main));

        btnThemeDark.setBackgroundResource("dark".equals(activeTheme) ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        btnThemeDark.setTextColor(ContextCompat.getColor(requireContext(), "dark".equals(activeTheme) ? R.color.white : R.color.text_main));

        btnThemeSystem.setBackgroundResource("system".equals(activeTheme) ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        btnThemeSystem.setTextColor(ContextCompat.getColor(requireContext(), "system".equals(activeTheme) ? R.color.white : R.color.text_main));
    }
}
