package com.projectflow.android.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.projectflow.android.R;
import com.projectflow.android.data.local.AppDatabase;
import com.projectflow.android.data.local.ProjectEntity;
import com.projectflow.android.data.local.WorkspaceEntity;
import com.projectflow.android.data.repository.ProjectFlowRepository;
import com.projectflow.android.ui.adapter.ProjectAdapter;

import java.util.List;

public class ProjectsFragment extends Fragment {

    private ProjectFlowRepository repository;
    private ProjectAdapter adapter;
    private RecyclerView rvProjects;
    private FloatingActionButton fabAdd;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_projects, container, false);
        rvProjects = v.findViewById(R.id.rvProjects);
        fabAdd = v.findViewById(R.id.fabAddProject);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new ProjectFlowRepository(requireContext());

        adapter = new ProjectAdapter(project -> {
            Toast.makeText(requireContext(), "Selected: " + project.name, Toast.LENGTH_SHORT).show();
        });

        rvProjects.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvProjects.setAdapter(adapter);

        repository.getProjects().observe(getViewLifecycleOwner(), list -> {
            adapter.setProjects(list);
        });

        fabAdd.setOnClickListener(v -> showCreateProjectDialog());
    }

    private void showCreateProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("New Project");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText etName = new EditText(requireContext());
        etName.setHint("Project Name");
        layout.addView(etName);

        final EditText etCode = new EditText(requireContext());
        etCode.setHint("Project Code (e.g. PRJ)");
        layout.addView(etCode);

        final EditText etDesc = new EditText(requireContext());
        etDesc.setHint("Description");
        layout.addView(etDesc);

        builder.setView(layout);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String code = etCode.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            if (name.isEmpty()) return;

            // Get first workspace ID from local DB
            new Thread(() -> {
                List<WorkspaceEntity> wsList = AppDatabase.getInstance(requireContext()).workspaceDao().getAllSync();
                String wsId = (wsList != null && !wsList.isEmpty()) ? wsList.get(0).id : "00000000-0000-0000-0000-000000000001";
                repository.createProject(wsId, name, code, desc,
                        () -> requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Project Created", Toast.LENGTH_SHORT).show()),
                        () -> requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Failed creating project", Toast.LENGTH_SHORT).show())
                );
            }).start();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
