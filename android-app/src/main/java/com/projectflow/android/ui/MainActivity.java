package com.projectflow.android.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.projectflow.android.R;
import com.projectflow.android.data.repository.ProjectFlowRepository;
import com.projectflow.android.ui.fragment.*;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private TextView tvHeaderTitle, tvWorkspaceBadge;
    private ProjectFlowRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvWorkspaceBadge = findViewById(R.id.tvWorkspaceBadge);

        repository = new ProjectFlowRepository(this);

        // Initial background fetch
        repository.refreshWorkspaces();
        repository.refreshProjects();

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment;
            if (id == R.id.nav_projects) {
                tvHeaderTitle.setText("Projects");
                fragment = new ProjectsFragment();
            } else if (id == R.id.nav_kanban) {
                tvHeaderTitle.setText("Kanban");
                fragment = new KanbanFragment();
            } else if (id == R.id.nav_risk_radar) {
                tvHeaderTitle.setText("Risk Radar");
                fragment = new RiskRadarFragment();
            } else if (id == R.id.nav_sync) {
                tvHeaderTitle.setText("Offline Sync");
                fragment = new SyncFragment();
            } else {
                tvHeaderTitle.setText("Dashboard");
                fragment = new DashboardFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }
    }
}
