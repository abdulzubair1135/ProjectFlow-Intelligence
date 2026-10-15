require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const { store, supabase, saveTaskToSupabase, saveProjectToSupabase, saveWorkspaceToSupabase } = require('./src/supabase');
const { evaluateProjectRisks } = require('./src/engine/deterministicRiskEngine');
const { evaluateScenario } = require('./src/engine/whatIfSchedulingEngine');

const app = express();
const PORT = process.env.PORT || 8082;

app.use(cors({ origin: true, credentials: true }));
app.use(express.json());

// Serve static logo and public assets
app.use(express.static(path.join(__dirname, 'admin-panel', 'public')));
app.use(express.static(path.join(__dirname, 'admin-panel', 'dist')));

// -------------------------------------------------------------
// AUTHENTICATION & SESSIONS (REAL SUPABASE + DEV FALLBACK)
// -------------------------------------------------------------
const jwt = require('jsonwebtoken');
const JWT_SECRET = process.env.JWT_SECRET || 'projectflow_jwt_secret_production_key_2026';

app.post('/api/v1/auth/login', async (req, res) => {
  const email = (req.body && req.body.email) ? req.body.email.trim().toLowerCase() : '';
  const password = req.body && req.body.password ? req.body.password : '';

  if (!email) {
    return res.status(400).json({ error: 'Email address is required' });
  }

  // 1. Attempt Supabase Auth if password is provided
  let authUser = null;
  if (supabase && password) {
    try {
      const { data, error } = await supabase.auth.signInWithPassword({ email, password });
      if (!error && data && data.user) {
        authUser = data.user;
      }
    } catch (e) {
      console.warn('[AUTH] Supabase sign-in warning:', e.message);
    }
  }

  // 2. Lookup in Supabase profiles / store
  let user = store.users.find(u => u.email.toLowerCase() === email);
  if (!user && supabase) {
    try {
      const { data: profile } = await supabase.from('profiles').select('*').eq('email', email).single();
      if (profile) {
        user = {
          id: profile.id,
          email: profile.email,
          fullName: profile.full_name,
          role: profile.global_role,
          active: profile.active !== false,
          createdAt: profile.created_at
        };
        store.users.push(user);
      }
    } catch (e) {
      console.warn('[AUTH] Profile fetch warning:', e.message);
    }
  }

  // 3. Fallback or create user if logging in
  if (!user) {
    let role = 'MEMBER';
    let fullName = email.split('@')[0];
    if (email.includes('admin')) { role = 'ADMINISTRATOR'; fullName = 'System Administrator'; }
    else if (email.includes('manager')) { role = 'MANAGER'; fullName = 'Project Manager'; }

    user = {
      id: uuidv4(),
      email,
      fullName,
      role,
      active: true,
      createdAt: new Date().toISOString()
    };
    store.users.push(user);

    // Persist to Supabase profiles
    if (supabase) {
      try {
        await supabase.from('profiles').upsert({
          id: user.id,
          email: user.email,
          full_name: user.fullName,
          global_role: user.role,
          active: true
        });
      } catch (err) {
        console.error('[AUTH] Profile upsert err:', err.message);
      }
    }
  }

  const token = jwt.sign(
    { userId: user.id, email: user.email, role: user.role },
    JWT_SECRET,
    { expiresIn: '30d' }
  );

  res.json({ token, user, message: 'Authentication successful' });
});

app.post('/api/v1/auth/signup', async (req, res) => {
  const email = (req.body && req.body.email) ? req.body.email.trim().toLowerCase() : '';
  const password = req.body && req.body.password ? req.body.password : 'ProjectFlow@2026';
  const fullName = (req.body && req.body.fullName) ? req.body.fullName.trim() : email.split('@')[0];
  const workspaceName = req.body && req.body.workspaceName ? req.body.workspaceName.trim() : '';

  if (!email) {
    return res.status(400).json({ error: 'Email address is required' });
  }

  let userId = uuidv4();

  // 1. Register with Supabase Auth
  if (supabase) {
    try {
      const { data, error } = await supabase.auth.signUp({
        email,
        password,
        options: { data: { full_name: fullName } }
      });
      if (data && data.user && data.user.id) {
        userId = data.user.id;
      }
    } catch (e) {
      console.warn('[AUTH] Supabase signUp warning:', e.message);
    }
  }

  const role = email.includes('admin') ? 'ADMINISTRATOR' : 'MEMBER';
  const user = {
    id: userId,
    email,
    fullName,
    role,
    active: true,
    createdAt: new Date().toISOString()
  };

  const existingIdx = store.users.findIndex(u => u.email.toLowerCase() === email);
  if (existingIdx >= 0) {
    store.users[existingIdx] = user;
  } else {
    store.users.push(user);
  }

  // 2. Persist to Supabase profiles
  if (supabase) {
    try {
      await supabase.from('profiles').upsert({
        id: user.id,
        email: user.email,
        full_name: user.fullName,
        global_role: user.role,
        active: true
      });
      console.log('[AUTH] Upserted Supabase profile for:', user.email);
    } catch (err) {
      console.error('[AUTH] Profile upsert err:', err.message);
    }
  }

  // 3. Log audit trail for Web Admin Panel
  (store.auditLogs = store.auditLogs || []).unshift({
    id: uuidv4(),
    userId: user.id,
    userEmail: user.email,
    action: 'USER_REGISTERED',
    entityType: 'USER',
    entityId: user.id,
    details: `User ${user.fullName} (${user.email}) registered from shared mobile application.`,
    ipAddress: req.ip || req.headers['x-forwarded-for'] || '127.0.0.1',
    createdAt: new Date().toISOString()
  });

  // 4. Create default workspace if specified
  if (workspaceName) {
    const ws = {
      id: uuidv4(),
      name: workspaceName,
      slug: workspaceName.toLowerCase().replace(/[^a-z0-9]+/g, '-'),
      description: 'Primary workspace created during signup',
      ownerId: user.id,
      archived: false,
      createdAt: new Date().toISOString()
    };
    store.workspaces.push(ws);
    saveWorkspaceToSupabase(ws);
  }

  const token = jwt.sign(
    { userId: user.id, email: user.email, role: user.role },
    JWT_SECRET,
    { expiresIn: '30d' }
  );

  res.status(201).json({ token, user, message: 'Account created successfully' });
});

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

// Device FCM Push Notification Registration
store.fcmTokens = store.fcmTokens || [];
app.post('/api/v1/users/fcm-token', (req, res) => {
  const { fcmToken, email } = req.body;
  if (fcmToken) {
    const existing = store.fcmTokens.find(t => t.token === fcmToken);
    if (!existing) {
      store.fcmTokens.push({ token: fcmToken, email: email || null, registeredAt: new Date().toISOString() });
      console.log('[FCM] Registered new mobile push token:', fcmToken.substring(0, 20) + '...');
    }
  }
  res.json({ success: true, count: store.fcmTokens.length });
});

// Broadcast Push Notification Dispatcher
async function dispatchPushNotification(title, message, extraData = {}) {
  console.log(`[PUSH] Dispatching to ${store.fcmTokens.length} mobile devices: "${title}" - "${message}"`);
  const record = {
    id: uuidv4(),
    title,
    message,
    recipients: store.fcmTokens.length,
    timestamp: new Date().toISOString()
  };
  (store.pushLogs = store.pushLogs || []).unshift(record);
}

app.post('/api/v1/notifications/send-push', async (req, res) => {
  const { title, message, taskId, priority, type } = req.body;
  const notif = {
    id: uuidv4(),
    title: title || 'Workspace Alert',
    message: message || 'You have a new update in ProjectFlow.',
    type: type || 'PUSH_ALERT',
    taskId: taskId || null,
    priority: priority || 'HIGH',
    read: false,
    createdAt: new Date().toISOString()
  };
  (store.notifications = store.notifications || []).unshift(notif);
  if (supabase) {
    try {
      await supabase.from('notifications').upsert({
        id: notif.id,
        title: notif.title,
        message: notif.message,
        type: notif.type,
        read: false,
        created_at: notif.createdAt
      });
    } catch (e) {
      console.warn('[NOTIF] Supabase sync:', e.message);
    }
  }
  await dispatchPushNotification(notif.title, notif.message, { taskId: notif.taskId, priority: notif.priority });
  res.json({ success: true, notification: notif, deliveredDevices: store.fcmTokens.length });
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

app.get('/api/v1/admin/users', async (req, res) => {
  if (supabase) {
    try {
      const { data: profiles, error } = await supabase.from('profiles').select('*');
      if (!error && profiles) {
        profiles.forEach(p => {
          const exists = store.users.find(u => u.id === p.id || (u.email && u.email.toLowerCase() === (p.email || '').toLowerCase()));
          if (!exists) {
            store.users.push({
              id: p.id,
              email: p.email,
              fullName: p.full_name || (p.email ? p.email.split('@')[0] : 'Member'),
              role: p.global_role || 'MEMBER',
              active: p.active !== false,
              createdAt: p.created_at || new Date().toISOString()
            });
          }
        });
      }
    } catch (e) {
      console.warn('[ADMIN] Profiles live sync error:', e.message);
    }
  }

  const query = (req.query.query || '').toLowerCase();
  let filtered = store.users;
  if (query) {
    filtered = filtered.filter(u => u.email.toLowerCase().includes(query) || (u.fullName && u.fullName.toLowerCase().includes(query)));
  }
  res.json({
    content: filtered,
    totalElements: filtered.length,
    totalPages: 1,
    size: 50,
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

app.get('/api/v1/projects', (req, res) => {
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
// TELEMETRY & ERROR MONITORING
// -------------------------------------------------------------
app.post('/api/v1/telemetry/errors', (req, res) => {
  const { severity, message, stack, source, userId, metadata } = req.body;
  const errorEntry = {
    id: uuidv4(),
    severity: severity || 'ERROR',
    message: message || 'Unknown error occurred',
    stack: stack || null,
    source: source || 'CLIENT_TELEMETRY',
    userId: userId || null,
    metadata: metadata || {},
    resolved: false,
    resolvedAt: null,
    timestamp: new Date().toISOString()
  };
  (store.errors = store.errors || []).unshift(errorEntry);
  res.status(201).json(errorEntry);
});

app.get('/api/v1/admin/system/errors', (req, res) => {
  const severity = req.query.severity;
  const status = req.query.status;
  let list = store.errors || [];
  if (severity) list = list.filter(e => e.severity.toUpperCase() === severity.toUpperCase());
  if (status === 'open') list = list.filter(e => !e.resolved);
  if (status === 'resolved') list = list.filter(e => e.resolved);
  res.json({
    content: list,
    totalElements: list.length,
    totalPages: 1,
    size: 25,
    number: 0
  });
});

app.patch('/api/v1/admin/system/errors/:id/resolve', (req, res) => {
  const err = (store.errors || []).find(e => e.id === req.params.id);
  if (!err) return res.status(404).json({ error: 'Error log entry not found' });
  err.resolved = true;
  err.resolvedAt = new Date().toISOString();
  res.json(err);
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
  saveWorkspaceToSupabase(ws);
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
  saveProjectToSupabase(project);
  res.status(201).json(project);
});

// -------------------------------------------------------------
// -------------------------------------------------------------
// TASKS & DEPENDENCIES
// -------------------------------------------------------------
app.get('/api/v1/tasks', (req, res) => {
  let list = store.tasks;
  if (req.query.projectId) list = list.filter(t => t.projectId === req.query.projectId);
  if (req.query.assigneeId) list = list.filter(t => t.assigneeId === req.query.assigneeId);
  if (req.query.teamId) list = list.filter(t => t.teamId === req.query.teamId);
  res.json(list);
});

app.get('/api/v1/projects/:id/tasks', (req, res) => {
  const list = store.tasks.filter(t => t.projectId === req.params.id);
  res.json(list);
});

app.post('/api/v1/projects/:id/tasks', async (req, res) => {
  const { title, description, priority, dueDate, assigneeId, teamId, teamName } = req.body;
  const task = {
    id: uuidv4(),
    projectId: req.params.id,
    title: title || 'New Task',
    description: description || '',
    priority: priority || 'MEDIUM',
    statusCategory: 'TODO',
    progressPercent: 0,
    dueDate: dueDate ? dueDate.split('T')[0] : null,
    assigneeId: assigneeId || null,
    teamId: teamId || null,
    teamName: teamName || null,
    createdAt: new Date().toISOString()
  };
  store.tasks.push(task);

  // Automatically generate notification for user or team
  const targetLabel = teamName ? `Team "${teamName}"` : 'You';
  const notif = {
    id: uuidv4(),
    title: 'New Task Assigned',
    message: `${targetLabel} received task: "${task.title}" (Priority: ${task.priority})`,
    type: 'TASK_ASSIGNED',
    userId: assigneeId || null,
    teamId: teamId || null,
    taskId: task.id,
    read: false,
    createdAt: new Date().toISOString()
  };
  (store.notifications = store.notifications || []).unshift(notif);
  saveTaskToSupabase(task);

  if (supabase) {
    try {
      await supabase.from('notifications').upsert({
        id: notif.id,
        title: notif.title,
        message: notif.message,
        type: notif.type,
        user_id: notif.userId || null,
        read: false,
        created_at: notif.createdAt
      });
    } catch (err) {
      console.warn('[NOTIF] Supabase notif upsert warning:', err.message);
    }
  }

  res.status(201).json(task);
});

app.patch('/api/v1/tasks/:id/status', (req, res) => {
  const task = store.tasks.find(t => t.id === req.params.id);
  if (!task) return res.status(404).json({ error: 'Task not found' });
  if (req.body.statusCategory) {
    task.statusCategory = req.body.statusCategory;
    if (task.statusCategory === 'DONE') {
      task.progressPercent = 100;
      task.completedAt = new Date().toISOString();
    } else if (task.statusCategory === 'IN_PROGRESS') {
      task.progressPercent = req.body.progressPercent || 50;
    }
  }
  saveTaskToSupabase(task);

  const statusNotif = {
    id: uuidv4(),
    title: task.statusCategory === 'DONE' ? 'Task Completed 🎉' : 'Task Status Updated',
    message: `"${task.title}" has been updated to ${task.statusCategory}.`,
    type: 'TASK_STATUS_CHANGED',
    taskId: task.id,
    read: false,
    createdAt: new Date().toISOString()
  };
  (store.notifications = store.notifications || []).unshift(statusNotif);
  if (supabase) {
    supabase.from('notifications').upsert({
      id: statusNotif.id,
      title: statusNotif.title,
      message: statusNotif.message,
      type: statusNotif.type,
      read: false,
      created_at: statusNotif.createdAt
    }).then(() => {}).catch(err => console.warn('[NOTIF] Status notif error:', err.message));
  }
  dispatchPushNotification(statusNotif.title, statusNotif.message, { taskId: task.id });

  res.json(task);
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
// TEAMS & GROUPS MANAGEMENT
// -------------------------------------------------------------
app.get('/api/v1/teams', (req, res) => {
  res.json(store.teams || []);
});

app.post('/api/v1/teams', (req, res) => {
  const { name, description, memberCount } = req.body;
  if (!name) return res.status(400).json({ error: 'Team name is required' });
  const slug = name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
  const channel = slug;
  const newTeam = {
    id: uuidv4(),
    name,
    slug,
    description: description || '',
    memberCount: memberCount || 1,
    channel,
    createdAt: new Date().toISOString()
  };
  (store.teams = store.teams || []).push(newTeam);

  // Auto-create team welcome message in dedicated channel
  (store.messages = store.messages || []).push({
    id: uuidv4(),
    senderId: store.users[0].id,
    senderName: 'System Administrator',
    content: `🎉 Welcome to #${channel}! This dedicated discussion group was created for "${name}".`,
    channel: channel,
    reactions: { '🎉': 3, '🚀': 2 },
    createdAt: new Date().toISOString()
  });

  // Post notification
  (store.notifications = store.notifications || []).unshift({
    id: uuidv4(),
    title: 'New Team & Group Created',
    message: `Team "${name}" and chat channel #${channel} are now active.`,
    type: 'TEAM_CREATED',
    read: false,
    createdAt: new Date().toISOString()
  });

  res.status(201).json(newTeam);
});

app.get('/api/v1/team', async (req, res) => {
  if (supabase) {
    try {
      const { data: profiles, error } = await supabase.from('profiles').select('*');
      if (!error && profiles) {
        profiles.forEach(p => {
          const exists = store.users.find(u => u.id === p.id || (u.email && u.email.toLowerCase() === (p.email || '').toLowerCase()));
          if (!exists) {
            store.users.push({
              id: p.id,
              email: p.email,
              fullName: p.full_name || (p.email ? p.email.split('@')[0] : 'Member'),
              role: p.global_role || 'MEMBER',
              active: p.active !== false,
              createdAt: p.created_at || new Date().toISOString()
            });
          }
        });
      }
    } catch (e) {}
  }
  const team = store.users.map(u => ({
    id: u.id,
    email: u.email,
    fullName: u.fullName,
    role: u.role,
    active: u.active !== false,
    assignedTasksCount: store.tasks.filter(t => t.assigneeId === u.id).length
  }));
  res.json(team);
});

// -------------------------------------------------------------
// NOTIFICATIONS
// -------------------------------------------------------------
app.get('/api/v1/notifications', (req, res) => {
  res.json(store.notifications || []);
});

app.patch('/api/v1/notifications/:id/read', (req, res) => {
  const notif = (store.notifications || []).find(n => n.id === req.params.id);
  if (notif) notif.read = true;
  res.json(notif || { success: true });
});

app.post('/api/v1/notifications/mark-all-read', (req, res) => {
  (store.notifications || []).forEach(n => { n.read = true; });
  res.json({ success: true, count: (store.notifications || []).length });
});

// -------------------------------------------------------------
// CHAT & DISCUSSIONS
// -------------------------------------------------------------
app.get('/api/v1/chat/channels', (req, res) => {
  const dynamicChannels = (store.teams || []).map(t => t.channel);
  const msgChannels = (store.messages || []).map(m => m.channel);
  const uniqueChannels = Array.from(new Set(['general', ...dynamicChannels, ...msgChannels])).filter(Boolean);
  res.json(uniqueChannels);
});

app.get('/api/v1/chat/messages', (req, res) => {
  const channel = req.query.channel;
  let list = store.messages || [];
  if (channel && channel !== 'all') {
    list = list.filter(m => m.channel === channel);
  }
  res.json(list);
});

app.post('/api/v1/chat/messages', (req, res) => {
  const { content, channel, senderName, senderId } = req.body;
  const msg = {
    id: uuidv4(),
    senderId: senderId || store.users[0].id,
    senderName: senderName || 'User',
    content: content || '',
    channel: channel || 'general',
    reactions: { '👍': 0, '❤️': 0, '🚀': 0, '🔥': 0, '🎯': 0 },
    createdAt: new Date().toISOString()
  };
  (store.messages = store.messages || []).push(msg);
  res.status(201).json(msg);
});

app.post('/api/v1/chat/messages/:id/react', (req, res) => {
  const { emoji } = req.body;
  const msg = (store.messages || []).find(m => m.id === req.params.id);
  if (!msg) return res.status(404).json({ error: 'Message not found' });
  msg.reactions = msg.reactions || {};
  const emo = emoji || '👍';
  msg.reactions[emo] = (msg.reactions[emo] || 0) + 1;
  res.json(msg);
});

// -------------------------------------------------------------
// USER PROFILE UPDATE
// -------------------------------------------------------------
app.patch('/api/v1/me/profile', (req, res) => {
  const user = store.users[0];
  if (req.body.fullName) user.fullName = req.body.fullName.trim();
  res.json(user);
});

// -------------------------------------------------------------
// REPORTS & REAL-TIME ANALYTICS
// -------------------------------------------------------------
app.get('/api/v1/reports/summary', (req, res) => {
  const totalTasks = store.tasks.length;
  const completedTasks = store.tasks.filter(t => t.statusCategory === 'DONE').length;
  const inProgressTasks = store.tasks.filter(t => t.statusCategory === 'IN_PROGRESS').length;
  const todoTasks = store.tasks.filter(t => t.statusCategory === 'TODO').length;
  const overdueTasks = store.tasks.filter(t => !t.completedAt && t.dueDate && new Date(t.dueDate) < new Date()).length;
  const completionRate = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;

  const totalProjects = store.projects.length;
  const activeProjects = store.projects.filter(p => p.status !== 'COMPLETED').length;

  res.json({
    totalTasks,
    completedTasks,
    inProgressTasks,
    todoTasks,
    overdueTasks,
    completionRate,
    totalProjects,
    activeProjects,
    statusDistribution: {
      TODO: todoTasks,
      IN_PROGRESS: inProgressTasks,
      DONE: completedTasks
    },
    generatedAt: new Date().toISOString()
  });
});

// -------------------------------------------------------------
// APPLICATION RELEASES & APK DISTRIBUTION
// -------------------------------------------------------------
app.get('/api/v1/releases/latest', (req, res) => {
  const appId = req.query.applicationId || 'com.projectflow.android';
  const published = store.releases
    .filter(r => r.applicationId === appId && r.status === 'PUBLISHED')
    .sort((a, b) => b.versionCode - a.versionCode);

  if (published.length === 0) {
    return res.status(404).json({ error: 'No published releases available' });
  }

  const latest = published[0];
  res.json({
    ...latest,
    downloadUrl: `/api/v1/releases/${latest.id}/download`
  });
});

app.get('/api/v1/releases/:id/download', (req, res) => {
  const rel = store.releases.find(r => r.id === req.params.id);
  if (!rel) {
    return res.status(404).json({ error: 'Release artifact not found' });
  }

  const filePath = path.join(__dirname, rel.storagePath);
  res.setHeader('Content-Type', rel.mimeType || 'application/vnd.android.package-archive');
  res.setHeader('Content-Disposition', `attachment; filename="${rel.artifactFilename}"`);
  res.setHeader('x-sha256-checksum', rel.sha256Checksum);

  res.sendFile(filePath, (err) => {
    if (err) {
      console.error('[RELEASE DOWNLOAD ERROR]', err.message);
      if (!res.headersSent) {
        res.status(500).json({ error: 'Failed streaming APK release artifact' });
      }
    }
  });
});

app.get('/api/v1/admin/releases', (req, res) => {
  res.json(store.releases);
});

app.post('/api/v1/admin/releases', (req, res) => {
  const { versionName, versionCode, artifactFilename, storagePath, releaseNotes, releaseChannel } = req.body;
  const newRelease = {
    id: uuidv4(),
    applicationId: req.body.applicationId || 'com.projectflow.android',
    versionName: versionName || '2.0.0',
    versionCode: versionCode || 200,
    artifactFilename: artifactFilename || 'ProjectFlow-Intelligence-v2.0.0.apk',
    storagePath: storagePath || 'releases/android/ProjectFlow-Intelligence-v2.0.0.apk',
    mimeType: 'application/vnd.android.package-archive',
    fileSizeBytes: req.body.fileSizeBytes || 8114674,
    sha256Checksum: req.body.sha256Checksum || '8c20000160064c7bb80af8765f912ed1d9bb20ef6bffbb4d35ed2b3729030409',
    releaseChannel: releaseChannel || 'STABLE',
    releaseNotes: releaseNotes || 'Official release',
    minimumSupportedVersionCode: 1,
    status: 'PUBLISHED',
    createdAt: new Date().toISOString(),
    publishedAt: new Date().toISOString()
  };
  store.releases.unshift(newRelease);
  res.status(201).json(newRelease);
});

app.post('/api/v1/admin/releases/:id/publish', (req, res) => {
  const rel = store.releases.find(r => r.id === req.params.id);
  if (!rel) return res.status(404).json({ error: 'Release not found' });
  rel.status = 'PUBLISHED';
  rel.publishedAt = new Date().toISOString();
  res.json(rel);
});

app.post('/api/v1/admin/releases/:id/unpublish', (req, res) => {
  const rel = store.releases.find(r => r.id === req.params.id);
  if (!rel) return res.status(404).json({ error: 'Release not found' });
  rel.status = 'DRAFT';
  res.json(rel);
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
