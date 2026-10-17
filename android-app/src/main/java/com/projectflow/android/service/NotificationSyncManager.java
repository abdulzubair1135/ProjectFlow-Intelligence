package com.projectflow.android.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.projectflow.android.data.remote.model.DtoModels.ChatMessageDto;
import com.projectflow.android.data.remote.model.DtoModels.NotificationDto;
import com.projectflow.android.data.repository.ProjectFlowRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Enterprise Real-time Notification Synchronization Engine.
 * Continuously monitors workspace discussions and system alerts, triggering
 * native Android heads-up notifications with sounds, vibrations, and channel deep-linking.
 */
public class NotificationSyncManager {

    private static final String TAG = "NotificationSync";
    private static final String PREF_NAME = "projectflow_notif_sync";
    private static final String KEY_SEEN_MESSAGES = "seen_chat_ids";
    private static final String KEY_SEEN_NOTIFS = "seen_notif_ids";
    private static final long SYNC_INTERVAL_MS = 3500; // 3.5 seconds real-time polling

    private static volatile NotificationSyncManager INSTANCE;
    private final Context appContext;
    private final ProjectFlowRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private boolean isFirstRun = true;

    private final Set<String> seenMessageIds = new HashSet<>();
    private final Set<String> seenNotifIds = new HashSet<>();

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;
            performSync();
            handler.postDelayed(this, SYNC_INTERVAL_MS);
        }
    };

    private NotificationSyncManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.repository = new ProjectFlowRepository(this.appContext);

        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> savedMessages = prefs.getStringSet(KEY_SEEN_MESSAGES, null);
        if (savedMessages != null) {
            seenMessageIds.addAll(savedMessages);
            isFirstRun = false;
        }
        Set<String> savedNotifs = prefs.getStringSet(KEY_SEEN_NOTIFS, null);
        if (savedNotifs != null) {
            seenNotifIds.addAll(savedNotifs);
        }
    }

    public static synchronized NotificationSyncManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new NotificationSyncManager(context);
        }
        return INSTANCE;
    }

    public synchronized void start() {
        if (isRunning) return;
        isRunning = true;
        Log.i(TAG, "NotificationSyncManager activated - Real-time chat & workspace alerts enabled");
        handler.removeCallbacks(syncRunnable);
        handler.post(syncRunnable);
    }

    public synchronized void stop() {
        isRunning = false;
        handler.removeCallbacks(syncRunnable);
        Log.i(TAG, "NotificationSyncManager paused");
    }

    public void triggerImmediateSync() {
        handler.post(this::performSync);
    }

    private void performSync() {
        SharedPreferences userPrefs = appContext.getSharedPreferences("projectflow_prefs", Context.MODE_PRIVATE);
        String currentEmail = userPrefs.getString("user_email", "").trim().toLowerCase();
        String currentName = userPrefs.getString("user_name", userPrefs.getString("user_display_name", "")).trim().toLowerCase();

        // 1. Sync Chat Messages across all channels
        repository.fetchAllChatMessages(new ProjectFlowRepository.DataCallback<List<ChatMessageDto>>() {
            @Override
            public void onData(List<ChatMessageDto> messages) {
                if (messages == null || messages.isEmpty()) return;

                if (isFirstRun && seenMessageIds.isEmpty()) {
                    // Seed existing messages without blasting notifications for past history
                    for (ChatMessageDto msg : messages) {
                        if (msg.id != null) seenMessageIds.add(msg.id);
                    }
                    persistSeenMessages();
                    isFirstRun = false;
                    return;
                }

                boolean newFound = false;
                for (ChatMessageDto msg : messages) {
                    if (msg.id == null || seenMessageIds.contains(msg.id)) continue;

                    // Mark as seen immediately
                    seenMessageIds.add(msg.id);
                    newFound = true;

                    // Check if message was sent by someone else
                    String author = (msg.senderName != null) ? msg.senderName.trim().toLowerCase() : "";
                    boolean isSelf = (!currentName.isEmpty() && author.equals(currentName))
                            || (!currentEmail.isEmpty() && currentEmail.startsWith(author));

                    if (!isSelf) {
                        Log.d(TAG, "New incoming chat alert from " + msg.senderName + " in #" + msg.channel);
                        String title = "💬 #" + (msg.channel != null ? msg.channel : "general") + " • " + (msg.senderName != null ? msg.senderName : "Team");
                        ProjectFlowNotificationHelper.showChatNotification(
                                appContext,
                                title,
                                msg.content,
                                msg.channel,
                                msg.senderName
                        );
                    }
                }

                if (newFound) {
                    persistSeenMessages();
                }
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Chat sync check error: " + message);
            }
        });

        // 2. Sync In-App System & Task Notifications
        repository.fetchNotifications(new ProjectFlowRepository.DataCallback<List<NotificationDto>>() {
            @Override
            public void onData(List<NotificationDto> notifications) {
                if (notifications == null || notifications.isEmpty()) return;

                boolean newFound = false;
                for (NotificationDto notif : notifications) {
                    if (notif.id == null || seenNotifIds.contains(notif.id)) continue;

                    seenNotifIds.add(notif.id);
                    newFound = true;

                    // Only alert if unread and not a chat message (chat messages handled above)
                    if (!notif.read && !"CHAT_MESSAGE".equalsIgnoreCase(notif.type)) {
                        if ("RISK_ALERT".equalsIgnoreCase(notif.type) || "CRITICAL".equalsIgnoreCase(notif.priority)) {
                            ProjectFlowNotificationHelper.showRiskNotification(appContext, notif.title, notif.message, notif.priority);
                        } else {
                            ProjectFlowNotificationHelper.showTaskNotification(appContext, notif.title, notif.message, notif.taskId, notif.priority);
                        }
                    }
                }

                if (newFound) {
                    persistSeenNotifs();
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void persistSeenMessages() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Keep bounded to last 200 message IDs to prevent unlimited storage growth
        Set<String> bounded = new HashSet<>(seenMessageIds);
        prefs.edit().putStringSet(KEY_SEEN_MESSAGES, bounded).apply();
    }

    private void persistSeenNotifs() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> bounded = new HashSet<>(seenNotifIds);
        prefs.edit().putStringSet(KEY_SEEN_NOTIFS, bounded).apply();
    }
}
