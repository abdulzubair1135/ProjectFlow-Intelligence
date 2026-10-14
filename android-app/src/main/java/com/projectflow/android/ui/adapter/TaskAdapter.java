package com.projectflow.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.projectflow.android.R;
import com.projectflow.android.data.local.TaskEntity;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<TaskEntity> tasks = new ArrayList<>();
    private final OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(TaskEntity task);
    }

    public TaskAdapter(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<TaskEntity> list) {
        this.tasks = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskEntity t = tasks.get(position);
        holder.tvTitle.setText(t.title);
        holder.tvDesc.setText(t.description != null ? t.description : "");
        holder.tvPriority.setText(t.priority != null ? t.priority : "MED");
        holder.tvProgress.setText(t.progressPercent + "%");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(t);
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvPriority, tvProgress;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvPriority = itemView.findViewById(R.id.tvTaskPriority);
            tvProgress = itemView.findViewById(R.id.tvTaskProgress);
        }
    }
}
