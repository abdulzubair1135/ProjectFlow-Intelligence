package com.projectflow.android.ui.fragment;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.projectflow.android.R;
import com.projectflow.android.data.remote.model.DtoModels.ReleaseDto;
import com.projectflow.android.distribution.ApkDistributionManager;

public class SettingsFragment extends Fragment {

    private ReleaseDto latestRelease = null;
    private TextView tvInstalledVersion, tvReleaseVersion, tvApkSize, tvReleaseStatus, tvShareProgress;
    private ProgressBar pbShareLoading;
    private Button btnShareApk;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        // Theme switching (Light, Dark, System)
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

        // Application & APK Distribution
        tvInstalledVersion = v.findViewById(R.id.tvInstalledVersion);
        tvReleaseVersion = v.findViewById(R.id.tvReleaseVersion);
        tvApkSize = v.findViewById(R.id.tvApkSize);
        tvReleaseStatus = v.findViewById(R.id.tvReleaseStatus);
        tvShareProgress = v.findViewById(R.id.tvShareProgress);
        pbShareLoading = v.findViewById(R.id.pbShareLoading);
        btnShareApk = v.findViewById(R.id.btnShareApk);
        View layoutShareApk = v.findViewById(R.id.layoutShareApk);

        // Populate installed package info
        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            tvInstalledVersion.setText("v" + pInfo.versionName + " (Code: " + pInfo.versionCode + ")");
        } catch (Exception e) {
            tvInstalledVersion.setText("v2.0.0 (Code: 200)");
        }

        // Fetch remote official release metadata
        ApkDistributionManager.fetchLatestRelease(requireContext(), new ApkDistributionManager.ReleaseCallback() {
            @Override
            public void onSuccess(ReleaseDto release) {
                latestRelease = release;
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        tvReleaseVersion.setText("v" + release.versionName + " • " + release.releaseChannel);
                        if (release.fileSizeBytes > 0) {
                            double mb = release.fileSizeBytes / (1024.0 * 1024.0);
                            tvApkSize.setText(String.format("%.1f MB", mb));
                        }
                        tvReleaseStatus.setText("Official Signed Release (" + release.status + ")");
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        tvReleaseStatus.setText("Standalone Offline Package Available");
                    });
                }
            }
        });

        View.OnClickListener shareAction = view -> triggerApkShare();
        btnShareApk.setOnClickListener(shareAction);
        layoutShareApk.setOnClickListener(shareAction);

        return v;
    }

    private void triggerApkShare() {
        pbShareLoading.setVisibility(View.VISIBLE);
        tvShareProgress.setVisibility(View.VISIBLE);
        tvShareProgress.setText("Initializing distribution pipeline...");
        btnShareApk.setEnabled(false);

        ApkDistributionManager.prepareAndShareApk(requireActivity(), latestRelease, new ApkDistributionManager.ShareCallback() {
            @Override
            public void onProgress(String status) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> tvShareProgress.setText(status));
                }
            }

            @Override
            public void onReady() {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        pbShareLoading.setVisibility(View.GONE);
                        tvShareProgress.setVisibility(View.GONE);
                        btnShareApk.setEnabled(true);
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        pbShareLoading.setVisibility(View.GONE);
                        tvShareProgress.setVisibility(View.GONE);
                        btnShareApk.setEnabled(true);
                        Toast.makeText(requireContext(), "Share Failed: " + message, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
}
