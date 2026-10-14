# Windows Desktop Application Guide

The **ProjectFlow Windows Desktop Application** provides an advanced manager workspace built using JavaFX 21 and styled with the Natural Mineral Design System.

---

## Technical Stack
- **Language**: Java 21 LTS
- **UI Framework**: JavaFX 21 (Controls, FXML, Graphics)
- **Local Persistence**: SQLite JDBC 3.45 (`projectflow_desktop.db`)
- **Core Engine Integration**: Direct binary dependency on `:shared-core`
- **HTTP Client**: Built-in Java 11+ `java.net.http.HttpClient` with Jackson 2.17

---

## Manager Sections & Interactive Capabilities

1. **Dashboard**: High-level telemetry, workspace metrics, and Risk Radar alert summary.
2. **Workspaces**: Multi-tenant workspace explorer with modal creation dialog.
3. **Projects**: Initiative portfolio table with baseline date tracking and status filters.
4. **Task Board (Kanban)**: Visual columns for TODO, IN_PROGRESS, and DONE with inline progress bars.
5. **Task Table**: High-density sortable grid for operational planning.
6. **Dependencies**: Visual predecessor-successor chain inspector with DAG cycle validation.
7. **Risk Radar**: Real-time evaluation of 6 deterministic rules with recommended remediations.
8. **Schedule Planner**: Simulation sandbox to test task delays and duration modifications.
9. **Scenario Comparison**: Side-by-side delta calculation between baseline and proposed dates.
10. **Workflow Config**: View and configure allowable status transitions.
11. **Team Management**: Member workload and capacity allocation monitor.
12. **Automation Rules**: Event-driven automation configuration.
13. **Activity History**: Audit log of local and remote state transitions.
14. **Sync & Conflicts**: Local SQLite synchronization status with Last-Write-Wins inspection.
15. **Profile & Settings**: API endpoint server configuration and cache control.

---

## Running the Desktop Application
Run `START_DESKTOP.bat` or execute:
```bat
.\gradlew.bat :desktop-app:run
```
