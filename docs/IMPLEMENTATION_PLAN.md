# ProjectFlow Intelligence — Implementation Plan

## 1. Development Phases

- **Phase 0: Environment & Repository Audit** [COMPLETED]
  - Verified Java 21 LTS runtime, Node v25, ADB, and physical Android 15 device (`AI+ Nova 1 5G`).
  - Configured Gradle 8.9 wrapper with Unicode filesystem safety patch.

- **Phase 1: Project Build Structure & Shared Core** [IN PROGRESS]
  - Root Gradle build configuration with multi-module setup (`shared-core`, `backend`, `desktop-app`, `android-app`).
  - Implement `shared-core` domain models, deterministic Risk Engine rules, and What-If Scheduling math.

- **Phase 2: Database Design, Flyway Migrations & Supabase Auth**
  - PostgreSQL normalized schema (`profiles`, `organizations`, `workspaces`, `workspace_members`, `projects`, `project_members`, `workflows`, `workflow_statuses`, `workflow_transitions`, `tasks`, `task_dependencies`, `task_comments`, `activity_events`, `risk_events`, `schedule_scenarios`, `scenario_changes`, `automation_rules`, `audit_logs`).
  - Flyway migrations and localized development test support.
  - Spring Security token authentication filter & Supabase JWT verification.

- **Phase 3: Spring Boot Backend Application**
  - Modular package structure with clean controller-service-repository separation.
  - Endpoints under `/api/v1/` for auth, workspaces, projects, tasks, workflows, risks, scenarios, sync, admin overview, health, and audit.
  - Unit and integration tests using JUnit 5 and Mockito.

- **Phase 4: Web Admin Control Panel**
  - React + TypeScript + Vite with Natural Mineral Design System (#F4F2E9, #78866B, #56664C, #292D28).
  - Routes: `/login`, `/dashboard`, `/users`, `/organizations`, `/workspaces`, `/projects`, `/tasks`, `/workflows`, `/risks`, `/automations`, `/audit-logs`, `/system-health`, `/system-errors`, `/settings`, `/profile`.
  - Responsive tables, search/filter controls, loading skeletons, error states, and zero fake data.

- **Phase 5: Windows Desktop Application**
  - JavaFX + FXML + SQLite JDBC local cache.
  - Manager-focused layout: Collapsible sidebar, project tables, Kanban board, Risk Radar, and What-If comparison tool.

- **Phase 6: Android Application**
  - Native Java + XML + AndroidX + Room + WorkManager + Retrofit.
  - 31 required functional screens/modules, including offline queue and conflict resolution.
  - Debug APK build, ADB packaging, installation on the connected `AI+ Nova 1 5G` phone.

- **Phase 7: Offline-First Synchronization Engine**
  - Persistent outbox queue, idempotency keys, version conflict detection, and retry with exponential backoff.

- **Phase 8: Windows Automation Scripts & Tooling**
  - `START_PROJECTFLOW.bat`, `STOP_PROJECTFLOW.bat`, `BUILD_PROJECTFLOW.bat`, `TEST_PROJECTFLOW.bat`, `INSTALL_ANDROID.bat`, `CHECK_ENVIRONMENT.bat`.
  - PowerShell automation helpers in `scripts/`.

- **Phase 9: Quality Assurance & Automated Testing**
  - Comprehensive unit and integration test suite execution across all modules.

- **Phase 10: Complete Documentation & Final Report**
  - Markdown documentation in `docs/` matching exact codebase implementations.
