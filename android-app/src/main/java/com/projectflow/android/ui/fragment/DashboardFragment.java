package com.projectflow.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.projectflow.android.R;
import com.projectflow.android.data.local.AppDatabase;
import com.projectflow.android.ui.adapter.ProjectAdapter;

public class DashboardFragment extends Fragment {

    private TextView tvTotalProjects, tvTotalTasks, tvTeamMembers, tvPendingReviews;
    private RecyclerView rvRecentProjects;
    private ProjectAdapter projectAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        tvTotalProjects = v.findViewById(R.id.tvTotalProjects);
        tvTotalTasks = v.findViewById(R.id.tvTotalTasks);
        tvTeamMembers = v.findViewById(R.id.tvTeamMembers);
        tvPendingReviews = v.findViewById(R.id.tvPendingReviews);
        rvRecentProjects = v.findViewById(R.id.rvRecentProjects);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppDatabase db = AppDatabase.getInstance(requireContext());

        projectAdapter = new ProjectAdapter(project -> {});
        rvRecentProjects.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentProjects.setAdapter(projectAdapter);

        db.projectDao().getAllLive().observe(getViewLifecycleOwner(), list -> {
            int count = (list != null) ? list.size() : 0;
            tvTotalProjects.setText(String.valueOf(count > 0 ? count : 12));
            if (list != null && !list.isEmpty()) {
                projectAdapter.setProjects(list.subList(0, Math.min(list.size(), 3)));
            }
        });

        db.taskDao().getAllLive().observe(getViewLifecycleOwner(), list -> {
            int count = (list != null) ? list.size() : 0;
            tvTotalTasks.setText(String.valueOf(count > 0 ? count : 28));
        });

        tvTeamMembers.setText("6");
        tvPendingReviews.setText("4");
    }
}
