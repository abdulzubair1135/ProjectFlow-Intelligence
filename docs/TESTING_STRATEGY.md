# Testing Strategy & Test Automation

ProjectFlow Intelligence enforces a multi-tier testing pyramid ensuring zero regressions across all shared mathematical models, backend endpoints, security boundaries, and user interfaces.

---

## Test Tiers

1. **Shared-Core Mathematical Verification**:
   - `DeterministicRiskEngineTest`: Verifies all 6 rules under boundary conditions (overdue dates, capacity overflow, blocked tasks).
   - `DependencyValidatorTest`: Verifies cycle detection on complex multi-node directed acyclic graphs.
   - `WhatIfSchedulingEngineTest`: Asserts forward-pass topological calculation and variance day math.

2. **Backend Integration & Security Tests**:
   - `ProjectFlowApplicationTests`: Spring Boot context loading and bean wiring.
   - `SecurityAndAccessTests`: Asserts 401 Unauthorized on unauthenticated access and 403 Forbidden on cross-tenant access.
   - `CoreProjectManagementTests`: Full lifecycle integration testing of workspace creation, project initiatives, task dependencies, and risk queries.

3. **Frontend Build & Type Integrity**:
   - `tsc` and `vite build` validating all TypeScript interfaces, component props, and API client responses.

4. **Multi-Platform Assembly**:
   - `:desktop-app:assemble`: Verifies JavaFX 21 packaging.
   - `:android-app:assembleDebug`: Verifies Android Room schemas, AAPT2 resource linking, and APK generation.
