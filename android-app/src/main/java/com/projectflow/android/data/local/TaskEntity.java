package com.projectflow.android.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class TaskEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String projectId;
    public String title;
    public String description;
    public String priority;
    public String statusCategory;
    public int progressPercent;
    public String dueDate;
    @androidx.room.Ignore
    public TaskEntity() {}

    public TaskEntity(@NonNull String id, String projectId, String title, String description, String priority, String statusCategory, int progressPercent, String dueDate) {
        this.id = id;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.statusCategory = statusCategory;
        this.progressPercent = progressPercent;
        this.dueDate = dueDate;
    }
}
