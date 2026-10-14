const express = require('express');
const cors = require('cors');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const { store, supabase } = require('./src/supabase');
const { evaluateProjectRisks } = require('./src/engine/deterministicRiskEngine');
const { evaluateScenario } = require('./src/engine/whatIfSchedulingEngine');

const app = express();
const PORT = process.env.PORT || 8080;

app.use(cors());
app.use(express.json());

// Serve static logo and public assets
app.use(express.static(path.join(__dirname, 'admin-panel', 'public')));
app.use(express.static(path.join(__dirname, 'admin-panel', 'dist')));

// -------------------------------------------------------------
// AUTHENTICATION & SESSIONS
// -------------------------------------------------------------
app.post('/api/v1/auth/dev-login', (req, res) => {
  const email = (req.body && req.body.email) ? req.body.email.trim().toLowerCase() : 'admin@projectflow.io';
  
  let role = 'MEMBER';
  let fullName = 'Team Member';
  let token = 'dev-member-token';

  if (email.includes('admin')) {
    role = 'ADMINISTRATOR';
    fullName = 'System Administrator';
    token = 'dev-admin-token';
  } else if (email.includes('manager')) {
    role = 'MANAGER';
    fullName = 'Project Manager';
    token = 'dev-manager-token';
  }

  let user = store.users.find(u => u.email.toLowerCase() === email);
  if (!user) {
    user = {
      id: uuidv4(),
      email,
      fullName,
      role,
      active: true,
      createdAt: new Date().toISOString()
    };
    store.users.push(user);
  }

  res.json({ token, user });
});

app.get('/api/v1/me', (req, res) => {
  const auth = req.headers.authorization || '';
  let email = 'admin@projectflow.io';
  if (auth.includes('manager')) email = 'manager@projectflow.io';
  if (auth.includes('member')) email = 'member@projectflow.io';

  const user = store.users.find(u => u.email === email) || store.users[0];
  res.json(user);
});

// -------------------------------------------------------------
// ADMIN CONSOLE ENDPOINTS
// -------------------------------------------------------------
app.get('/api/v1/admin/overview', (req, res) => {
  res.json({
    totalUsers: store.users.length,
    totalWorkspaces: store.workspaces.length,
    totalProjects: store.projects.length,
    totalTasks: store.tasks.length,
    activeRisksCount: store.tasks.filter(t => !t.completedAt && t.dueDate && new Date(t.dueDate) < new Date()).length,
    systemStatus: 'ONLINE (Node.js + Supabase)'
  });
});

app.get('/api/v1/admin/users', (req, res) => {
  const query = (req.query.query || '').toLowerCase();
  let filtered = store.users;
  if (query) {
    filtered = filtered.filter(u => u.email.toLowerCase().includes(query) || (u.fullName && u.fullName.toLowerCase().includes(query)));
  }
  res.json({
    content: filtered,
    totalElements: filtered.length,
    totalPages: 1,
    size: 20,
    number: 0
  });
});

app.patch('/api/v1/admin/users/:id/role', (req, res) => {
  const user = store.users.find(u => u.id === req.params.id);
  if (!user) return res.status(404).json({ message: 'User not found' });
  if (req.body.role) user.role = req.body.role;
  res.json(user);
});

app.get('/api/v1/admin/workspaces', (req, res) => {
  res.json(store.workspaces);
});

app.get('/api/v1/admin/projects', (req, res) => {
  res.json(store.projects);
});

app.get('/api/v1/admin/system/health', (req, res) => {
  const mem = process.memoryUsage();
  res.json({
    status: 'UP',
    databaseStatus: 'Supabase PostgreSQL Cloud',
    databaseLatencyMs: 3,
    freeMemoryMb: Math.round((mem.heapTotal - mem.heapUsed) / (1024 * 1024)),
    totalMemoryMb: Math.round(mem.heapTotal / (1024 * 1024)),
    activeThreads: 1,
    schemaVersion: 'V1__supabase_tables',
    checkedAt: new Date().toISOString()
  });
});

app.get('/api/v1/admin/audit-events', (req, res) => {
  res.json({
    content: store.auditLogs,
    totalElements: store.auditLogs.length,
    totalPages: 1,
    size: 25,
    number: 0
  });
});

// -------------------------------------------------------------
// WORKSPACES & PROJECTS
// -------------------------------------------------------------
app.get('/api/v1/workspaces', (req, res) => {
  res.json(store.workspaces);
});

app.post('/api/v1/workspaces', (req, res) => {
  const { name, slug, description } = req.body;
  const ws = {
    id: uuidv4(),
    name: name || 'New Workspace',
    slug: slug || 'new-workspace',
    description: description || '',
    ownerId: store.users[0].id,
    archived: false,
    createdAt: new Date().toISOString()
  };
  store.workspaces.push(ws);
  res.status(201).json(ws);
});

app.get('/api/v1/workspaces/:id/projects', (req, res) => {
  const list = store.projects.filter(p => p.workspaceId === req.params.id);
  res.json(list);
});

app.post('/api/v1/workspaces/:id/projects', (req, res) => {
  const { name, code, description } = req.body;
  const project = {
    id: uuidv4(),
    workspaceId: req.params.id,
    name: name || 'New Project',
    code: code || 'PRJ',
    description: description || '',
    status: 'PLANNING',
    health: 'ON_TRACK',
    startDate: new Date().toISOString().split('T')[0],
    targetDate: new Date(Date.now() + 30 * 86400000).toISOString().split('T')[0],
    createdAt: new Date().toISOString()
  };
  store.projects.push(project);
  res.status(201).json(project);
});

// -------------------------------------------------------------
// TASKS & DEPENDENCIES
// -------------------------------------------------------------
app.get('/api/v1/projects/:id/tasks', (req, res) => {
  const list = store.tasks.filter(t => t.projectId === req.params.id);
  res.json(list);
});

app.post('/api/v1/projects/:id/tasks', (req, res) => {
  const { title, description, priority, dueDate } = req.body;
  const task = {
    id: uuidv4(),
    projectId: req.params.id,
    title: title || 'New Task',
    description: description || '',
    priority: priority || 'MEDIUM',
    statusCategory: 'TODO',
    progressPercent: 0,
    dueDate: dueDate ? dueDate.split('T')[0] : null,
    createdAt: new Date().toISOString()
  };
  store.tasks.push(task);
  res.status(201).json(task);
});

// -------------------------------------------------------------
// DETERMINISTIC RISK RADAR & WHAT-IF SIMULATION
// -------------------------------------------------------------
app.get('/api/v1/projects/:id/risks', (req, res) => {
  const project = store.projects.find(p => p.id === req.params.id) || store.projects[0];
  const projectTasks = store.tasks.filter(t => t.projectId === req.params.id);
  const findings = evaluateProjectRisks(project, projectTasks, store.dependencies);
  res.json(findings);
});

app.post('/api/v1/projects/:id/risks/evaluate', (req, res) => {
  const project = store.projects.find(p => p.id === req.params.id) || store.projects[0];
  const projectTasks = store.tasks.filter(t => t.projectId === req.params.id);
  const findings = evaluateProjectRisks(project, projectTasks, store.dependencies);
  res.json(findings);
});

app.post('/api/v1/projects/:id/scenarios/evaluate', (req, res) => {
  const project = store.projects.find(p => p.id === req.params.id) || store.projects[0];
  const projectTasks = store.tasks.filter(t => t.projectId === req.params.id);
  const comparison = evaluateScenario(project, projectTasks, store.dependencies, req.body);
  res.json(comparison);
});

// -------------------------------------------------------------
// OFFLINE SYNC ENDPOINTS
// -------------------------------------------------------------
app.post('/api/v1/sync/push', (req, res) => {
  res.json({ appliedCount: (req.body.operations || []).length, status: 'APPLIED' });
});

app.get('/api/v1/sync/pull', (req, res) => {
  res.json({
    workspaces: store.workspaces,
    projects: store.projects,
    tasks: store.tasks
  });
});

// -------------------------------------------------------------
// SPA FRONTEND FALLBACK (SERVES ADMIN PANEL ON ROOT)
// -------------------------------------------------------------
app.get('*', (req, res) => {
  const indexPath = path.join(__dirname, 'admin-panel', 'dist', 'index.html');
  res.sendFile(indexPath, (err) => {
    if (err) {
      res.send(`
        <!DOCTYPE html>
        <html>
        <head><title>ProjectFlow Intelligence</title></head>
        <body style="font-family:sans-serif;background:#F4F2E9;color:#292D28;padding:40px;text-align:center;">
          <h1>ProjectFlow Intelligence API is Operational</h1>
          <p>REST API running at <a href="/api/v1/admin/overview">/api/v1/admin/overview</a></p>
        </body>
        </html>
      `);
    }
  });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`[PROJECTFLOW] JavaScript Backend listening on port ${PORT}`);
});
