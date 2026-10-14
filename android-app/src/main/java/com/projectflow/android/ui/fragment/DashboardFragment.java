package com.projectflow.android.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.projectflow.android.R;
import com.projectflow.android.data.local.AppDatabase;
import com.projectflow.android.data.local.ProjectEntity;
import com.projectflow.android.data.local.TaskEntity;
import com.projectflow.android.data.repository.ProjectFlowRepository;
import com.projectflow.android.ui.CreateTaskActivity;
import com.projectflow.android.ui.MainActivity;
import com.projectflow.android.ui.adapter.ProjectAdapter;
import com.projectflow.android.ui.view.CircularProgressView;

import java.util.Calendar;
import java.util.List;

public class DashboardFragment extends Fragment {

    private TextView tvGreeting, tvUserName, tvUserRole;
    private TextView tvTotalProjects, tvTotalTasks, tvCompletedTasks, tvPendingTasks;
    private TextView badgeDone, badgeActive, tvProgressSubtitle, tvEmptyProjects;
    private CircularProgressView progressView;
    private RecyclerView rvRecentProjects;
    private ProjectAdapter projectAdapter;
    private SwipeRefreshLayout swipeRefresh;
    private FrameLayout btnNotificationBell;
    private TextView tvNotifBadge;
    private LinearLayout qaCreateTask, qaCalendar, qaReports, qaChat;
    private TextView btnViewAllProjects;
    private ProjectFlowRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new ProjectFlowRepository(requireContext());
        AppDatabase db = AppDatabase.getInstance(requireContext());

        // Header
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserRole = view.findViewById(R.id.tvUserRole);
        btnNotificationBell = view.findViewById(R.id.btnNotificationBell);
        tvNotifBadge = view.findViewById(R.id.tvNotifBadge);

        // Metrics
        tvTotalProjects = view.findViewById(R.id.tvTotalProjects);
        tvTotalTasks = view.findViewById(R.id.tvTotalTasks);
        tvCompletedTasks = view.findViewById(R.id.tvCompletedTasks);
        tvPendingTasks = view.findViewById(R.id.tvPendingTasks);

        // Progress
        progressView = view.findViewById(R.id.progressView);
        badgeDone = view.findViewById(R.id.badgeDone);
        badgeActive = view.findViewById(R.id.badgeActive);
        tvProgressSubtitle = view.findViewById(R.id.tvProgressSubtitle);

        // Quick Actions
        qaCreateTask = view.findViewById(R.id.qaCreateTask);
        qaCalendar = view.findViewById(R.id.qaCalendar);
        qaReports = view.findViewById(R.id.qaReports);
        qaChat = view.findViewById(R.id.qaChat);

        // Projects List
        rvRecentProjects = view.findViewById(R.id.rvRecentProjects);
        tvEmptyProjects = view.findViewById(R.id.tvEmptyProjects);
        btnViewAllProjects = view.findViewById(R.id.btnViewAllProjects);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        // Set dynamic time-based greeting
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            tvGreeting.setText("Good Morning,");
        } else if (hour < 17) {
            tvGreeting.setText("Good Afternoon,");
        } else {
            tvGreeting.setText("Good Evening,");
        }

        // Set user info from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("projectflow_prefs", Context.MODE_PRIVATE);
        String savedName = prefs.getString("user_name", "Rahul Verma");
        String savedRole = prefs.getString("user_role", "ADMINISTRATOR");
        tvUserName.setText(savedName);
        tvUserRole.setText(savedRole);

        // Setup Projects RecyclerView
        projectAdapter = new ProjectAdapter(project -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToFragment(new ProjectsFragment(), "Projects");
            }
        });
        rvRecentProjects.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentProjects.setAdapter(projectAdapter);

        // Wire Real LiveData Observers
        db.projectDao().getAllLive().observe(getViewLifecycleOwner(), projects -> {
            int projCount = (projects != null) ? projects.size() : 0;
            tvTotalProjects.setText(String.valueOf(projCount));

            if (projects != null && !projects.isEmpty()) {
                tvEmptyProjects.setVisibility(View.GONE);
                rvRecentProjects.setVisibility(View.VISIBLE);
                projectAdapter.setProjects(projects.subList(0, Math.min(projects.size(), 4)));
            } else {
                rvRecentProjects.setVisibility(View.GONE);
                tvEmptyProjects.setVisibility(View.VISIBLE);
            }
        });

        db.taskDao().getAllLive().observe(getViewLifecycleOwner(), tasks -> {
            int total = (tasks != null) ? tasks.size() : 0;
            int completed = 0;
            int pending = 0;

            if (tasks != null) {
                for (TaskEntity t : tasks) {
                    if ("DONE".equalsIgnoreCase(t.statusCategory)) {
                        completed++;
                    } else {
                        pending++;
                    }
                }
            }

            tvTotalTasks.setText(String.valueOf(total));
            tvCompletedTasks.setText(String.valueOf(completed));
            tvPendingTasks.setText(String.valueOf(pending));

            badgeDone.setText(completed + " Done");
            badgeActive.setText(pending + " Active");

            int percent = total > 0 ? Math.round((completed * 100f) / total) : 0;
            progressView.setProgress(percent);
            tvProgressSubtitle.setText(percent + "% tasks completed across active milestones");
        });

        // Quick Actions listeners
        qaCreateTask.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CreateTaskActivity.class));
        });

        qaCalendar.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToFragment(new CalendarFragment(), "Calendar");
            }
        });

        qaReports.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToFragment(new ReportsFragment(), "Reports");
            }
        });

        qaChat.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToFragment(new ChatFragment(), "Team Discussions");
            }
        });

        btnViewAllProjects.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToFragment(new ProjectsFragment(), "Projects");
            }
        });

        btnNotificationBell.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToFragment(new NotificationsFragment(), "Notifications");
            }
        });

        // Swipe Refresh
        swipeRefresh.setColorSchemeResources(R.color.primary_earth, R.color.muted_clay);
        swipeRefresh.setOnRefreshListener(() -> {
            repository.refreshWorkspaces();
            repository.refreshProjects();
            swipeRefresh.setRefreshing(false);
        });
    }
}
