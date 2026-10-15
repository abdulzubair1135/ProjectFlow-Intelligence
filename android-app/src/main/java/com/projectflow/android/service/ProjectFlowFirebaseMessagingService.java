package com.projectflow.android.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.projectflow.android.R;
import com.projectflow.android.ui.MainActivity;
import com.projectflow.android.ui.TaskDetailsActivity;

public class ProjectFlowFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "ProjectFlowFCM";
    public static final String CHANNEL_ID = "projectflow_notifications_channel";
    public static final String CHANNEL_NAME = "ProjectFlow Alerts & Tasks";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM Token: " + token);
        // Persist token in SharedPreferences
        getSharedPreferences("projectflow_prefs", MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        String title = "ProjectFlow Intelligence";
        String messageBody = "New workspace update";
        String taskId = null;
        String priority = "MEDIUM";

        // Handle notification payload
        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                messageBody = remoteMessage.getNotification().getBody();
            }
        }

        // Handle data payload
        if (!remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("message")) {
                messageBody = remoteMessage.getData().get("message");
            }
            if (remoteMessage.getData().containsKey("taskId")) {
                taskId = remoteMessage.getData().get("taskId");
            }
            if (remoteMessage.getData().containsKey("priority")) {
                priority = remoteMessage.getData().get("priority");
            }
        }

        sendNotification(title, messageBody, taskId, priority);
    }

    private void sendNotification(String title, String messageBody, String taskId, String priority) {
        Intent intent;
        if (taskId != null && !taskId.isEmpty()) {
            intent = new Intent(this, TaskDetailsActivity.class);
            intent.putExtra("task_id", taskId);
            intent.putExtra("task_title", title);
            intent.putExtra("task_desc", messageBody);
            intent.putExtra("task_priority", priority);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Notification Channel for Android O (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Critical notifications, task delegations, and risk alerts");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
