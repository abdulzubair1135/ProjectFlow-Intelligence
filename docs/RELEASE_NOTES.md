# Release Notes - ProjectFlow Intelligence v2.0.0

**Release Date**: October 14, 2026  
**Status**: General Availability (GA)

---

## Major Highlights

### 1. Deterministic Risk Radar Engine
- 6 automated rules running without subjective score fabrication: Overdue Tasks, Approaching Deadlines, Blocked Tasks, Stalled Progress, Capacity Overloads, and Project Deadline Risks.

### 2. Topological What-If Schedule Simulator
- In-memory simulation allowing project managers to evaluate delay cascades and duration modifications on alternative branches before committing to baseline records.

### 3. Enterprise Multi-Tenancy & Flyway V1
- 19 normalized database tables with strict tenant workspace isolation, composite indexing, and audit logging.
- Dual-mode persistence: Supabase PostgreSQL in production, H2 in PostgreSQL compatibility mode for local dev.

### 4. Natural Mineral Admin Control Panel
- Web console with real-time telemetry, user management, workspace administration, and audit event explorer.

### 5. Windows Desktop Manager Workspace
- JavaFX 21 desktop application with 15 manager sections, interactive Kanban board, and SQLite JDBC local caching.

### 6. Native Android Mobile Application
- Android 15 (API 35) application with Room persistence, WorkManager background synchronization, and verified physical device deployment on `AI+ Nova 1 5G`.
