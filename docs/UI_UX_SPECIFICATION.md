# ProjectFlow Intelligence — Comprehensive UI/UX Specification

## 1. Interaction Principles
- **No Dead Controls:** Every button, tab, card, and action must trigger a real operation, navigation, or feedback dialog.
- **Immediate Visual Feedback:** Buttons display ripple touch responses; loading operations show indeterminate progress bars or skeletons.
- **State Fidelity:** Screens cleanly transition between `Loading`, `Content`, `Empty`, and `Error` states.
- **Truth in Telemetry:** Dashboards and charts never display fake fallback numbers. If zero items exist, display clean zero counters or helpful empty prompts.

---

## 2. Screen-by-Screen Interaction Specification

### 2.1 Splash & Onboarding
- **Splash:** Async initialization of local repository and token verification. If auth token exists, seamlessly route to `MainActivity`; otherwise route to `OnboardingActivity` (or `LoginActivity` if already seen).
- **Onboarding:** Horizontal swipeable pages highlighting:
  1. *Streamlined Project Management*
  2. *Deterministic Risk Radar & Early Warnings*
  3. *Seamless Offline Synchronization & Native Sharing*
  - Includes *Skip*, *Continue*, and *Get Started* actions with bottom dot indicators.

### 2.2 Authentication
- **Login:**
  - Email format validation with `@` and domain checks.
  - Quick demo-role switcher chips (Admin, Manager, Member) for rapid developer testing.
  - Real password authentication via `/api/v1/auth/dev-login`.
  - Secure session token persistence in `EncryptedSharedPreferences` / private `SharedPreferences`.

### 2.3 Dashboard (Screen 4)
- **Header:** Personalized greeting ("Good Morning / Afternoon / Evening"), authenticated user's display name, user avatar circle, and notification bell shortcut with live unread counter.
- **Summary Metrics Grid:** 4 responsive cards:
  - Total Projects (actual count from Room `projectDao`)
  - Total Tasks (actual count from Room `taskDao`)
  - Completed Tasks (tasks where `statusCategory == 'DONE'`)
  - Pending / Overdue (tasks where `statusCategory != 'DONE'`)
- **Work Progress Card:** Animated circular progress ring showing overall task completion rate (`%`).
- **Quick Action Bar:** 4 one-tap actions:
  - *Create Task* -> launches `CreateTaskActivity`
  - *Create Project* -> displays modal project creator
  - *Calendar* -> switches to `CalendarFragment`
  - *Reports* -> switches to `ReportsFragment`
- **My Tasks & Recent Projects:** Real Recycler views with direct navigation on click.

### 2.4 Projects Explorer (Screen 5) & Project Details
- **Projects Explorer:**
  - Search input filtering by project name and code.
  - Status filter chips: `All`, `Active`, `Completed`, `On Hold`.
  - Project Cards: Name, code badge, description preview, status pill, horizontal progress bar, target date.
  - Floating Action Button (+) opens Create Project modal.
- **Project Details (`ProjectDetailsActivity`):**
  - Section tabs: `Overview`, `Tasks`, `Risks`.
  - Quick action to add task directly to project.

### 2.5 Tasks Explorer (Screen 6) & Task Details
- **Tasks Explorer:**
  - Filter chips: `All`, `My Tasks`, `Today`, `Upcoming`, `Overdue`, `Completed`.
  - Search bar.
  - Task Card: Checkbox for one-tap completion toggle, title, project code badge, priority tag (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`), and due date.
- **Task Details (`TaskDetailsActivity`):**
  - Full title, description, project association, assignee, start & due dates.
  - Action buttons: Mark In Progress, Mark Done, Delete.

### 2.6 Dynamic Calendar (Screen 10)
- **Month Navigation:** Previous `<` and Next `>` arrows with month and year header (e.g. "October 2026").
- **Grid Calculation:** Standard 7-column calendar grid calculating first day of month and total days.
- **Event Dots:** Real calculation: dates with tasks display status dots.
- **Selected Day Task Feed:** Selecting any date filters the task list to tasks due on that specific date.
- **"Today" Action:** Instant shortcut to jump back to current day.

### 2.7 Reports & Analytics (Screen 11)
- **Real Calculations:**
  - Completion Rate (%)
  - Status Distribution (TODO, IN_PROGRESS, REVIEW, DONE)
  - Overdue Task Count
  - Average Progress across active projects
- **Visual Charts:** Animated progress bars and rings styled in the earth-tone palette.

### 2.8 Notifications (Screen 13)
- List of system and project notifications with read/unread styling.
- "Mark all as read" button.
- Relative timestamp formatter ("5m ago", "2h ago", "Yesterday").

### 2.9 Chat & Team Discussions
- Direct project channel conversation view.
- Incoming vs outgoing message bubbles with timestamps and sender initials.
- Message input composer with send button.

### 2.10 Profile & Settings (Screens 12 & 14)
- **Profile:** User avatar, name, email, role badge, appearance switcher (Light, Dark, System), security settings, and logout action.
- **Settings:** Appearance, Notification permissions, Connectivity status with pending sync counter, and official **Share APK** native chooser.
