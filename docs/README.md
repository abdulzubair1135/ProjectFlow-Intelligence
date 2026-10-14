# ProjectFlow Intelligence

**ProjectFlow Intelligence** is an enterprise cross-platform project management and decision-support platform designed to help teams identify and mitigate project delays before they impact delivery timelines.

---

## Key Capabilities

1. **Deterministic Risk Radar**: 6 mathematical rules continuously analyze live application tasks, dependency chains, and member workloads to detect overdue work, approaching deadlines, circular or blocked dependencies, stalled velocity, and capacity saturation—without subjective score fabrication.
2. **What-If Schedule Impact Simulation**: A topological forward-pass scheduling engine that computes the precise cascade of delayed predecessor tasks onto all downstream dependencies and project milestones before managers commit changes to production records.
3. **Cross-Platform Ecosystem**:
   - **Spring Boot 3.3 Backend**: Stateless JWT authentication, role-based access control (RBAC), multi-tenant workspace isolation, Flyway schema migrations, and PostgreSQL/Supabase compatibility with local H2 fallback.
   - **React 18 + Vite Admin Control Panel**: Natural Mineral Design System (`#56664C`, `#78866B`, `#F4F2E9`, `#FAFAF7`, `#292D28`), live telemetry, user management, workspace administration, and compliance audit trail.
   - **JavaFX 21 Desktop Manager Workspace**: Rich desktop experience with collapsible navigation, interactive Kanban board, task tables, What-If simulation sandbox, and local SQLite offline caching.
   - **Native Android Application**: Android 15 (API 35) compatible native application with Room SQLite persistence, WorkManager background synchronization, and Retrofit HTTP communications.
4. **Resilient Offline Synchronization**: Local SQLite databases in mobile and desktop clients queue write operations and synchronize automatically with the central server using Last-Write-Wins (LWW) conflict resolution backed by full audit logging.

---

## Repository Structure

```
projectflow-intelligence/
├── shared-core/       # Domain models, deterministic Risk Radar, and What-If scheduling engine
├── backend/           # Spring Boot 3.3.4 REST APIs, Flyway migrations, JPA entities, security
├── admin-panel/       # React 18, Vite, TypeScript, Tailwind CSS, Natural Mineral Design System
├── desktop-app/       # JavaFX 21 Windows desktop management workspace with SQLite caching
├── android-app/       # Native Android (API 26-35) Java application with Room and WorkManager
├── database/          # SQL migration scripts (V1__init_schema.sql)
├── docs/              # 33 comprehensive architecture and specification documents
├── scripts/           # Automation scripts
├── START_PROJECTFLOW.bat   # Unified service launcher
├── STOP_PROJECTFLOW.bat    # Clean shutdown script
├── BUILD_PROJECTFLOW.bat   # Complete multi-platform build runner
├── TEST_PROJECTFLOW.bat    # Full test suite execution runner
└── INSTALL_ANDROID.bat     # APK installer and ADB reverse port configurator
```

---

## Quick Start

### Prerequisites
- **Java**: OpenJDK 21 LTS (`JAVA_HOME` pointing to JDK 21)
- **Node.js**: Node v18+ and npm v9+
- **Android SDK**: API 34/35 platform tools and ADB (for Android installation)

### 1-Click Launch
Double-click `START_PROJECTFLOW.bat` or run:
```bat
START_PROJECTFLOW.bat
```
This launches:
- Backend REST API at `http://localhost:8080/api/v1`
- Admin Web Console at `http://localhost:5173`
- Configures ADB port reversal (`adb reverse tcp:8080 tcp:8080`) for attached mobile devices.

### Default Dev Credentials
- **System Administrator**: `admin@projectflow.io`
- **Project Manager**: `manager@projectflow.io`
- **Team Member**: `member@projectflow.io`
