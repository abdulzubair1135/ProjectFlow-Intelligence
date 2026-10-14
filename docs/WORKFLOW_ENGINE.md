# Custom Workflow Engine Specification

The **ProjectFlow Workflow Engine** governs state transitions and validation rules for tasks progressing across lifecycle stages.

---

## Workflow Graph Representation

A workflow consists of:
- **Statuses**: Discrete states belonging to a category (`TODO`, `IN_PROGRESS`, `REVIEW`, `DONE`, `CANCELLED`).
- **Transitions**: Permitted directed edges between statuses:
  $$\text{Transition}(S_{\text{from}} \to S_{\text{to}}, \text{RequiredRoles})$$

---

## Transition Enforcement Rules

1. **Predecessor Clearance**:
   - A task cannot transition into `IN_PROGRESS` or `DONE` if it has uncompleted predecessor dependencies (`DependencyType == FINISH_TO_START`), unless overridden by a Project Manager with audit recording.
2. **Role Authorization**:
   - Transitions into `DONE` or `REOPENED` require `MANAGER` or `ADMINISTRATOR` privileges.
3. **Completion Timestamp Recording**:
   - Moving a task into the `DONE` category automatically stamps `completed_at = NOW()`.
   - Reopening a completed task clears `completed_at` and creates an audit event.
