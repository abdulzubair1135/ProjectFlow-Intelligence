package com.projectflow.android.ui;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.projectflow.android.R;
import com.projectflow.android.data.repository.ProjectFlowRepository;
import com.projectflow.android.service.NotificationSyncManager;
import com.projectflow.android.ui.fragment.*;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNav;
    private TextView tvHeaderTitle;
    private ImageView btnMenu;
    private ProjectFlowRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtils.applyLightStatusBar(this);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);

        // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        navigationView = findViewById(R.id.navigation_view);
        bottomNav = findViewById(R.id.bottom_nav);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        btnMenu = findViewById(R.id.btnMenu);

        repository = new ProjectFlowRepository(this);
        repository.refreshWorkspaces();
        repository.refreshProjects();

        // Hamburger Menu click -> Open Drawer (Screen 15)
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        // Bottom Navigation (Screens 4, 5, 6, 10, More)
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_projects) {
                loadFragment(new ProjectsFragment(), "Projects");
            } else if (id == R.id.nav_tasks) {
                loadFragment(new TasksFragment(), "Tasks");
            } else if (id == R.id.nav_team) {
                loadFragment(new TeamFragment(), "Team Members");
            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment(), "Profile & Settings");
            } else {
                loadFragment(new DashboardFragment(), "Dashboard");
            }
            return true;
        });

        // Navigation Drawer (Screen 15: Navigation Drawer)
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.drawer_home) {
                loadFragment(new DashboardFragment(), "Dashboard");
                bottomNav.setSelectedItemId(R.id.nav_home);
            } else if (id == R.id.drawer_projects) {
                loadFragment(new ProjectsFragment(), "Projects");
                bottomNav.setSelectedItemId(R.id.nav_projects);
            } else if (id == R.id.drawer_tasks) {
                loadFragment(new TasksFragment(), "Tasks");
                bottomNav.setSelectedItemId(R.id.nav_tasks);
            } else if (id == R.id.drawer_team) {
                loadFragment(new TeamFragment(), "Team Members");
                bottomNav.setSelectedItemId(R.id.nav_team);
            } else if (id == R.id.drawer_calendar) {
                loadFragment(new CalendarFragment(), "Calendar");
            } else if (id == R.id.drawer_reports) {
                loadFragment(new ReportsFragment(), "Reports");
            } else if (id == R.id.drawer_chat) {
                loadFragment(new ChatFragment(), "Team Discussions");
            } else if (id == R.id.drawer_notifications) {
                loadFragment(new NotificationsFragment(), "Notifications");
            } else if (id == R.id.drawer_profile) {
                loadFragment(new ProfileFragment(), "Profile & Settings");
                bottomNav.setSelectedItemId(R.id.nav_profile);
            } else if (id == R.id.drawer_settings) {
                loadFragment(new ProfileFragment(), "Profile & Settings");
                bottomNav.setSelectedItemId(R.id.nav_profile);
            }

            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });

        // Initial default screen: Dashboard (Screen 4)
        if (savedInstanceState == null) {
            if (!handleIntent(getIntent())) {
                loadFragment(new DashboardFragment(), "Dashboard");
            }
        } else {
            handleIntent(getIntent());
        }

        // Start Real-Time Notification & Chat Sync Manager
        NotificationSyncManager.getInstance(this).start();
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private boolean handleIntent(android.content.Intent intent) {
        if (intent == null) return false;
        String navigateTo = intent.getStringExtra("navigate_to");
        if ("CHAT".equalsIgnoreCase(navigateTo)) {
            String channel = intent.getStringExtra("chat_channel");
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (current instanceof ChatFragment) {
                ((ChatFragment) current).switchChannel(channel);
            } else {
                ChatFragment chatFragment = ChatFragment.newInstance(channel);
                loadFragment(chatFragment, "Team Discussions");
            }
            return true;
        } else if ("NOTIFICATIONS".equalsIgnoreCase(navigateTo)) {
            loadFragment(new NotificationsFragment(), "Notifications");
            return true;
        }
        return false;
    }

    public void navigateToFragment(Fragment fragment, String title) {
        loadFragment(fragment, title);
    }

    private void loadFragment(Fragment fragment, String title) {
        tvHeaderTitle.setText(title);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationSyncManager.getInstance(this).start();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
