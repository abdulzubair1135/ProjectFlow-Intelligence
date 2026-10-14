# ProjectFlow Intelligence — Frontend Architecture Specification

**Document Version:** 2.0.0  
**Target:** Android Client (`com.projectflow.android`)  
**Language:** Java 17 + Android SDK  
**Build System:** Gradle 8.9 / AGP 8.5.2  
**Architecture Pattern:** Repository Pattern + MVVM-ready ViewBinding + Single-Activity/Multi-Fragment Core

---

## 1. Architectural Overview

ProjectFlow Intelligence employs an offline-first, layered architecture designed for low latency, fault tolerance, and fluid user interactions. The application cleanly decouples UI representation, reactive data synchronization, and remote API persistence.

```
┌─────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                     │
│  Activities (Splash, Login, Main, ProjectDetails, TaskDetails)  │
│  Fragments (Dashboard, Projects, Tasks, Calendar, Reports, etc.)│
│  Custom Views (CircularProgressView, BottomSheetModals)         │
└───────────────────────────────┬─────────────────────────────────┘
                                │ Observes LiveData / Events
┌───────────────────────────────▼─────────────────────────────────┐
│                           DOMAIN LAYER                          │
│  Business Rules, Status State Machines, Filter Engines, DTOs    │
└───────────────────────────────┬─────────────────────────────────┘
                                │ Calls Repository
┌───────────────────────────────▼─────────────────────────────────┐
│                        DATA / REPOSITORY LAYER                  │
│  ProjectFlowRepository (Single Source of Truth, Executor Pool)  │
└────────────────┬────────────────────────────────┬───────────────┘
                 │ Local Cache                    │ Remote Sync
┌────────────────▼───────────────┐┌───────────────▼───────────────┐
│          ROOM DATABASE         ││       RETROFIT API CLIENT     │
│  AppDatabase, Room DAOs        ││  ApiService (REST Endpoints)  │
│  SQLite Persistence (Offline)  ││  OkHttp Authenticator/Bearer  │
└────────────────────────────────┘└───────────────────────────────┘
                 │                                │
                 └───────────────┬────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │ WORKMANAGER SYNC QUEUE  │
                    │ SyncWorker, SQLite Log  │
                    └─────────────────────────┘
```

---

## 2. Directory Structure

```
android-app/src/main/java/com/projectflow/android/
├── data/
│   ├── api/
│   │   ├── ApiClient.java          # Retrofit singleton with OkHttp interceptors
│   │   ├── ApiService.java         # REST endpoint declarations (Projects, Tasks, Team, Chat, etc.)
│   │   └── DtoModels.java          # Typed Network Data Transfer Objects
│   ├── db/
│   │   ├── AppDatabase.java        # Room database definition with schema migrations
│   │   └── ProjectFlowDao.java     # Room data access objects with LiveData queries
│   ├── model/
│   │   ├── WorkspaceEntity.java    # Local workspace entity
│   │   ├── ProjectEntity.java      # Local project entity
│   │   ├── TaskEntity.java         # Local task entity
│   │   ├── RiskEntity.java         # Local risk entity
│   │   └── SyncQueueEntity.java    # Offline mutation transaction queue
│   ├── repository/
│   │   └── ProjectFlowRepository.java # Coordinator of Room + Retrofit + WorkManager
│   └── sync/
│       └── SyncWorker.java         # Background WorkManager periodic/opportunistic sync
├── ui/
│   ├── view/
│   │   └── CircularProgressView.java # Custom Canvas animated circular progress view
│   ├── adapter/
│   │   ├── CalendarAdapter.java    # Dynamic grid calendar day cell adapter
│   │   ├── ChatAdapter.java        # Dual-bubble (incoming/outgoing) chat recycler adapter
│   │   ├── NotificationsAdapter.java # Notification list with read status toggling
│   │   ├── TeamMembersAdapter.java # Team member cards with role chips & initials avatar
│   │   ├── ProjectsAdapter.java    # Project explorer cards with health & progress
│   │   └── TasksAdapter.java       # Task checklist cards with priority badges & action menu
│   ├── auth/
│   │   ├── LoginActivity.java      # Auth & Dev-Login screen
│   │   └── OnboardingActivity.java # 3-step animated feature onboarding
│   ├── project/
│   │   └── ProjectDetailsActivity.java # Deep-dive project hub with tabs (Tasks, Risks, Team)
│   ├── task/
│   │   ├── TaskDetailsActivity.java    # Task inspector with status transition state machine
│   │   └── CreateTaskActivity.java     # Task creation modal with dynamic date picker
│   ├── settings/
│   │   └── ShareApkActivity.java   # Native FileProvider installer distribution engine
│   ├── SplashActivity.java         # Brand reveal & session validation entrypoint
│   └── MainActivity.java           # Primary shell hosting NavigationDrawer & BottomNavigation
└── util/
    ├── NetworkUtils.java           # Active connectivity state monitor
    └── SessionManager.java         # Encrypted SharedPreferences token & profile store
```

---

## 3. Core Component Lifecycles & Data Flow

### 3.1 Single Source of Truth Pattern
All UI components read directly from Room (using `LiveData` or asynchronous queries dispatched on the repository's `ExecutorService`). When the user acts:
1. The local Room database is updated optimistically.
2. The UI immediately reflects the updated state without spinner latency.
3. If online, the Retrofit API service is asynchronously called to mutate the backend.
4. If offline, the mutation is serialized and committed to `sync_queue` table.
5. `SyncWorker` drains the `sync_queue` once network connectivity is restored.

### 3.2 Dynamic Earth-Tone UI Theming Engine
The presentation layer adheres strictly to the **मिट्टी (Natural Earth)** design system:
- Light Theme: Warm clay, sand background (`#F3EDE2`), natural cream cards (`#FFFDF8`), terracota primary accents (`#795B43`), olive secondary badges (`#47745A`).
- Dark Theme: Deep basalt clay (`#211C17`), charcoal surface (`#2B241E`), burnt sienna accents (`#C3A17D`).
- Theming is switchable at runtime without app restart via `AppCompatDelegate.setDefaultNightMode()` and persisted in `SessionManager`.

### 3.3 Navigation Routing
- `MainActivity` controls 8 primary destinations via fragment replacement transactions accompanied by custom slide and fade animations (`R.anim.slide_in_right`, `R.anim.slide_out_left`).
- Contextual actions (Project Details, Task Details, Create Task, Share APK) transition smoothly to dedicated Activities with custom window transition styles.
