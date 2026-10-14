# ProjectFlow Intelligence — Comprehensive Screen Inventory

**Version:** 2.0.0  
**Design Standard:** Natural मिट्टी (Earth-Tone) Light & Dark

---

## 1. Complete Catalog of Application Screens

### Screen 1: Splash Screen
- **Class:** `SplashActivity.java` | **Layout:** `activity_splash.xml`
- **Purpose:** Brand reveal and asynchronous session validation.
- **Components:** Brand logo (`@drawable/logo`), typography title, indeterminate horizontal mineral progress bar.
- **Routing:** Directs authenticated users to `MainActivity`; first-time users to `OnboardingActivity`; unauthenticated returning users to `LoginActivity`.

### Screen 2: Onboarding Flow
- **Class:** `OnboardingActivity.java` | **Layout:** `activity_onboarding.xml`
- **Purpose:** Introduces product capabilities (Task workflows, Risk Radar, Offline synchronization).
- **Actions:** Skip, Continue, Get Started. Persists `onboarding_completed` flag in `SharedPreferences`.

### Screen 3: Authentication (Login)
- **Class:** `LoginActivity.java` | **Layout:** `activity_login.xml`
- **Purpose:** Authenticates user against backend `/api/v1/auth/dev-login`.
- **Features:** Email & password input validation, remember-me checkbox, live error messages, progress indicators, and session token persistence.

### Screen 4: Dashboard
- **Class:** `DashboardFragment.java` | **Layout:** `fragment_dashboard.xml`
- **Purpose:** Primary operational cockpit.
- **Components:**
  - Time-aware personalized greeting ("Good Morning / Afternoon / Evening").
  - Live user identity & role badge.
  - Notification bell with unread count indicator.
  - 4 Real metric KPI cards (Total Projects, Total Tasks, Completed Tasks, Active/Pending).
  - Animated `CircularProgressView` tracking work completion rate.
  - Quick Action bar: New Task, Calendar, Reports, Team Chat.
  - Recent Projects carousel with live progress bars and target deadlines.
  - Pull-to-refresh data synchronization.

### Screen 5: Projects Explorer
- **Class:** `ProjectsFragment.java` | **Layout:** `fragment_projects.xml`
- **Purpose:** Browsing, searching, and filtering organizational projects.
- **Features:** Real-time search query filtering, status chips (`All`, `Active`, `Completed`, `On Hold`), floating action button to create new projects, and tap-through to `ProjectDetailsActivity`.

### Screen 6: Project Details
- **Class:** `ProjectDetailsActivity.java` | **Layout:** `activity_project_details.xml`
- **Purpose:** In-depth project view with dedicated sub-sections.
- **Components:** Code badge, status pill, title, description, target date, horizontal progress bar, associated tasks list, inline "Add Task" button, and live Deterministic Risk Radar findings.

### Screen 7: Tasks Explorer
- **Class:** `TasksFragment.java` | **Layout:** `fragment_tasks.xml`
- **Purpose:** Task backlog and sprint management.
- **Features:** Search input, status tab filters (`All`, `Today`, `Overdue`, `Upcoming`), priority tags (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`), due date badges, and tap-through to `TaskDetailsActivity`.

### Screen 8: Create Task Form
- **Class:** `CreateTaskActivity.java` | **Layout:** `activity_create_task.xml`
- **Purpose:** Validated form for scheduling new project tasks.
- **Fields:** Title, description, project selector, priority radio options (`Low`, `Medium`, `High`), and interactive native `DatePickerDialog`.

### Screen 9: Task Details
- **Class:** `TaskDetailsActivity.java` | **Layout:** `activity_task_details.xml`
- **Purpose:** Detailed view and status manipulation of an individual task.
- **Features:** Full title, description, priority badge, due date, subtask checklist, and one-tap status action buttons ("In Progress", "Mark Done") with immediate Room SQLite persistence.

### Screen 10: Dynamic Calendar & Scheduling
- **Class:** `CalendarFragment.java` | **Layout:** `fragment_calendar.xml`
- **Purpose:** Real calendar date navigation and deadline tracking.
- **Features:** Dynamic month math (no hardcoded dates), Previous/Next month arrows, "Today" quick shortcut, weekday header, grid of days, event markers on dates with tasks, and filtered task list for the selected day.

### Screen 11: Reports & Analytics
- **Class:** `ReportsFragment.java` | **Layout:** `fragment_reports.xml`
- **Purpose:** Objective operational analytics and workload telemetry.
- **Features:** Live completion rate %, overdue task count, real proportional Task Status Distribution bar (Done, In Progress, To Do), active project progress leaderboard, and team member workload allocation.

### Screen 12: Notifications
- **Class:** `NotificationsFragment.java` | **Layout:** `fragment_notifications.xml`
- **Purpose:** System alerts, task assignments, and deadline warnings.
- **Features:** All vs Unread filter chips, "Mark all read" header action, relative timestamps, and one-tap read state updates.

### Screen 13: Chat & Team Discussions
- **Class:** `ChatFragment.java` | **Layout:** `fragment_chat.xml`
- **Purpose:** Real-time team collaboration and engineering channel.
- **Features:** Discussion header, incoming and outgoing message bubbles, message composer with send button, and persistent history via backend API.

### Screen 14: My Profile
- **Class:** `ProfileFragment.java` | **Layout:** `fragment_profile.xml`
- **Purpose:** Authenticated user management and preferences.
- **Features:** Avatar initials, display name, email, role badge, organization info, edit display name dialog, three-way theme switcher (Light मिट्टी, Dark Clay, System Default), and sign-out confirmation.

### Screen 15: Settings & APK Distribution
- **Class:** `SettingsFragment.java` | **Layout:** `fragment_settings.xml`
- **Purpose:** Application configuration, sync telemetry, and verified distribution.
- **Features:** Application release info, verified SHA-256 APK sharing via Android's native share sheet (`FileProvider`), and offline sync telemetry.

### Screen 16: Navigation Drawer
- **Class:** `MainActivity.java` | **Layout:** `drawer_header.xml`, `drawer_menu.xml`
- **Purpose:** Centralized sidebar navigation.
- **Features:** Approved brand logo, workspace identifier, and semantic vector icon links to all 10 core destinations.
