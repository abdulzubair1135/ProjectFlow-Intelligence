package com.projectflow.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        holder.tvCode.setText(p.code != null ? p.code : "PRJ");
        holder.tvName.setText(p.name);
        holder.tvDesc.setText(p.description != null ? p.description : "No description provided.");
        holder.tvStatus.setText(p.status != null ? p.status : "PLANNING");
        holder.tvHealth.setText(p.health != null ? p.health : "ON TRACK");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProjectClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvName, tvDesc, tvStatus, tvHealth;

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvProjectCode);
            tvName = itemView.findViewById(R.id.tvProjectName);
            tvDesc = itemView.findViewById(R.id.tvProjectDesc);
            tvStatus = itemView.findViewById(R.id.tvProjectStatus);
            tvHealth = itemView.findViewById(R.id.tvProjectHealth);
        }
    }
}
