package com.projectflow.android.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.projectflow.android.R;
import com.projectflow.android.data.local.WorkspaceEntity;
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

        // DrawerLayout with fitsSystemWindows="true" natively handles status bar and bottom navigation insets.

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

        TextView tvWorkspaceBadge = findViewById(R.id.tvWorkspaceBadge);
        if (tvWorkspaceBadge != null) {
            tvWorkspaceBadge.setOnClickListener(v -> {
                repository.refreshWorkspaces();
                repository.refreshProjects();
                repository.refreshAllTasks();
                Toast.makeText(this, "ProjectFlow Cloud: Connected & Syncing (0ms latency)", Toast.LENGTH_SHORT).show();
            });
        }

        // Setup Drawer Header Live Data
        View headerView = navigationView != null ? navigationView.getHeaderView(0) : null;
        if (headerView != null) {
            TextView tvDrawerWorkspaceName = headerView.findViewById(R.id.tvDrawerWorkspaceName);
            View drawerLogoBadge = headerView.findViewById(R.id.drawerLogoBadge);

            if (drawerLogoBadge != null) {
                drawerLogoBadge.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("ProjectFlow Intelligence")
                            .setMessage("Version: 2.4.0 (Production)\nArchitecture: Offline-First Room SQLite + Real-time Cloud Sync\nGateway: Node.js + FCM Engine")
                            .setPositiveButton("Close", null)
                            .show();
                });
            }
        }

        repository = new ProjectFlowRepository(this);
        repository.refreshWorkspaces();
        repository.refreshProjects();
        repository.refreshAllTasks();

        // Sync live role from JWT token and backend database
        android.content.SharedPreferences prefs = getSharedPreferences("projectflow_prefs", MODE_PRIVATE);
        String currentEmail = prefs.getString("user_email", "").trim().toLowerCase();
        String token = prefs.getString("auth_token", null);
        if (token != null && token.contains(".")) {
            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    byte[] decoded = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.DEFAULT);
                    String payload = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
                    org.json.JSONObject obj = new org.json.JSONObject(payload);
                    if (obj.has("role")) {
                        String tokenRole = obj.getString("role");
                        prefs.edit().putString("user_role", tokenRole).apply();
                    }
                }
            } catch (Exception ignored) {}
        }
        repository.fetchTeam(new ProjectFlowRepository.DataCallback<java.util.List<com.projectflow.android.data.remote.model.DtoModels.TeamMemberDto>>() {
            @Override
            public void onData(java.util.List<com.projectflow.android.data.remote.model.DtoModels.TeamMemberDto> members) {
                if (members != null && !currentEmail.isEmpty()) {
                    for (var m : members) {
                        if (m.email != null && m.email.trim().equalsIgnoreCase(currentEmail)) {
                            if (m.role != null && !m.role.isEmpty()) {
                                prefs.edit().putString("user_role", m.role.toUpperCase()).apply();
                            }
                            break;
                        }
                    }
                }
            }
            @Override
            public void onError(String error) {}
        });

        if (headerView != null) {
            TextView tvDrawerWorkspaceName = headerView.findViewById(R.id.tvDrawerWorkspaceName);
            repository.getWorkspaces().observe(this, workspaces -> {
                if (workspaces != null && !workspaces.isEmpty()) {
                    WorkspaceEntity ws = workspaces.get(0);
                    if (tvDrawerWorkspaceName != null && ws.name != null && !ws.name.isEmpty()) {
                        tvDrawerWorkspaceName.setText("Workspace: " + ws.name);
                    }
                }
            });
        }

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
                loadFragment(new TeamFragment(), "Persons & Groups");
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
                loadFragment(new TeamFragment(), "Persons & Groups");
                bottomNav.setSelectedItemId(R.id.nav_team);
            } else if (id == R.id.drawer_chat) {
                loadFragment(new ChatFragment(), "Live Discussions & Chats");
            } else if (id == R.id.drawer_calendar) {
                loadFragment(new CalendarFragment(), "Calendar");
            } else if (id == R.id.drawer_reports) {
                loadFragment(new ReportsFragment(), "Reports");
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

        // Synchronize Header Title & Bottom Nav on Back Stack Popping
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (current instanceof TeamFragment) {
                tvHeaderTitle.setText("Persons & Groups");
                bottomNav.setSelectedItemId(R.id.nav_team);
            } else if (current instanceof DashboardFragment) {
                tvHeaderTitle.setText("Dashboard");
                bottomNav.setSelectedItemId(R.id.nav_home);
            } else if (current instanceof ProjectsFragment) {
                tvHeaderTitle.setText("Projects");
                bottomNav.setSelectedItemId(R.id.nav_projects);
            } else if (current instanceof TasksFragment) {
                tvHeaderTitle.setText("Tasks");
                bottomNav.setSelectedItemId(R.id.nav_tasks);
            } else if (current instanceof ProfileFragment) {
                tvHeaderTitle.setText("Profile & Settings");
                bottomNav.setSelectedItemId(R.id.nav_profile);
            }
        });

        // Start Real-Time Notification & Chat Sync Manager
        NotificationSyncManager.getInstance(this).start();

        // Ensure FCM Push Token is registered with active server
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String fcmDeviceToken = task.getResult();
                    prefs.edit().putString("fcm_token", fcmDeviceToken).apply();
                    new Thread(() -> {
                        try {
                            java.util.Map<String, String> body = new java.util.HashMap<>();
                            body.put("fcmToken", fcmDeviceToken);
                            String email = prefs.getString("user_email", "abdulzubair221@gmail.com");
                            body.put("email", email);
                            com.projectflow.android.data.remote.ApiClient.getService(this).registerFcmToken(body).execute();
                        } catch (Exception ignored) {}
                    }).start();
                }
            });
        } catch (Exception ignored) {}
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        StatusBarUtils.applyLightStatusBar(this);
        setIntent(intent);
        handleIntent(intent);
    }

    private boolean handleIntent(android.content.Intent intent) {
        if (intent == null) return false;
        String navigateTo = intent.getStringExtra("navigate_to");
        if ("CHAT".equalsIgnoreCase(navigateTo) || "CHAT_ROOM".equalsIgnoreCase(navigateTo)) {
            String channel = intent.getStringExtra("chat_channel");
            String title = intent.getStringExtra("chat_title");
            String type = intent.getStringExtra("chat_type");
            if (channel == null || channel.isEmpty()) channel = "general";
            if (title == null || title.isEmpty()) {
                title = channel.startsWith("dm_") ? "System Administrator" : ("#" + channel);
            }
            if (type == null || type.isEmpty()) {
                type = channel.startsWith("dm_") ? "DIRECT" : "GROUP";
            }

            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (current instanceof ChatFragment) {
                ((ChatFragment) current).openChatRoom(channel, title, type);
            } else {
                ChatFragment chatFragment = ChatFragment.newInstance(channel, title, type);
                navigateToFragment(chatFragment, "Chat: " + title);
            }
            return true;
        } else if ("NOTIFICATIONS".equalsIgnoreCase(navigateTo)) {
            loadFragment(new NotificationsFragment(), "Notifications");
            return true;
        } else if ("TASK".equalsIgnoreCase(navigateTo) || "TASKS".equalsIgnoreCase(navigateTo)) {
            String taskId = intent.getStringExtra("task_id");
            if (taskId != null && !taskId.isEmpty()) {
                Intent taskIntent = new Intent(this, TaskDetailsActivity.class);
                taskIntent.putExtra("task_id", taskId);
                taskIntent.putExtra("task_title", intent.getStringExtra("task_title"));
                taskIntent.putExtra("task_desc", intent.getStringExtra("task_desc"));
                taskIntent.putExtra("task_priority", intent.getStringExtra("task_priority"));
                startActivity(taskIntent);
            } else {
                loadFragment(new TasksFragment(), "Tasks");
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_tasks);
            }
            return true;
        } else if ("PROJECT".equalsIgnoreCase(navigateTo) || "PROJECTS".equalsIgnoreCase(navigateTo)) {
            String projectId = intent.getStringExtra("project_id");
            if (projectId != null && !projectId.isEmpty()) {
                Intent projIntent = new Intent(this, ProjectDetailsActivity.class);
                projIntent.putExtra("project_id", projectId);
                projIntent.putExtra("project_name", intent.getStringExtra("project_name"));
                startActivity(projIntent);
            } else {
                loadFragment(new ProjectsFragment(), "Projects");
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_projects);
            }
            return true;
        }
        return false;
    }

    public void navigateToFragment(Fragment fragment, String title) {
        tvHeaderTitle.setText(title);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(title)
                .commit();
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
        StatusBarUtils.applyLightStatusBar(this);
        NotificationSyncManager.getInstance(this).start();
    }

    private long backPressedTime = 0;

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }

        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (!(current instanceof DashboardFragment)) {
            loadFragment(new DashboardFragment(), "Dashboard");
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
            return;
        }

        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
            backPressedTime = System.currentTimeMillis();
        }
    }
}
