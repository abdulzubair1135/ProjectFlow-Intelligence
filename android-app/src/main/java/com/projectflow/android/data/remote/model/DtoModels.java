package com.projectflow.android.data.remote.model;

import java.util.List;

public class DtoModels {

    public static class WorkspaceDto {
        public String id;
        public String name;
        public String slug;
        public String description;
        public String createdAt;
    }

    public static class ProjectDto {
        public String id;
        public String workspaceId;
        public String name;
        public String code;
        public String description;
        public String status;
        public String health;
        public String targetDate;
    }

    public static class TaskDto {
        public String id;
        public String projectId;
        public String title;
        public String description;
        public String priority;
        public String statusCategory;
        public int progressPercent;
        public String dueDate;
    }

    public static class RiskDto {
        public String id;
        public String ruleId;
        public String severity;
        public String title;
        public String description;
        public String recommendedAction;
        public String detectedAt;
    }

    public static class UserDto {
        public String id;
        public String email;
        public String fullName;
        public String role;
        public boolean active;
    }

    public static class AuthResponse {
        public String token;
        public UserDto user;
    }
}
