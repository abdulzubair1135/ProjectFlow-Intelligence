# ProjectFlow Intelligence — APK Distribution & Native Sharing Architecture

## 1. Overview
ProjectFlow Intelligence provides an integrated, production-ready APK distribution and sharing subsystem. It allows users directly from **Settings → Application → Share ProjectFlow Intelligence APK** to transfer the authentic, cryptographically-verified application installer to any nearby device or contact via Android's native share sheet.

Supported channels include:
- **WhatsApp, Telegram, Signal & Chat Clients**
- **Nearby Share / Quick Share**
- **Bluetooth File Transfer**
- **Gmail, Outlook & Mail Clients**
- **Local File Managers & Cloud Drives**

---

## 2. Architectural Components

```
+---------------------------------------------------------------------------------+
|                               Android Application                               |
|                                                                                 |
|  [Settings Screen (14)]                                                         |
|         |                                                                       |
|         v (tap "Share APK")                                                     |
|  [ApkDistributionManager]                                                       |
|    |                                                                            |
|    +---> 1. Query Backend (/api/v1/releases/latest)                             |
|    |        |                                                                   |
|    |        +-- [Online] -> Stream APK to cache/releases/                       |
|    |        |               Verify SHA-256 Checksum                             |
|    |        |                                                                   |
|    |        +-- [Offline / Fallback] -> Check getApplicationInfo().sourceDir    |
|    |                                    Verify standalone package integrity    |
|    |                                                                            |
|    +---> 2. Secure Content Exposure via FileProvider                            |
|    |        content://com.projectflow.android.fileprovider/app_releases/v2.0.0.apk  |
|    |                                                                            |
|    +---> 3. Intent.ACTION_SEND + Intent.FLAG_GRANT_READ_URI_PERMISSION         |
|    |                                                                            |
|    v                                                                            |
|  [Android Native Share Sheet (Intent.createChooser)]                           |
+---------------------------------------------------------------------------------+
```

### 2.1 FileProvider Configuration
Direct filesystem paths (`file://`) are strictly blocked on modern Android (API 24+) to avoid `FileUriExposedException`. ProjectFlow uses `androidx.core.content.FileProvider` configured with:

- **Authority:** `${applicationId}.fileprovider` (`com.projectflow.android.fileprovider`)
- **Exported:** `false` (inaccessible to other processes without explicit intent grant)
- **GrantUriPermissions:** `true`
- **Paths Configuration (`res/xml/file_paths.xml`):**
  ```xml
  <paths>
      <cache-path name="app_releases" path="releases/" />
      <files-path name="internal_releases" path="releases/" />
      <external-cache-path name="shared_releases" path="releases/" />
  </paths>
  ```

### 2.2 Intent Construction
The sharing intent is created strictly with:
- `Intent.ACTION_SEND`
- `Intent.EXTRA_STREAM` pointing to the secure `content://` URI
- `Intent.setType("application/vnd.android.package-archive")`
- `Intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)`
- Wrapped in `Intent.createChooser(shareIntent, "Share ProjectFlow Intelligence APK via")`

This guarantees that the receiving application receives temporary, read-only access strictly for the duration of the transfer.

---

## 3. Distribution Lifecycle & Offline Fallback

1. **Remote Release Fetching (Primary Flow):**
   - The app queries `GET /api/v1/releases/latest`.
   - If a published release exists with a higher or matching version code, the app checks its local cache (`context.getCacheDir()/releases/`).
   - If not cached or hash mismatch, the APK is streamed directly from `/api/v1/releases/{id}/download`.
   - Streaming SHA-256 calculation executes in real-time. If the checksum does not match the server signature, the corrupted file is discarded and an error is reported.

2. **Standalone APK Fallback (Offline Mode):**
   - When offline or if the backend server is unreachable, `ApkDistributionManager` inspects the installed package source directory: `context.getApplicationInfo().sourceDir`.
   - To guard against multi-APK / split-APK installations (which fail to install on other devices if shared as a single split), `PackageManager.getPackageArchiveInfo()` is invoked.
   - If verified as a valid standalone APK, it is copied safely to the release cache and shared.

---

## 4. UI Guidelines & Design Tokens
In compliance with the **Natural Mineral** design language:
- **Base Container:** `#FAFAF7` background with `#E2E0D8` stroke border and 16dp rounded corners.
- **Accents:** `#56664C` (Sage Pine) for primary actions, `#78866B` for secondary labels.
- **Dynamic Status Badges:**
  - `Ready to Share`: Emerald badge `#2E7D32` on `#E8F5E9` background.
  - `Checking Updates`: Amber badge `#F57C00` with indeterminate progress bar.
  - `Verifying Hash`: Visual progress indicator during cryptographic integrity validation.
