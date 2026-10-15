package com.projectflow.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.projectflow.android.R;
import com.projectflow.android.data.local.AppDatabase;
import com.projectflow.android.data.local.ProjectEntity;
import com.projectflow.android.data.repository.ProjectFlowRepository;

import java.util.List;

public class CreateTaskActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        View btnBack = findViewById(R.id.btnBack);
        EditText etTitle = findViewById(R.id.etTaskTitle);
        EditText etDesc = findViewById(R.id.etTaskDesc);
        EditText etDueDate = findViewById(R.id.etTaskDueDate);
        RadioGroup rgPriority = findViewById(R.id.rgPriority);
        View btnSubmit = findViewById(R.id.btnSubmitCreateTask);

        btnBack.setOnClickListener(v -> finish());

        // Check if pre-filled date or project passed from calendar/project screen
        String prefilledDate = getIntent().getStringExtra("default_due_date");
        if (prefilledDate != null && !prefilledDate.isEmpty()) {
            etDueDate.setText(prefilledDate);
        }

        String intentProjectId = getIntent().getStringExtra("project_id");

        etDueDate.setFocusable(false);
        etDueDate.setClickable(true);
        etDueDate.setOnClickListener(v -> {
            java.util.Calendar c = java.util.Calendar.getInstance();
            int year = c.get(java.util.Calendar.YEAR);
            int month = c.get(java.util.Calendar.MONTH);
            int day = c.get(java.util.Calendar.DAY_OF_MONTH);

            new android.app.DatePickerDialog(this, (view, y, m, d) -> {
                String formatted = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                etDueDate.setText(formatted);
            }, year, month, day).show();
        });

        ProjectFlowRepository repository = new ProjectFlowRepository(this);

        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            if (title.isEmpty()) {
                etTitle.setError("Task title is required");
                etTitle.requestFocus();
                return;
            }

            int selectedPriorityId = rgPriority.getCheckedRadioButtonId();
            String priority = "MEDIUM";
            if (selectedPriorityId == R.id.rbLow) priority = "LOW";
            else if (selectedPriorityId == R.id.rbHigh) priority = "HIGH";

            final String finalPriority = priority;
            new Thread(() -> {
                List<ProjectEntity> projects = AppDatabase.getInstance(this).projectDao().getAllSync();
                String projectId = (intentProjectId != null && !intentProjectId.isEmpty()) ? intentProjectId :
                        ((projects != null && !projects.isEmpty()) ? projects.get(0).id : "10000000-0000-0000-0000-000000000001");

                String dueDate = etDueDate.getText() != null ? etDueDate.getText().toString().trim() : null;
                repository.createTask(projectId, title, desc, finalPriority, dueDate,
                        () -> runOnUiThread(() -> {
                            Toast.makeText(this, "Task Created Successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }),
                        () -> runOnUiThread(() -> Toast.makeText(this, "Failed creating task", Toast.LENGTH_SHORT).show())
                );
            }).start();
        });
    }
}
