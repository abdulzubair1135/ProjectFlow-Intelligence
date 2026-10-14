package com.projectflow.android.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.projectflow.android.R;
import com.projectflow.android.data.repository.ProjectFlowRepository;
import com.projectflow.android.ui.CreateTaskActivity;
import com.projectflow.android.ui.TaskDetailsActivity;
import com.projectflow.android.ui.adapter.TaskAdapter;

public class TasksFragment extends Fragment {

    private ProjectFlowRepository repository;
    private TaskAdapter adapter;
    private RecyclerView rvTasks;
    private FloatingActionButton fabAddTask;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_tasks, container, false);
        rvTasks = v.findViewById(R.id.rvTasks);
        fabAddTask = v.findViewById(R.id.fabAddTask);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ProjectFlowRepository(requireContext());

        adapter = new TaskAdapter(task -> {
            Intent intent = new Intent(requireContext(), TaskDetailsActivity.class);
            intent.putExtra("task_id", task.id);
            intent.putExtra("task_title", task.title);
            intent.putExtra("task_desc", task.description);
            intent.putExtra("task_priority", task.priority);
            intent.putExtra("task_due_date", task.dueDate);
            intent.putExtra("task_project_id", task.projectId);
            startActivity(intent);
        });

        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(adapter);

        repository.getAllTasks().observe(getViewLifecycleOwner(), list -> {
            adapter.setTasks(list);
        });

        fabAddTask.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CreateTaskActivity.class));
        });
    }
}
