package com.projectflow.android.ui;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.projectflow.android.R;

public class TaskDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvDesc = findViewById(R.id.tvDetailDesc);
        TextView tvPriorityBadge = findViewById(R.id.tvDetailPriorityBadge);
        TextView tvDueDate = findViewById(R.id.tvDetailDueDate);

        btnBack.setOnClickListener(v -> finish());

        String title = getIntent().getStringExtra("task_title");
        String desc = getIntent().getStringExtra("task_desc");
        String priority = getIntent().getStringExtra("task_priority");
        String dueDate = getIntent().getStringExtra("task_due_date");

        if (title != null && !title.isEmpty()) tvTitle.setText(title);
        if (desc != null && !desc.isEmpty()) tvDesc.setText(desc);
        if (priority != null && !priority.isEmpty()) tvPriorityBadge.setText(priority + " Priority");
        if (dueDate != null && !dueDate.isEmpty()) tvDueDate.setText(dueDate.split("T")[0]);
    }
}
