import { AdminOverview, AppRelease, AuditLog, ErrorLogEntry, PageResponse, Project, RiskFinding, SystemHealth, Task, Team, UserProfile, Workspace } from '../types';

const API_BASE = import.meta.env.VITE_API_URL || '/api/v1';

function getAuthHeaders(): HeadersInit {
  const token = localStorage.getItem('projectflow_token') || 'dev-admin-token';
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    if (res.status === 401) {
      // Unauthenticated
      console.warn('API returned 401 Unauthorized');
    }
    const errBody = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(errBody.message || `API error ${res.status}`);
  }
  if (res.status === 204) {
    return {} as T;
  }
  return res.json();
}

export const api = {
  // Auth
  async devLogin(email: string) {
    const res = await fetch(`${API_BASE}/auth/dev-login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    });
    return handleResponse<{ token: string; user: UserProfile }>(res);
  },

  async getCurrentUser() {
    const res = await fetch(`${API_BASE}/me`, {
      headers: getAuthHeaders()
    });
    return handleResponse<UserProfile>(res);
  },

  // Admin Overview & Health
  async getAdminOverview() {
    const res = await fetch(`${API_BASE}/admin/overview`, {
      headers: getAuthHeaders()
    });
    return handleResponse<AdminOverview>(res);
  },

  async getSystemHealth() {
    const res = await fetch(`${API_BASE}/admin/system/health`, {
      headers: getAuthHeaders()
    });
    return handleResponse<SystemHealth>(res);
  },

  async getAuditEvents(page = 0, size = 25) {
    const res = await fetch(`${API_BASE}/admin/audit-events?page=${page}&size=${size}`, {
      headers: getAuthHeaders()
    });
    return handleResponse<PageResponse<AuditLog>>(res);
  },

  // Users
  async getUsers(query = '', page = 0, size = 20) {
    const q = query ? `&query=${encodeURIComponent(query)}` : '';
    const res = await fetch(`${API_BASE}/admin/users?page=${page}&size=${size}${q}`, {
      headers: getAuthHeaders()
    });
    return handleResponse<PageResponse<UserProfile>>(res);
  },

  async updateUserRole(userId: string, role: string) {
    const res = await fetch(`${API_BASE}/admin/users/${userId}/role`, {
      method: 'PATCH',
      headers: getAuthHeaders(),
      body: JSON.stringify({ role })
    });
    return handleResponse<UserProfile>(res);
  },

  // Workspaces
  async getWorkspaces() {
    const res = await fetch(`${API_BASE}/admin/workspaces`, {
      headers: getAuthHeaders()
    });
    return handleResponse<Workspace[]>(res);
  },

  async createWorkspace(data: { name: string; slug: string; description?: string }) {
    const res = await fetch(`${API_BASE}/workspaces`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse<Workspace>(res);
  },

  // Projects
  async getProjects() {
    const res = await fetch(`${API_BASE}/admin/projects`, {
      headers: getAuthHeaders()
    });
    return handleResponse<Project[]>(res);
  },

  async getWorkspaceProjects(workspaceId: string) {
    const res = await fetch(`${API_BASE}/workspaces/${workspaceId}/projects`, {
      headers: getAuthHeaders()
    });
    return handleResponse<Project[]>(res);
  },

  async createProject(workspaceId: string, data: { name: string; code: string; description?: string }) {
    const res = await fetch(`${API_BASE}/workspaces/${workspaceId}/projects`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse<Project>(res);
  },

  // Tasks
  async getProjectTasks(projectId: string) {
    const res = await fetch(`${API_BASE}/projects/${projectId}/tasks`, {
      headers: getAuthHeaders()
    });
    return handleResponse<Task[]>(res);
  },

  async createTask(projectId: string, data: {
    title: string;
    description?: string;
    priority?: string;
    dueDate?: string;
    assigneeId?: string;
    assigneeName?: string;
    teamId?: string;
    teamName?: string;
  }) {
    const res = await fetch(`${API_BASE}/projects/${projectId}/tasks`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse<Task>(res);
  },

  // Teams & Squads
  async getTeams() {
    const res = await fetch(`${API_BASE}/teams`, {
      headers: getAuthHeaders()
    });
    return handleResponse<Team[]>(res);
  },

  async createTeam(data: { name: string; description?: string }) {
    const res = await fetch(`${API_BASE}/teams`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse<Team>(res);
  },

  // Risks
  async getProjectRisks(projectId: string) {
    const res = await fetch(`${API_BASE}/projects/${projectId}/risks`, {
      headers: getAuthHeaders()
    });
    return handleResponse<RiskFinding[]>(res);
  },

  async evaluateRisks(projectId: string) {
    const res = await fetch(`${API_BASE}/projects/${projectId}/risks/evaluate`, {
      method: 'POST',
      headers: getAuthHeaders()
    });
    return handleResponse<RiskFinding[]>(res);
  },

  // App Releases & APK Distribution
  async getReleases() {
    const res = await fetch(`${API_BASE}/admin/releases`, {
      headers: getAuthHeaders()
    });
    return handleResponse<AppRelease[]>(res);
  },

  async createRelease(data: Partial<AppRelease>) {
    const res = await fetch(`${API_BASE}/admin/releases`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse<AppRelease>(res);
  },

  async publishRelease(id: string) {
    const res = await fetch(`${API_BASE}/admin/releases/${id}/publish`, {
      method: 'POST',
      headers: getAuthHeaders()
    });
    return handleResponse<AppRelease>(res);
  },

  async unpublishRelease(id: string) {
    const res = await fetch(`${API_BASE}/admin/releases/${id}/unpublish`, {
      method: 'POST',
      headers: getAuthHeaders()
    });
    return handleResponse<AppRelease>(res);
  },

  getDownloadUrl(id: string) {
    return `${API_BASE}/releases/${id}/download`;
  },

  // Error Telemetry & Diagnostics
  async getSystemErrors(severity?: string, status?: string) {
    let query = '';
    const params: string[] = [];
    if (severity) params.push(`severity=${encodeURIComponent(severity)}`);
    if (status) params.push(`status=${encodeURIComponent(status)}`);
    if (params.length > 0) query = `?${params.join('&')}`;

    const res = await fetch(`${API_BASE}/admin/system/errors${query}`, {
      headers: getAuthHeaders()
    });
    return handleResponse<PageResponse<ErrorLogEntry>>(res);
  },

  async reportError(data: { severity?: string; message: string; stack?: string; source?: string; metadata?: any }) {
    const res = await fetch(`${API_BASE}/telemetry/errors`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse<ErrorLogEntry>(res);
  },

  async resolveError(id: string) {
    const res = await fetch(`${API_BASE}/admin/system/errors/${id}/resolve`, {
      method: 'PATCH',
      headers: getAuthHeaders()
    });
    return handleResponse<ErrorLogEntry>(res);
  }
};
