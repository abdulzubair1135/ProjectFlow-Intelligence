package com.projectflow.android.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "projects")
public class ProjectEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String workspaceId;
    public String name;
    public String code;
    public String description;
    public String status;
    public String health;
    public String targetDate;

    public ProjectEntity(@NonNull String id, String workspaceId, String name, String code, String description, String status, String health, String targetDate) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.name = name;
        this.code = code;
        this.description = description;
        this.status = status;
        this.health = health;
        this.targetDate = targetDate;
    }
}
