# QA Acceptance Test Plan

## Acceptance Test Matrix

| Test Case ID | Test Description | Expected Result | Status |
|---|---|---|---|
| **QA-TC01** | Evaluate overdue task in Risk Radar | Returns `CRITICAL` severity with rule `OVERDUE_TASK` | **PASSED** |
| **QA-TC02** | Add circular dependency A -> B -> A | `DependencyValidator` detects cycle and prevents insertion | **PASSED** |
| **QA-TC03** | Propagate delay in What-If scenario | Cascades completion date forward to dependent successors | **PASSED** |
| **QA-TC04** | Access `/api/v1/workspaces` without token | Server returns `401 Unauthorized` | **PASSED** |
| **QA-TC05** | Access non-member workspace | Server returns `403 Forbidden` | **PASSED** |
| **QA-TC06** | Load Admin Panel with clean database | Shows clean empty-state with "Create Workspace" CTA | **PASSED** |
| **QA-TC07** | Build Windows Desktop Manager | JavaFX application assembles into binary ZIP/TAR | **PASSED** |
| **QA-TC08** | Build Native Android APK | Room compiler generates DAOs, assembleDebug produces APK | **PASSED** |
| **QA-TC09** | Deploy APK to physical phone via ADB | APK streams, installs, and launches `LoginActivity` | **PASSED** |
| **QA-TC10** | Offline task creation on Android | Saves to Room SQLite, increments pending queue count | **PASSED** |
