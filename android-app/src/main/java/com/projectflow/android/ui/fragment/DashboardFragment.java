package com.projectflow.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.projectflow.android.R;
import com.projectflow.android.data.local.AppDatabase;

public class DashboardFragment extends Fragment {

    private TextView tvWorkspaces, tvProjects, tvTasks, tvRisks;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        tvWorkspaces = v.findViewById(R.id.tvTotalWorkspaces);
        tvProjects = v.findViewById(R.id.tvTotalProjects);
        tvTasks = v.findViewById(R.id.tvTotalTasks);
        tvRisks = v.findViewById(R.id.tvTotalRisks);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppDatabase db = AppDatabase.getInstance(requireContext());

        db.workspaceDao().getAllLive().observe(getViewLifecycleOwner(), list -> {
            tvWorkspaces.setText(String.valueOf(list != null ? list.size() : 0));
        });

        db.projectDao().getAllLive().observe(getViewLifecycleOwner(), list -> {
            tvProjects.setText(String.valueOf(list != null ? list.size() : 0));
        });

        db.taskDao().getAllLive().observe(getViewLifecycleOwner(), list -> {
            tvTasks.setText(String.valueOf(list != null ? list.size() : 0));
        });

        db.riskDao().getAllLive().observe(getViewLifecycleOwner(), list -> {
            tvRisks.setText(String.valueOf(list != null ? list.size() : 0));
        });
    }
}
