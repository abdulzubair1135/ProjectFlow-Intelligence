package com.projectflow.android.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.projectflow.android.R;
import com.projectflow.android.data.local.AppDatabase;
import com.projectflow.android.data.local.TaskEntity;

public class TaskDetailsActivity extends AppCompatActivity {

    private String taskId = "";
    private String taskProjectId = "";
    private String taskTitle = "";
    private String taskDesc = "";
    private String taskPriority = "";
    private String taskDue = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvDesc = findViewById(R.id.tvDetailDesc);
        TextView tvPriorityBadge = findViewById(R.id.tvDetailPriorityBadge);
        TextView tvDueDate = findViewById(R.id.tvDetailDueDate);
        TextView tvStatus = findViewById(R.id.tvDetailStatus);

        Button btnSetInProgress = findViewById(R.id.btnSetInProgress);
        Button btnSetDone = findViewById(R.id.btnSetDone);

        btnBack.setOnClickListener(v -> finish());

        taskId = getIntent().getStringExtra("task_id");
        taskProjectId = getIntent().getStringExtra("task_project_id");
        taskTitle = getIntent().getStringExtra("task_title");
        taskDesc = getIntent().getStringExtra("task_desc");
        taskPriority = getIntent().getStringExtra("task_priority");
        taskDue = getIntent().getStringExtra("task_due");
        if (taskDue == null) taskDue = getIntent().getStringExtra("task_due_date");
        String currentStatus = getIntent().getStringExtra("task_status");

        if (taskTitle != null && !taskTitle.isEmpty()) tvTitle.setText(taskTitle);
        if (taskDesc != null && !taskDesc.isEmpty()) tvDesc.setText(taskDesc);
        if (taskPriority != null && !taskPriority.isEmpty()) tvPriorityBadge.setText(taskPriority + " Priority");
        if (taskDue != null && !taskDue.isEmpty()) tvDueDate.setText(taskDue.split("T")[0]);
        if (currentStatus != null && !currentStatus.isEmpty()) tvStatus.setText(currentStatus);

        btnSetInProgress.setOnClickListener(v -> updateTaskStatus("IN_PROGRESS", tvStatus));
        btnSetDone.setOnClickListener(v -> updateTaskStatus("DONE", tvStatus));
    }

    private void updateTaskStatus(String newStatus, TextView tvStatus) {
        tvStatus.setText(newStatus);
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            String pId = (taskProjectId != null && !taskProjectId.isEmpty()) ? taskProjectId : "10000000-0000-0000-0000-000000000001";
            int progress = "DONE".equalsIgnoreCase(newStatus) ? 100 : 50;
            TaskEntity updated = new TaskEntity(taskId != null ? taskId : java.util.UUID.randomUUID().toString(),
                    pId, taskTitle, taskDesc, taskPriority, newStatus, progress, taskDue);
            db.taskDao().insert(updated);

            runOnUiThread(() -> Toast.makeText(this, "Task status updated to " + newStatus, Toast.LENGTH_SHORT).show());
        }).start();
    }
}
