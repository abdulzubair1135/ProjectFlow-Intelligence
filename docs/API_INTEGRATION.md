# ProjectFlow Intelligence — API & Backend Integration Guide

**Document Version:** 2.0.0  
**Target:** Android Client (`com.projectflow.android`) & REST Backend (`server.js`)  
**Network Protocol:** HTTP/1.1 REST + JSON Payloads  
**Authentication:** Bearer JWT Token Authorization Header

---

## 1. Overview & Base Configuration

The ProjectFlow Intelligence mobile application connects to the unified Node.js/Supabase backend services via a configured Retrofit 2 REST client.

### Environment & Endpoints
- **Base URL (Local Network / Emulator):** `http://10.0.2.2:3000/api/v1/`
- **Base URL (Physical Device / LAN):** `http://<HOST_IP>:3000/api/v1/`
- **Base URL (Production Cloud / Render):** `https://projectflow-api.onrender.com/api/v1/`

All mobile network requests are routed through `com.projectflow.android.data.api.ApiClient`, which automatically injects:
```
Authorization: Bearer <SUPABASE_AUTH_TOKEN>
Content-Type: application/json
Accept: application/json
User-Agent: ProjectFlow-Android/2.0.0
```

---

## 2. API Endpoint Specification

### 2.1 Workspace & Projects
| Method | Endpoint | Description | Request Body | Response Payload |
|---|---|---|---|---|
| `GET` | `/workspaces` | Retrieve all workspaces user has access to | None | `List<WorkspaceDto>` |
| `GET` | `/projects` | Retrieve all projects under active workspace | Query: `workspace_id` | `List<ProjectDto>` |
| `POST` | `/projects` | Create a new project | `{ name, description, priority, deadline }` | `ProjectDto` |
| `GET` | `/projects/:id` | Retrieve single project details with risks | None | `ProjectDetailDto` |

### 2.2 Tasks & Workflow
| Method | Endpoint | Description | Request Body | Response Payload |
|---|---|---|---|---|
| `GET` | `/tasks` | List all tasks | Query: `project_id`, `status` | `List<TaskDto>` |
| `POST` | `/tasks` | Create new task | `{ title, description, project_id, status, priority, due_date, assignee_id }` | `TaskDto` |
| `PATCH` | `/tasks/:id` | Update task status or metadata | `{ status, priority, due_date, assignee_id }` | `TaskDto` |
| `DELETE` | `/tasks/:id` | Delete task | None | `{ success: true }` |

### 2.3 Team Members & Collaboration
| Method | Endpoint | Description | Request Body | Response Payload |
|---|---|---|---|---|
| `GET` | `/team` | Fetch workspace roster & role assignments | None | `List<TeamMemberDto>` |
| `GET` | `/team/:id` | Fetch single team member activity profile | None | `TeamMemberDto` |

### 2.4 Real-Time Chat & Discussion
| Method | Endpoint | Description | Request Body | Response Payload |
|---|---|---|---|---|
| `GET` | `/chat/messages` | Fetch discussion messages in channel/project | Query: `channel_id`, `limit` | `List<ChatMessageDto>` |
| `POST` | `/chat/messages` | Post a chat message | `{ channel_id, content, sender_name }` | `ChatMessageDto` |

### 2.5 Notifications & Alerts
| Method | Endpoint | Description | Request Body | Response Payload |
|---|---|---|---|---|
| `GET` | `/notifications` | Fetch user alerts & deadline notifications | None | `List<NotificationDto>` |
| `PATCH` | `/notifications/:id/read` | Mark single notification as read | `{ read: true }` | `NotificationDto` |
| `POST` | `/notifications/mark-all-read` | Mark all notifications read | None | `{ count: number }` |

### 2.6 Analytics & Reports
| Method | Endpoint | Description | Request Body | Response Payload |
|---|---|---|---|---|
| `GET` | `/reports/summary` | Aggregate metrics (completion rate, status counts) | None | `ReportSummaryDto` |

### 2.7 User Profile & Settings
| Method | Endpoint | Description | Request Body | Response Payload |
|---|---|---|---|---|
| `GET` | `/me` | Fetch authenticated user profile | None | `UserDto` |
| `PATCH` | `/me/profile` | Update profile display name & preferences | `{ display_name, avatar_url }` | `UserDto` |

---

## 3. Offline Synchronization Protocol

When the mobile device is offline:
1. Every write operation generates a `SyncQueueEntity` row in local SQLite:
   ```sql
   CREATE TABLE sync_queue (
       id TEXT PRIMARY KEY,
       entity_type TEXT NOT NULL,
       action TEXT NOT NULL,
       payload_json TEXT NOT NULL,
       created_at INTEGER NOT NULL,
       retry_count INTEGER DEFAULT 0
   );
   ```
2. `SyncWorker` (WorkManager) triggers automatically when network constraints (`NetworkType.CONNECTED`) are satisfied.
3. Queue records are dispatched in chronological order (`created_at ASC`) to prevent state inversion.
4. If a conflict arises, server timestamps dictate the winner (Last-Write-Wins), and the local Room record is reconciled.
