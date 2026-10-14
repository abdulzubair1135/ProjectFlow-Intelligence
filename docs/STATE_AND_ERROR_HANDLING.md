# ProjectFlow Intelligence — State & Error Handling Architecture

**Document Version:** 2.0.0  
**Target:** Android Client (`com.projectflow.android`)  
**Scope:** Resilience, Error Boundaries, Network Drop Recovery, UI State Machine

---

## 1. UI State Management Architecture

To deliver an ultra-responsive, flicker-free user experience, all major screen views (Dashboard, Tasks, Projects, Calendar, Reports, Chat) adhere to a 4-state lifecycle model:

```
          ┌─────────────┐
          │   LOADING   │ (Shimmer / Skeleton / ProgressBar)
          └──────┬──────┘
                 │
       ┌─────────┴─────────┐
       ▼                   ▼
┌──────────────┐    ┌──────────────┐
│   SUCCESS    │    │    ERROR     │ (Network timeout, 5xx, Auth expired)
│ (Data Bound) │    └──────┬───────┘
└──────┬───────┘           │
       │                   │ User taps "Retry"
       ▼                   ▼
┌──────────────┐    ┌──────────────┐
│    EMPTY     │    │   OFFLINE    │ (Cached fallback active)
└──────────────┘    └──────────────┘
```

### State Definitions
1. **LOADING:** Displayed during initial cold start when Room cache is empty.
2. **SUCCESS:** Populated with domain entities from Room `LiveData`. If new data is arriving from network, it updates seamlessly without replacing the entire list.
3. **EMPTY:** Displayed when an entity query returns zero results (e.g., zero active tasks for today, zero search matches). Renders a context-appropriate graphic, descriptive copy, and a prominent Action button (e.g., "Create First Task").
4. **ERROR / OFFLINE:** Displayed when remote sync fails and no cache exists, or as a non-intrusive `Snackbar` warning when viewing stale offline data.

---

## 2. Network Error Categorization & Handling

All Retrofit calls execute asynchronously via `ProjectFlowRepository` with centralized error handling:

| Error Type | HTTP Code / Exception | Handling Strategy | User Experience |
|---|---|---|---|
| **Unauthorized** | `401 Unauthorized` | Clear expired session token via `SessionManager.clearSession()`. | Redirect to `LoginActivity` with toast: *"Session expired. Please log in again."* |
| **Forbidden** | `403 Forbidden` | Prevent action execution. | SnackBar: *"You do not have permission to modify this resource."* |
| **Not Found** | `404 Not Found` | Reconcile local Room database (remove orphan records). | Toast: *"Resource no longer exists on server."* |
| **Validation Error** | `422 Unprocessable` / `400 Bad Request` | Inspect JSON response `error.message`. | Highlight form input fields with helper errors. |
| **Server Failure** | `500`, `502`, `503` | Log diagnostic error stack; fall back to local Room database. | Subtle banner: *"Cloud sync unavailable. Operating in offline mode."* |
| **Network Timeout** | `SocketTimeoutException`, `UnknownHostException` | Queue mutation in `sync_queue` table; notify `SyncWorker`. | Offline badge displayed in Top App Bar; mutations saved locally. |

---

## 3. Offline Resilience & Queue State Machine

The offline transaction engine guarantees zero data loss:
1. When user triggers a mutation (Create Task, Complete Task, Send Chat Message) while disconnected:
   - A unique UUID is generated locally.
   - The entity is inserted into Room with status `PENDING_SYNC`.
   - The mutation is queued in `sync_queue`.
2. UI displays the item immediately with a subtle sync clock indicator.
3. WorkManager's `SyncWorker` periodically tests `NetworkUtils.isOnline(context)` and executes batch drains.
4. Once processed by server, the local entity status is flipped to `SYNCED`.
