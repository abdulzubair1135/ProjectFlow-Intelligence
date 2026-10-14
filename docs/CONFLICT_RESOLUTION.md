# Conflict Resolution Policy

When multiple clients update the same record while disconnected, ProjectFlow Intelligence applies a deterministic **Last-Write-Wins (LWW) with Audit Snapshotting** strategy.

---

## Conflict Evaluation Workflow

1. **Entity Versioning**:
   - Every core entity (`projects`, `tasks`) maintains a `version` (optimistic locking counter) and `updated_at` (UTC timestamp).

2. **Resolution Rules**:
   - **Non-Conflicting Field Updates**: If Client A updates `title` and Client B updates `dueDate`, the server merges the discrete field modifications cleanly.
   - **Conflicting Concurrent Updates**: If two clients mutate the same field concurrently, the update with the highest server-verified timestamp wins.
   - **Audit Snapshot**: The superseded state is preserved in the `audit_logs` table under the action `SYNC_CONFLICT_RESOLVED`, including both incoming payload and previous server state.

3. **Manual Conflict Inspection**:
   - Both the Web Admin Console (`/audit-logs`) and the Windows Desktop Manager (`Sync & Conflicts` tab) provide full visibility into conflict snapshots.
   - Managers can review conflict events and revert or re-apply individual field values at any time.
