package com.projectflow.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.projectflow.android.R;
import com.projectflow.android.data.local.AppDatabase;
import com.projectflow.android.data.repository.ProjectFlowRepository;
import com.projectflow.android.ui.adapter.RiskAdapter;
import com.projectflow.android.ui.adapter.TaskAdapter;

public class ProjectDetailsActivity extends AppCompatActivity {

    private TextView tvProjectCode, tvProjectStatus, tvProjectName, tvProjectDesc, tvTargetDate, tvProgressPercent;
    private ProgressBar pbProjectProgress;
    private RecyclerView rvProjectTasks, rvProjectRisks;
    private TextView tvEmptyTasks, tvEmptyRisks, btnAddTask;
    private TaskAdapter taskAdapter;
    private RiskAdapter riskAdapter;
    private ProjectFlowRepository repository;
    private String projectId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtils.applyLightStatusBar(this);
        setContentView(R.layout.activity_project_details);

        repository = new ProjectFlowRepository(this);

        tvProjectCode = findViewById(R.id.tvProjectCode);
        tvProjectStatus = findViewById(R.id.tvProjectStatus);
        tvProjectName = findViewById(R.id.tvProjectName);
        tvProjectDesc = findViewById(R.id.tvProjectDesc);
        tvTargetDate = findViewById(R.id.tvTargetDate);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        pbProjectProgress = findViewById(R.id.pbProjectProgress);

        rvProjectTasks = findViewById(R.id.rvProjectTasks);
        rvProjectRisks = findViewById(R.id.rvProjectRisks);
        tvEmptyTasks = findViewById(R.id.tvEmptyTasks);
        tvEmptyRisks = findViewById(R.id.tvEmptyRisks);
        btnAddTask = findViewById(R.id.btnAddTask);

        projectId = getIntent().getStringExtra("project_id");
        if (projectId == null) projectId = "10000000-0000-0000-0000-000000000001";

        String name = getIntent().getStringExtra("project_name");
        String code = getIntent().getStringExtra("project_code");
        String desc = getIntent().getStringExtra("project_desc");
        String target = getIntent().getStringExtra("project_target");
        String status = getIntent().getStringExtra("project_status");

        tvProjectName.setText(name != null ? name : "ProjectFlow Core Platform");
        tvProjectCode.setText(code != null ? code : "PRJ");
        tvProjectDesc.setText(desc != null ? desc : "Full-stack project management and intelligence engine.");
        tvTargetDate.setText("Target: " + (target != null ? target : "Ongoing"));
        tvProjectStatus.setText(status != null ? status : "IN_PROGRESS");

        taskAdapter = new TaskAdapter(task -> {
            Intent intent = new Intent(ProjectDetailsActivity.this, TaskDetailsActivity.class);
            intent.putExtra("task_id", task.id);
            intent.putExtra("task_title", task.title);
            intent.putExtra("task_desc", task.description);
            intent.putExtra("task_priority", task.priority);
            intent.putExtra("task_status", task.statusCategory);
            intent.putExtra("task_due", task.dueDate);
            intent.putExtra("task_progress", task.progressPercent);
            intent.putExtra("task_project_id", task.projectId);
            startActivity(intent);
        });
        rvProjectTasks.setLayoutManager(new LinearLayoutManager(this));
        rvProjectTasks.setAdapter(taskAdapter);

        riskAdapter = new RiskAdapter();
        rvProjectRisks.setLayoutManager(new LinearLayoutManager(this));
        rvProjectRisks.setAdapter(riskAdapter);

        btnAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(ProjectDetailsActivity.this, CreateTaskActivity.class);
            intent.putExtra("project_id", projectId);
            startActivity(intent);
        });

        // Observe Tasks
        AppDatabase db = AppDatabase.getInstance(this);
        db.taskDao().getByProjectLive(projectId).observe(this, tasks -> {
            if (tasks != null && !tasks.isEmpty()) {
                tvEmptyTasks.setVisibility(View.GONE);
                rvProjectTasks.setVisibility(View.VISIBLE);
                taskAdapter.setTasks(tasks);

                int done = 0;
                for (var t : tasks) if ("DONE".equalsIgnoreCase(t.statusCategory)) done++;
                int prog = Math.round((done * 100f) / tasks.size());
                pbProjectProgress.setProgress(prog);
                tvProgressPercent.setText(prog + "% Complete");
            } else {
                rvProjectTasks.setVisibility(View.GONE);
                tvEmptyTasks.setVisibility(View.VISIBLE);
            }
        });

        // Observe Risks
        repository.getRisks(projectId).observe(this, risks -> {
            if (risks != null && !risks.isEmpty()) {
                tvEmptyRisks.setVisibility(View.GONE);
                rvProjectRisks.setVisibility(View.VISIBLE);
                riskAdapter.setRisks(risks);
            } else {
                rvProjectRisks.setVisibility(View.GONE);
                tvEmptyRisks.setVisibility(View.VISIBLE);
            }
        });
    }
}
