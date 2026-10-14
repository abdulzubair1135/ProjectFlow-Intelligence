import { AdminOverview, AuditLog, PageResponse, Project, RiskFinding, SystemHealth, Task, UserProfile, Workspace } from '../types';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

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

  async createTask(projectId: string, data: { title: string; description?: string; priority?: string; dueDate?: string }) {
    const res = await fetch(`${API_BASE}/projects/${projectId}/tasks`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(data)
    });
    return handleResponse<Task>(res);
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
  }
};
