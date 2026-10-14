package com.projectflow.android.data.remote;

import com.projectflow.android.data.remote.model.DtoModels.*;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface ApiService {

    @POST("auth/dev-login")
    Call<AuthResponse> devLogin(@Body Map<String, String> body);

    @GET("workspaces")
    Call<List<WorkspaceDto>> getWorkspaces();

    @POST("workspaces")
    Call<WorkspaceDto> createWorkspace(@Body Map<String, String> body);

    @GET("workspaces/{id}/projects")
    Call<List<ProjectDto>> getProjects(@Path("id") String workspaceId);

    @POST("workspaces/{id}/projects")
    Call<ProjectDto> createProject(@Path("id") String workspaceId, @Body Map<String, String> body);

    @GET("projects/{id}/tasks")
    Call<List<TaskDto>> getTasks(@Path("id") String projectId);

    @POST("projects/{id}/tasks")
    Call<TaskDto> createTask(@Path("id") String projectId, @Body Map<String, Object> body);

    @POST("projects/{id}/risks/evaluate")
    Call<List<RiskDto>> evaluateRisks(@Path("id") String projectId);

    @GET("releases/latest")
    Call<ReleaseDto> getLatestRelease(@Query("applicationId") String applicationId);

    @Streaming
    @GET("releases/{id}/download")
    Call<okhttp3.ResponseBody> downloadRelease(@Path("id") String releaseId);
}
