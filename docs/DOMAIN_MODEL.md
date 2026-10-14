# Domain Model Specification

## Core Domain Entities (`shared-core`)

### 1. `UserProfile`
Represents an authenticated user within the system.
- `id` (UUID): Unique user ID.
- `email` (String): Normalized unique email.
- `fullName` (String): Display name.
- `globalRole` (Role): `ADMINISTRATOR`, `MANAGER`, `MEMBER`, `VIEWER`, `OWNER`.
- `active` (boolean): Account activation flag.

### 2. `Workspace`
Tenant isolation domain boundary.
- `id` (UUID): Unique workspace identifier.
- `name` (String): Display name.
- `slug` (String): URL-safe unique tenant slug.
- `description` (String): Contextual overview.
- `ownerId` (UUID): Reference to owning user.

### 3. `Project`
A bounded project initiative within a workspace.
- `id` (UUID): Project ID.
- `workspaceId` (UUID): Partition key.
- `name` (String): Project title.
- `startDate` (LocalDate): Planned initiation date.
- `targetDate` (LocalDate): Project baseline deadline.
- `status` (String): `PLANNING`, `ACTIVE`, `ON_HOLD`, `COMPLETED`, `ARCHIVED`.

### 4. `Task`
A discrete unit of work within a project.
- `id` (UUID): Task ID.
- `projectId` (UUID): Parent project.
- `title` (String): Work unit summary.
- `priority` (TaskPriority): `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
- `statusId` (UUID): Reference to workflow status.
- `dueDate` (LocalDate): Hard commitment deadline.
- `estimatedHours` (double): Effort estimate.
- `completedAt` (Instant): Timestamp of completion.

### 5. `TaskDependency`
Predecessor-successor relationship between tasks.
- `id` (UUID): Dependency ID.
- `taskId` (UUID): Successor task.
- `predecessorTaskId` (UUID): Predecessor task.
- `dependencyType` (String): `FINISH_TO_START`.

### 6. `RiskFinding`
Deterministic anomaly detected by `DeterministicRiskEngine`.
- `ruleId` (RiskRuleId): Identifier of evaluated rule.
- `severity` (RiskSeverity): `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.
- `affectedEntityName` (String): Task or milestone name.
- `explanation` (String): Root-cause rationale.
- `suggestedAction` (String): Actionable resolution guidance.

### 7. `ScheduleScenario` & `ScenarioChange`
Isolated simulation model testing changes before baseline commit.
- `proposedDueDate` (LocalDate): Simulated new completion date.
- `proposedEstimatedHours` (Double): Adjusted workload estimate.
