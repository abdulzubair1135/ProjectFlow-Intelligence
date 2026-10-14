# ProjectFlow Intelligence — Known Limitations & Future Roadmap

**Document Version:** 2.0.0  
**Scope:** Production Constraints, Environmental Prerequisites, and Version 2.1 Enhancements

---

## 1. Environmental & Configuration Prerequisites

1. **Remote Cloud Backend Connectivity:**
   - The Android client defaults to `http://10.0.2.2:3000/api/v1/` for emulators and `http://192.168.x.x:3000/api/v1/` for LAN device testing.
   - For real-world production deployment on Render, update `ApiClient.java` with the production domain (`https://projectflow-api.onrender.com/api/v1/`) and inject valid Supabase credentials into the backend environment.
   - If the backend server is temporarily uncontactable, the application seamlessly activates offline-mode with local Room caching and queues sync mutations in `sync_queue`.

2. **Google Play Services Dependency (Nearby Share):**
   - The native ShareSheet (`ShareApkActivity`) utilizes standard Android `Intent.ACTION_SEND` and `FileProvider`. Sharing via Nearby Share / Quick Share relies on Google Play Services being present on the device. All standard fallback transports (WhatsApp, Telegram, Bluetooth, Drive, Gmail) operate without Play Services.

---

## 2. Technical Limitations

1. **Push Notifications (FCM):**
   - The current notification system supports in-app polling and local notifications via Room and WorkManager. Remote push notifications via Firebase Cloud Messaging (FCM) require a registered `google-services.json` file linked to an active Firebase project.
2. **WebSocket Real-Time Chat Subscriptions:**
   - Chat messaging currently executes over authenticated REST endpoints with auto-refresh intervals. Full duplex WebSocket subscription (`wss://`) is architected in `ChatFragment` and can be enabled once WebSocket gateways are provisioned on Render.

---

## 3. Recommended Future Roadmap (v2.1.0)

1. **Biometric Authentication:**
   - Integrate `androidx.biometric:biometric` for fingerprint and face unlock on supported Android devices.
2. **Kanban Drag-and-Drop:**
   - Implement `ItemTouchHelper` dragging for board view task transitions between Todo, In Progress, and Done columns.
3. **Multi-File Attachment Support:**
   - Extend task comments and chat messages to support Supabase Storage document and image attachments.
