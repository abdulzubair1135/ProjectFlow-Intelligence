# ProjectFlow Intelligence — Requirements Traceability Matrix (RTM)

**Version:** 3.0.0  
**Compliance Standard:** Complete Master Specification  
**Status Key:** `VERIFIED`, `IMPLEMENTED`, `TESTED`

---

| Req ID | Phase | Feature / Requirement | Implementation File(s) | Backend / DB Dependency | UI / Client Dependency | Verification Evidence | Status |
|---|---|---|---|---|---|---|:---:|
| **REQ-01** | Phase 1 | User Auth & Session Restoration | `LoginActivity.java`, `SessionManager.java` | `POST /api/v1/auth/dev-login` | `activity_login.xml` | ADB launch tested, session restored | `VERIFIED` |
| **REQ-02** | Phase 1 | Workspace Multi-Tenancy | `WorkspaceEntity.java`, `ProjectFlowDao.java` | `GET/POST /api/v1/workspaces` | `fragment_profile.xml` | Workspace isolation verified | `VERIFIED` |
| **REQ-03** | Phase 1 | Project Management | `ProjectsFragment.java`, `ProjectDetailsActivity.java` | `GET/POST /api/v1/projects` | `fragment_projects.xml` | Created and displayed in Room | `VERIFIED` |
| **REQ-04** | Phase 1 | Task Management & CRUD | `TasksFragment.java`, `CreateTaskActivity.java` | `GET/POST/PATCH /api/v1/tasks` | `fragment_tasks.xml` | Task "Design System Review" created & verified | `VERIFIED` |
| **REQ-05** | Phase 1 | Kanban & Status Filtering | `TasksFragment.java`, `TaskDetailsActivity.java` | `PATCH /tasks/:id` | Tab filters: All, Today, Overdue, Upcoming | Filter chips tested on device | `VERIFIED` |
| **REQ-06** | Phase 1 | Dependency & Cycle Validation | `shared-core/.../DependencyValidator.java` | `test/backend.test.js` | Direct acyclic graph validation | 100% passed in `npm test` | `VERIFIED` |
| **REQ-07** | Phase 1 | Team Collaboration Directory | `TeamFragment.java`, `TeamMembersAdapter.java` | `GET /api/v1/team` | `fragment_team.xml` | Role filtering & modal tested | `VERIFIED` |
| **REQ-08** | Phase 1 | Real-time Chat Discussions | `ChatFragment.java`, `ChatAdapter.java` | `GET/POST /api/v1/chat/messages` | `fragment_chat.xml` | Dual-bubble layout verified | `VERIFIED` |
| **REQ-09** | Phase 1 | Deadline & Overdue Alerts | `NotificationsFragment.java` | `GET /api/v1/notifications` | `fragment_notifications.xml` | Relative time & read toggle verified | `VERIFIED` |
| **REQ-10** | Phase 1 | Offline Sync Engine | `ProjectFlowRepository.java`, `SyncWorker.java` | Room SQLite `sync_queue` | `bg_badge_success.xml` | Room optimistic writes verified | `VERIFIED` |
| **REQ-11** | Phase 2 | Deterministic Risk Radar | `DeterministicRiskEngine.java`, `deterministicRiskEngine.js` | `POST /projects/:id/risks/evaluate` | `activity_project_details.xml` | Tested with overdue task trigger | `VERIFIED` |
| **REQ-12** | Phase 2 | What-If Scenario Planning | `WhatIfSchedulingEngine.java` | `test/backend.test.js` | Scenario comparison calculation | Computed finish date: 2026-11-20 | `VERIFIED` |
| **REQ-13** | Phase 2 | Reports & Analytics Graphs | `ReportsFragment.java` | `GET /api/v1/reports/summary` | `fragment_reports.xml` | Status distribution bar & rate % verified | `VERIFIED` |
| **REQ-14** | Phase 2 | Dynamic Month Calendar | `CalendarFragment.java`, `CalendarAdapter.java` | Local date calculations | `fragment_calendar.xml` | October 2026 computed on device | `VERIFIED` |
| **REQ-15** | Phase 3 | User Profile & Integrated Settings | `ProfileFragment.java`, `MainActivity.java` | `PATCH /api/v1/me/profile` | `fragment_profile.xml` | Bottom nav 5th tab + embedded settings | `VERIFIED` |
| **REQ-16** | Phase 3 | Native APK Sharing System | `ApkDistributionManager.java` | `FileProvider` (`content://...`) | `fragment_profile.xml` | Verified on physical device | `VERIFIED` |
| **REQ-17** | Phase 3 | Web Admin Panel | `admin-panel/src/` | Supabase REST APIs | React 18 + Vite + TS | Bundled cleanly (`dist/` in 8.73s) | `VERIFIED` |
| **REQ-18** | Phase 3 | Windows Desktop Application | `desktop-app/src/` | `DesktopApiClient.java` | JavaFX 21 + FXML | JAR built successfully in 14s | `VERIFIED` |
