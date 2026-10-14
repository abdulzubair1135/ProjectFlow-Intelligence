# ProjectFlow Intelligence — Comprehensive Test Plan

**Document Version:** 2.0.0  
**Test Scope:** Android Frontend, Node.js Backend REST APIs, Local Room Database, Physical Hardware Device Integration

---

## 1. Test Levels & Strategy

The testing strategy validates the end-to-end user workflows, database integrity, error resilience, and visual compliance:

```
┌────────────────────────────────────────────────────────┐
│           PHYSICAL DEVICE HARDWARE TESTING (ADB)       │
│  Navigation, UI Fluidity, Room Observers, Native Share │
├────────────────────────────────────────────────────────┤
│           INTEGRATION & REST API TESTING               │
│  Supertest HTTP suites, Supabase Mock, Auth Headers    │
├────────────────────────────────────────────────────────┤
│           LOCAL CACHE & DATABASE TESTS (ROOM)          │
│  Entity CRUD, LiveData Subscriptions, WorkManager Sync │
└────────────────────────────────────────────────────────┘
```

---

## 2. Test Matrix & Scenarios

### 2.1 Backend API Test Suites
- **Auth & Session Suite:** Verify token issuance, permission claims, and protected route access control.
- **Projects & Workspaces Suite:** Test creation, hierarchical listing, status updates, and constraint validations.
- **Tasks & Mutations Suite:** Validate status categories (`TODO`, `IN_PROGRESS`, `DONE`), deadline calculations, and priority sorting.
- **Collaboration Suite:** Validate chat messages retrieval, message posting, team member roster, and notifications toggling.

### 2.2 Android UI/UX & Functional Scenarios
- **TC-AND-01 (Splash & Session Check):** App cold boots, displays branded logo, validates session, and transitions to Dashboard.
- **TC-AND-02 (Dashboard Dynamic Metrics):** Confirms counts are sourced directly from Room, updates dynamically upon task creation, and renders animated `CircularProgressView`.
- **TC-AND-03 (Tasks Workflow):** Create task modal with title, priority selection, due date picker, instant Room write, and list update.
- **TC-AND-04 (Calendar Navigation):** Dynamically computes days of month, highlights today, allows Previous/Next month paging, and shows task deadlines.
- **TC-AND-05 (Reports & Analytics):** Dynamic status distribution bar, real calculated completion rate, active project progress.
- **TC-AND-06 (Team Members Directory):** Search bar filtering, role chips (All, Admins, Managers, Members), initials avatar.
- **TC-AND-07 (Team Chat Discussions):** Incoming and outgoing bubbles, timestamping, message posting.
- **TC-AND-08 (Theme Switcher):** Light and Dark palette rendering adhering to the Natural Mineral (मिट्टी) tokens.
- **TC-AND-09 (APK Distribution):** Settings → Share APK triggers Android native ShareSheet with real APK binary via FileProvider.
