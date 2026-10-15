package com.projectflow.android.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.projectflow.android.R;
import com.projectflow.android.ui.MainActivity;
import com.projectflow.android.ui.TaskDetailsActivity;

/**
 * Enterprise Notification Management Engine for ProjectFlow Intelligence.
 * Handles Android 8.0 - 15+ Notification Channels, Heads-up Banners,
 * Sound & Vibration profiles, Task Deep-linking, and Quick Actions.
 */
public class ProjectFlowNotificationHelper {

    public static final String CHANNEL_TASKS = "projectflow_tasks_channel";
    public static final String CHANNEL_RISKS = "projectflow_risks_channel";
    public static final String CHANNEL_GENERAL = "projectflow_general_channel";

    /**
     * Initializes all enterprise notification channels on app startup.
     */
    public static void initChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            // 1. Task Assignments & Status Channel
            NotificationChannel taskChannel = new NotificationChannel(
                    CHANNEL_TASKS,
                    "Task Assignments & Delegations",
                    NotificationManager.IMPORTANCE_HIGH
            );
            taskChannel.setDescription("Real-time notifications when tasks are assigned, updated, or completed.");
            taskChannel.enableLights(true);
            taskChannel.setLightColor(Color.parseColor("#47745A")); // Forest Green
            taskChannel.enableVibration(true);
            taskChannel.setVibrationPattern(new long[]{0, 250, 150, 250});
            nm.createNotificationChannel(taskChannel);

            // 2. Schedule Risk & Deadline Alerts Channel
            NotificationChannel riskChannel = new NotificationChannel(
                    CHANNEL_RISKS,
                    "Project Risks & Delay Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            riskChannel.setDescription("Urgent alerts triggered by the deterministic schedule risk engine.");
            riskChannel.enableLights(true);
            riskChannel.setLightColor(Color.parseColor("#B7473C")); // Rust Red
            riskChannel.enableVibration(true);
            riskChannel.setVibrationPattern(new long[]{0, 350, 200, 350});
            nm.createNotificationChannel(riskChannel);

            // 3. General Workspace Channel
            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_GENERAL,
                    "Workspace & Team Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("Team announcements and general collaboration updates.");
            nm.createNotificationChannel(generalChannel);
        }
    }

    /**
     * Displays an actionable task assignment or update notification in the system tray.
     */
    public static void showTaskNotification(Context context, String title, String message, String taskId, String priority) {
        initChannels(context);

        Intent intent;
        if (taskId != null && !taskId.isEmpty()) {
            intent = new Intent(context, TaskDetailsActivity.class);
            intent.putExtra("task_id", taskId);
            intent.putExtra("task_title", title);
            intent.putExtra("task_desc", message);
            intent.putExtra("task_priority", priority != null ? priority : "MEDIUM");
        } else {
            intent = new Intent(context, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_TASKS)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSound(defaultSound)
                .setColor(Color.parseColor("#47745A"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent);

        // Add Quick Action: View Task
        if (taskId != null && !taskId.isEmpty()) {
            builder.addAction(R.drawable.ic_tasks, "View Task", contentPendingIntent);
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            int notifId = (taskId != null) ? taskId.hashCode() : (int) System.currentTimeMillis();
            nm.notify(notifId, builder.build());
        }
    }

    /**
     * Displays an urgent schedule risk or critical delay notification.
     */
    public static void showRiskNotification(Context context, String title, String riskDetail, String severity) {
        initChannels(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_RISKS)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(riskDetail)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle("⚠️ " + title)
                        .bigText(riskDetail))
                .setSound(defaultSound)
                .setColor(Color.parseColor("#B7473C"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
