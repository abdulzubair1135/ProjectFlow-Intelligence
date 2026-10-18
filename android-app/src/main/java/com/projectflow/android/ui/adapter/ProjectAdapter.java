package com.projectflow.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.projectflow.android.R;
import com.projectflow.android.data.local.ProjectEntity;

import java.util.ArrayList;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<ProjectEntity> projects = new ArrayList<>();
    private final OnProjectClickListener listener;

    public interface OnProjectClickListener {
        void onProjectClick(ProjectEntity project);
    }

    public ProjectAdapter(OnProjectClickListener listener) {
        this.listener = listener;
    }

    public void setProjects(List<ProjectEntity> list) {
        this.projects = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        ProjectEntity p = projects.get(position);
        holder.tvName.setText(p.name);
        String status = p.status != null ? p.status : "Active";
        holder.tvMeta.setText(status + " • " + (p.code != null ? p.code : "PRJ"));

        if (holder.tvAssignee != null) {
            if (p.assigneeName != null && !p.assigneeName.isEmpty()) {
                holder.tvAssignee.setText("👤 Assigned: " + p.assigneeName);
                holder.tvAssignee.setVisibility(View.VISIBLE);
            } else if (p.assigneeEmail != null && !p.assigneeEmail.isEmpty()) {
                holder.tvAssignee.setText("👤 Assigned: " + p.assigneeEmail);
                holder.tvAssignee.setVisibility(View.VISIBLE);
            } else if (p.teamName != null && !p.teamName.isEmpty()) {
                holder.tvAssignee.setText("👥 Squad: " + p.teamName);
                holder.tvAssignee.setVisibility(View.VISIBLE);
            } else {
                holder.tvAssignee.setText("👤 Unassigned");
                holder.tvAssignee.setVisibility(View.VISIBLE);
            }
        }

        int progress = 65;
        if (p.name.contains("Mobile")) progress = 75;
        else if (p.name.contains("Website")) progress = 40;
        else if (p.name.contains("Dashboard") || p.name.contains("Client")) progress = 20;

        holder.tvProgress.setText(progress + "%");
        holder.pbProgress.setProgress(progress);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProjectClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMeta, tvAssignee, tvProgress;
        ProgressBar pbProgress;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProjectName);
            tvMeta = itemView.findViewById(R.id.tvProjectMeta);
            tvAssignee = itemView.findViewById(R.id.tvProjectAssignee);
            tvProgress = itemView.findViewById(R.id.tvProgressPercent);
            pbProgress = itemView.findViewById(R.id.pbProjectProgress);
        }
    }
}
