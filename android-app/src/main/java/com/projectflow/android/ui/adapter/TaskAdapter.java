package com.projectflow.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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

        String priority = t.priority != null ? t.priority : "Medium";
        holder.tvPriority.setText(priority);

        String dueDate = t.dueDate != null ? t.dueDate.split("T")[0] : "28 Apr";
        holder.tvDueDate.setText(dueDate);

        holder.cbComplete.setChecked("DONE".equalsIgnoreCase(t.statusCategory));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(t);
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPriority, tvDueDate;
        CheckBox cbComplete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvPriority = itemView.findViewById(R.id.tvTaskPriority);
            tvDueDate = itemView.findViewById(R.id.tvTaskDueDate);
            cbComplete = itemView.findViewById(R.id.cbTaskComplete);
        }
    }
}
