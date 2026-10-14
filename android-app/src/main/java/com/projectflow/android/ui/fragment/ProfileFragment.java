package com.projectflow.android.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.projectflow.android.R;
import com.projectflow.android.ui.LoginActivity;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView tvName = v.findViewById(R.id.tvProfileName);
        Button btnLogout = v.findViewById(R.id.btnLogout);

        SharedPreferences prefs = requireContext().getSharedPreferences("projectflow_prefs", Context.MODE_PRIVATE);
        String email = prefs.getString("user_email", "Rahul Verma");
        if (email.contains("@")) {
            tvName.setText(email.split("@")[0]);
        }

        btnLogout.setOnClickListener(view -> {
            prefs.edit().remove("auth_token").remove("user_email").apply();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return v;
    }
}
