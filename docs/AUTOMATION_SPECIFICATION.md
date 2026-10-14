# Automation Engine Specification

The **ProjectFlow Automation Engine** executes event-driven rules to eliminate manual repetitive administrative overhead.

---

## Supported Automation Triggers & Actions

| Trigger Event | Evaluated Condition | Automated Action |
|---|---|---|
| `TASK_COMPLETED` | Successor tasks are in `BLOCKED` status | Transition successor tasks to `TODO` and notify assignees |
| `RISK_DETECTED` | Severity is `CRITICAL` or `HIGH` | Post alert notification to workspace manager dashboard |
| `DUE_DATE_PASSED` | Task is incomplete | Flag as `OVERDUE_TASK` in Risk Radar and adjust project health to `AT_RISK` |
| `SCENARIO_APPROVED` | Manager approves What-If schedule | Apply scenario task dates to baseline and recalculate critical path |

---

## Execution Guarantees
- Rules execute asynchronously via Spring application event dispatchers (`@Async`).
- All automated actions record the actor as `SYSTEM_AUTOMATION` in `audit_logs` with the trigger ID in the metadata payload.
