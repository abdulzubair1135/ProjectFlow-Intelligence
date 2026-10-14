package com.projectflow.android.ui;

import android.os.Bundle;
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

        ImageView btnBack = findViewById(R.id.btnBack);
        EditText etTitle = findViewById(R.id.etTaskTitle);
        EditText etDesc = findViewById(R.id.etTaskDesc);
        EditText etDueDate = findViewById(R.id.etTaskDueDate);
        RadioGroup rgPriority = findViewById(R.id.rgPriority);
        Button btnSubmit = findViewById(R.id.btnSubmitCreateTask);

        btnBack.setOnClickListener(v -> finish());

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
                String projectId = (projects != null && !projects.isEmpty()) ? projects.get(0).id : "00000000-0000-0000-0000-000000000001";

                repository.createTask(projectId, title, desc, finalPriority,
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
