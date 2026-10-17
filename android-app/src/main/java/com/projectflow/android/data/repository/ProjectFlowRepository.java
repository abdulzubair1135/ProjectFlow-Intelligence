package com.projectflow.android.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.projectflow.android.data.local.*;
import com.projectflow.android.data.remote.ApiClient;
import com.projectflow.android.data.remote.ApiService;
import com.projectflow.android.data.remote.model.DtoModels.*;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProjectFlowRepository {
    private final AppDatabase db;
    private final ApiService api;
    private final ExecutorService executor;

    public ProjectFlowRepository(Context context) {
        this.db = AppDatabase.getInstance(context);
        this.api = ApiClient.getService(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    // Workspaces
    public LiveData<List<WorkspaceEntity>> getWorkspaces() {
        refreshWorkspaces();
        return db.workspaceDao().getAllLive();
    }

    public void refreshWorkspaces() {
        executor.execute(() -> {
            try {
                Response<List<WorkspaceDto>> resp = api.getWorkspaces().execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    List<WorkspaceEntity> entities = new ArrayList<>();
                    for (WorkspaceDto dto : resp.body()) {
                        entities.add(new WorkspaceEntity(dto.id, dto.name, dto.slug, dto.description, dto.createdAt));
                    }
                    db.workspaceDao().insertAll(entities);
                }
            } catch (Exception e) {
                // Offline fallback - Room database already has the cached records
            }
        });
    }

    public void createWorkspace(String name, String slug, String description, Runnable onSuccess, Runnable onError) {
        executor.execute(() -> {
            try {
                Map<String, String> body = new HashMap<>();
                body.put("name", name);
                body.put("slug", slug);
                body.put("description", description != null ? description : "");
                Response<WorkspaceDto> resp = api.createWorkspace(body).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    WorkspaceDto dto = resp.body();
                    db.workspaceDao().insert(new WorkspaceEntity(dto.id, dto.name, dto.slug, dto.description, dto.createdAt));
                    if (onSuccess != null) onSuccess.run();
                    return;
                }
            } catch (Exception e) {
                // Save offline
                String id = UUID.randomUUID().toString();
                db.workspaceDao().insert(new WorkspaceEntity(id, name, slug, description, new Date().toString()));
                db.syncQueueDao().insert(new SyncQueueEntity(UUID.randomUUID().toString(), "CREATE", "WORKSPACE", id, "{}", "PENDING", new Date().toString()));
                if (onSuccess != null) onSuccess.run();
                return;
            }
            if (onError != null) onError.run();
        });
    }

    // Projects
    public LiveData<List<ProjectEntity>> getProjects() {
        refreshProjects();
        return db.projectDao().getAllLive();
    }

    public void refreshProjects() {
        executor.execute(() -> {
            try {
                List<WorkspaceEntity> wsList = db.workspaceDao().getAllSync();
                for (WorkspaceEntity ws : wsList) {
                    Response<List<ProjectDto>> resp = api.getProjects(ws.id).execute();
                    if (resp.isSuccessful() && resp.body() != null) {
                        List<ProjectEntity> entities = new ArrayList<>();
                        for (ProjectDto dto : resp.body()) {
                            entities.add(new ProjectEntity(dto.id, dto.workspaceId, dto.name, dto.code, dto.description, dto.status, dto.health, dto.targetDate));
                        }
                        db.projectDao().insertAll(entities);
                    }
                }
            } catch (Exception e) {
                // Offline mode
            }
        });
    }

    public void createProject(String workspaceId, String name, String code, String description, Runnable onSuccess, Runnable onError) {
        executor.execute(() -> {
            try {
                Map<String, String> body = new HashMap<>();
                body.put("name", name);
                body.put("code", code);
                body.put("description", description != null ? description : "");
                Response<ProjectDto> resp = api.createProject(workspaceId, body).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    ProjectDto dto = resp.body();
                    db.projectDao().insert(new ProjectEntity(dto.id, dto.workspaceId, dto.name, dto.code, dto.description, dto.status, dto.health, dto.targetDate));
                    if (onSuccess != null) onSuccess.run();
                    return;
                }
            } catch (Exception e) {
                String id = UUID.randomUUID().toString();
                db.projectDao().insert(new ProjectEntity(id, workspaceId, name, code, description, "PLANNING", "ON_TRACK", null));
                db.syncQueueDao().insert(new SyncQueueEntity(UUID.randomUUID().toString(), "CREATE", "PROJECT", id, "{}", "PENDING", new Date().toString()));
                if (onSuccess != null) onSuccess.run();
                return;
            }
            if (onError != null) onError.run();
        });
    }

    // Tasks
    public LiveData<List<TaskEntity>> getAllTasks() {
        refreshAllTasks();
        return db.taskDao().getAllLive();
    }

    public void refreshAllTasks() {
        executor.execute(() -> {
            try {
                Response<List<TaskDto>> resp = api.getAllTasks().execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    List<TaskEntity> entities = new ArrayList<>();
                    for (TaskDto dto : resp.body()) {
                        entities.add(new TaskEntity(dto.id, dto.projectId, dto.title, dto.description, dto.priority, dto.statusCategory, dto.progressPercent, dto.dueDate));
                    }
                    db.taskDao().insertAll(entities);
                }
            } catch (Exception e) {
                // Offline fallback
            }
        });
    }

    public LiveData<List<TaskEntity>> getTasks(String projectId) {
        refreshTasks(projectId);
        return db.taskDao().getByProjectLive(projectId);
    }

    public void refreshTasks(String projectId) {
        if (projectId == null) return;
        executor.execute(() -> {
            try {
                Response<List<TaskDto>> resp = api.getTasks(projectId).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    List<TaskEntity> entities = new ArrayList<>();
                    for (TaskDto dto : resp.body()) {
                        entities.add(new TaskEntity(dto.id, dto.projectId, dto.title, dto.description, dto.priority, dto.statusCategory, dto.progressPercent, dto.dueDate));
                    }
                    db.taskDao().insertAll(entities);
                }
            } catch (Exception e) {
                // Offline
            }
        });
    }

    public void createTask(String projectId, String title, String description, String priority, String dueDate, Runnable onSuccess, Runnable onError) {
        executor.execute(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("title", title);
                body.put("description", description);
                body.put("priority", priority);
                if (dueDate != null && !dueDate.isEmpty()) {
                    body.put("dueDate", dueDate);
                }
                Response<TaskDto> resp = api.createTask(projectId, body).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    TaskDto dto = resp.body();
                    db.taskDao().insert(new TaskEntity(dto.id, dto.projectId, dto.title, dto.description, dto.priority, dto.statusCategory, dto.progressPercent, dto.dueDate));
                    if (onSuccess != null) onSuccess.run();
                    return;
                }
            } catch (Exception e) {
                String id = UUID.randomUUID().toString();
                db.taskDao().insert(new TaskEntity(id, projectId, title, description, priority, "TODO", 0, dueDate));
                db.syncQueueDao().insert(new SyncQueueEntity(UUID.randomUUID().toString(), "CREATE", "TASK", id, "{}", "PENDING", new Date().toString()));
                if (onSuccess != null) onSuccess.run();
                return;
            }
            if (onError != null) onError.run();
        });
    }

    public void createTask(String projectId, String title, String description, String priority, Runnable onSuccess, Runnable onError) {
        createTask(projectId, title, description, priority, null, onSuccess, onError);
    }

    public void updateTaskStatus(String taskId, String statusCategory, int progressPercent, Runnable onDone) {
        executor.execute(() -> {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("statusCategory", statusCategory);
                body.put("progressPercent", progressPercent);
                api.updateTaskStatus(taskId, body).execute();
            } catch (Exception ignored) {}
            if (onDone != null) onDone.run();
        });
    }

    // Risks
    public LiveData<List<RiskEntity>> getRisks(String projectId) {
        evaluateRisks(projectId);
        return db.riskDao().getByProjectLive(projectId);
    }

    public void evaluateRisks(String projectId) {
        if (projectId == null) return;
        executor.execute(() -> {
            try {
                Response<List<RiskDto>> resp = api.evaluateRisks(projectId).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    List<RiskEntity> entities = new ArrayList<>();
                    for (RiskDto dto : resp.body()) {
                        String rId = dto.id != null ? dto.id : UUID.randomUUID().toString();
                        entities.add(new RiskEntity(rId, projectId, dto.ruleId, dto.severity, dto.title, dto.description, dto.recommendedAction, dto.detectedAt));
                    }
                    db.riskDao().deleteByProject(projectId);
                    db.riskDao().insertAll(entities);
                }
            } catch (Exception e) {
                // Offline
            }
        });
    }

    public LiveData<Integer> getPendingSyncCount() {
        return db.syncQueueDao().getPendingCountLive();
    }

    public interface DataCallback<T> {
        void onData(T data);
        void onError(String message);
    }

    public void fetchTeam(DataCallback<List<TeamMemberDto>> callback) {
        executor.execute(() -> {
            try {
                Response<List<TeamMemberDto>> resp = api.getTeam().execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    if (callback != null) callback.onData(resp.body());
                } else {
                    if (callback != null) callback.onError("Failed to load team");
                }
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void fetchNotifications(DataCallback<List<NotificationDto>> callback) {
        executor.execute(() -> {
            try {
                Response<List<NotificationDto>> resp = api.getNotifications().execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    if (callback != null) callback.onData(resp.body());
                } else {
                    if (callback != null) callback.onError("Failed to load notifications");
                }
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void markNotificationRead(String id, Runnable onDone) {
        executor.execute(() -> {
            try {
                api.markNotificationRead(id).execute();
            } catch (Exception ignored) {}
            if (onDone != null) onDone.run();
        });
    }

    public void markAllNotificationsRead(Runnable onDone) {
        executor.execute(() -> {
            try {
                api.markAllNotificationsRead().execute();
            } catch (Exception ignored) {}
            if (onDone != null) onDone.run();
        });
    }

    public void fetchChatChannels(DataCallback<List<String>> callback) {
        executor.execute(() -> {
            try {
                Response<List<String>> resp = api.getChatChannels().execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    if (callback != null) callback.onData(resp.body());
                } else {
                    if (callback != null) callback.onError("Failed to load channels");
                }
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void fetchAllChatMessages(DataCallback<List<ChatMessageDto>> callback) {
        executor.execute(() -> {
            try {
                Response<List<ChatMessageDto>> resp = api.getChatMessages().execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    if (callback != null) callback.onData(resp.body());
                } else {
                    if (callback != null) callback.onError("Failed to load chat messages");
                }
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void fetchChatMessages(DataCallback<List<ChatMessageDto>> callback) {
        fetchChatMessagesByChannel("general", callback);
    }

    public void fetchChatMessagesByChannel(String channel, DataCallback<List<ChatMessageDto>> callback) {
        executor.execute(() -> {
            try {
                Response<List<ChatMessageDto>> resp = api.getChatMessagesByChannel(channel).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    if (callback != null) callback.onData(resp.body());
                } else {
                    if (callback != null) callback.onError("Failed to load chat messages");
                }
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void sendChatMessage(String content, String senderName, DataCallback<ChatMessageDto> callback) {
        sendChatMessage(content, senderName, "general", callback);
    }

    public void sendChatMessage(String content, String senderName, String channel, DataCallback<ChatMessageDto> callback) {
        executor.execute(() -> {
            try {
                Map<String, String> body = new HashMap<>();
                body.put("content", content);
                body.put("senderName", senderName != null ? senderName : "You");
                body.put("channel", channel != null ? channel : "general");
                Response<ChatMessageDto> resp = api.sendChatMessage(body).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    if (callback != null) callback.onData(resp.body());
                } else {
                    if (callback != null) callback.onError("Failed sending message");
                }
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void reactToMessage(String messageId, String emoji, DataCallback<ChatMessageDto> callback) {
        executor.execute(() -> {
            try {
                Map<String, String> body = new HashMap<>();
                body.put("emoji", emoji != null ? emoji : "👍");
                Response<ChatMessageDto> resp = api.reactToMessage(messageId, body).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    if (callback != null) callback.onData(resp.body());
                } else {
                    if (callback != null) callback.onError("Failed to react to message");
                }
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void fetchTeams(DataCallback<List<TeamGroupDto>> callback) {
        executor.execute(() -> {
            try {
                Response<List<TeamGroupDto>> resp = api.getTeams().execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    if (callback != null) callback.onData(resp.body());
                } else {
                    if (callback != null) callback.onError("Failed to load teams");
                }
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void fetchReportSummary(DataCallback<ReportSummaryDto> callback) {
        executor.execute(() -> {
            try {
                Response<ReportSummaryDto> resp = api.getReportSummary().execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    if (callback != null) callback.onData(resp.body());
                } else {
                    if (callback != null) callback.onError("Failed to fetch report summary");
                }
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void updateProfile(String fullName, DataCallback<UserDto> callback) {
        executor.execute(() -> {
            try {
                Map<String, String> body = new HashMap<>();
                body.put("fullName", fullName);
                Response<UserDto> resp = api.updateProfile(body).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    if (callback != null) callback.onData(resp.body());
                } else {
                    if (callback != null) callback.onError("Failed to update profile");
                }
            } catch (Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }
}
