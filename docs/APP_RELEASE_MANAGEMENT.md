# ProjectFlow Intelligence — Application Release Management

## 1. Overview
The **Application Release Management** subsystem gives administrators complete operational control over distributed Android client versions. It bridges database records, static binary artifact storage, cryptographic verification, and the Web Admin Control Panel.

---

## 2. Release Lifecycle States

Every application build progresses through standard lifecycle phases:

```
  +---------+      Publish Action       +-------------+
  |  DRAFT  | ------------------------> |  PUBLISHED  |
  +---------+                           +-------------+
       |                                       |
       |                                       v
       | Retract / Deprecate            +-------------+
       +------------------------------> | DEPRECATED  |
                                        +-------------+
                                               |
                                               v
                                        +-------------+
                                        |  ARCHIVED   |
                                        +-------------+
```

- **DRAFT:** Initial upload state. The binary is registered on the server with its metadata and checksum, but client apps will not receive it over the air.
- **PUBLISHED:** Active distribution state. Any Android client calling `GET /api/v1/releases/latest` will be directed to this version.
- **DEPRECATED:** Older version that should no longer be distributed, but remains available for audit or historical telemetry.
- **ARCHIVED:** Inactive, legacy release.

---

## 3. Database Specification (`app_releases`)

The schema is defined in `database/migrations/V2__app_releases.sql`:

| Column | Type | Description |
|---|---|---|
| `id` | UUID | Unique identifier (Primary Key) |
| `version_name` | VARCHAR(64) | Semantic version string (e.g., `2.0.0`) |
| `version_code` | INT | Monotonically increasing build integer (e.g., `2`) |
| `release_notes` | TEXT | Human-readable changelog and release notes |
| `channel` | VARCHAR(32) | `ALPHA`, `BETA`, or `STABLE` |
| `artifact_url` | VARCHAR(512) | Filesystem path or object URL to the binary |
| `artifact_size` | BIGINT | Size in bytes |
| `sha256_checksum` | VARCHAR(64) | Cryptographic SHA-256 hex string |
| `min_android_sdk` | INT | Minimum supported Android SDK (API 26+) |
| `target_android_sdk`| INT | Target Android SDK (API 34) |
| `status` | VARCHAR(32) | `DRAFT`, `PUBLISHED`, `ARCHIVED`, `DEPRECATED` |
| `published_at` | TIMESTAMPTZ | Timestamp when published |
| `created_at` | TIMESTAMPTZ | Timestamp when registered |

---

## 4. REST API Endpoints

### Public / Client Endpoints:
- `GET /api/v1/releases/latest`: Returns the highest active `PUBLISHED` release DTO.
- `GET /api/v1/releases/{id}/download`: Streams the raw `.apk` file with headers:
  - `Content-Type: application/vnd.android.package-archive`
  - `Content-Disposition: attachment; filename="ProjectFlow-Intelligence-v{versionName}.apk"`
  - `Content-Length: {size}`
  - `ETag: "{sha256}"`

### Admin Management Endpoints:
- `GET /api/v1/admin/releases`: Lists all registered releases sorted by `version_code DESC`.
- `POST /api/v1/admin/releases`: Registers a new release payload.
- `POST /api/v1/admin/releases/{id}/publish`: Promotes release to `PUBLISHED` and timestamps it.
- `POST /api/v1/admin/releases/{id}/unpublish`: Demotes release back to `DRAFT`.

---

## 5. Web Admin Panel Interface
Located at `/releases` in the ProjectFlow Admin Console:
1. **Live Metric Cards:** Highlights active channel, artifact size, SHA-256 signature, and supported SDK range.
2. **Release Catalog Table:** Displays version details, channel badges, SHA-256 copy helpers, publication status toggles, and direct APK download buttons.
3. **Register Release Modal:** Allows administrators to register newly signed build artifacts directly into the cluster.
