# Security Audit Trail and Compliance

ProjectFlow Intelligence provides tamper-evident logging for all critical platform activities, supporting SOC 2, ISO 27001, and enterprise governance requirements.

---

## Audited Events

1. **Authentication & Session**: `USER_LOGIN`, `USER_LOGOUT`, `TOKEN_REFRESH`, `LOGIN_FAILED`.
2. **Access Control**: `USER_ROLE_CHANGED`, `WORKSPACE_MEMBER_ADDED`, `WORKSPACE_MEMBER_REMOVED`.
3. **Project Management**: `PROJECT_CREATED`, `PROJECT_ARCHIVED`, `DEADLINE_AMENDED`.
4. **Dependency & Workflow**: `DEPENDENCY_ADDED`, `DEPENDENCY_REMOVED`, `TRANSITION_OVERRIDDEN`.
5. **Simulation & Decision**: `SCENARIO_CREATED`, `SCENARIO_APPROVED`, `SCENARIO_REJECTED`.
6. **Data Synchronization**: `SYNC_BATCH_APPLIED`, `SYNC_CONFLICT_RESOLVED`.

---

## Log Immutability & Retention
- Audit records in `audit_logs` are write-only (`INSERT` only). `UPDATE` and `DELETE` queries are prohibited at the repository layer.
- Each log contains: `id`, `workspace_id`, `actor_id`, `action`, `entity_type`, `entity_id`, `metadata` (JSON snapshot), and `timestamp`.
- The Web Admin Console exposes a dedicated `/audit-logs` view with search and pagination for compliance officers.
