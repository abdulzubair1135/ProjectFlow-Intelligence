# What-If Scheduling Simulation Engine

The **What-If Scheduling Engine** (`WhatIfSchedulingEngine`) simulates schedule impacts on isolated alternative branches without modifying live production records until managerial approval.

---

## Topological Forward-Pass Algorithm

1. **Cycle Detection (DAG Validation)**:
   - Validates that the task dependency graph is a strict Directed Acyclic Graph (DAG) using Tarjan/Kahn depth-first traversal.
   - If circular dependencies exist (e.g. A -> B -> C -> A), the simulation aborts with `valid = false`.

2. **In-Memory Task Cloning**:
   - Deep-clones all project tasks into an isolated simulation map to ensure baseline records remain strictly immutable.

3. **Scenario Delta Overlay**:
   - Applies the proposed adjustments (`proposedDueDate`, `proposedEstimatedHours`, `proposedAssigneeId`) to the targeted tasks.

4. **Forward Propagation Along Dependency Chains**:
   - For every task in topological order:
     $$\text{EarlyStart}(S) = \max_{P \in \text{Predecessors}(S)} \left( \text{FinishDate}(P) \right)$$
     $$\text{FinishDate}(S) = \max \left( \text{DueDate}(S), \text{EarlyStart}(S) + \lceil \text{EstimatedHours} / 8 \rceil \right)$$
   - Any delay in a predecessor task cascades forward automatically through all direct and indirect successors.

5. **Impact Comparison**:
   - Compares simulated finish date against baseline finish date to compute `deltaDays`.
   - Re-evaluates all 6 Risk Radar rules against the simulated schedule to report `newRisksCount` and `resolvedRisksCount`.

6. **Manager Approval Workflow**:
   - A manager can review the side-by-side comparison.
   - Upon explicit approval, changes are written to the live database within a transactional boundary and logged to the audit trail.
