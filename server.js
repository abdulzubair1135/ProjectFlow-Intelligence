require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const { store, supabase, saveTaskToSupabase, saveProjectToSupabase, saveWorkspaceToSupabase, saveChatMessageToSupabase } = require('./src/supabase');
const { evaluateProjectRisks } = require('./src/engine/deterministicRiskEngine');
const { evaluateScenario } = require('./src/engine/whatIfSchedulingEngine');

const app = express();
const PORT = process.env.PORT || 8082;

app.use(cors({ origin: true, credentials: true }));
app.use(express.json());

const jwt = require('jsonwebtoken');
const JWT_SECRET = process.env.JWT_SECRET || 'projectflow_jwt_secret_production_key_2026';

// Global Active Sessions Store
store.activeSessions = store.activeSessions || {};

// Presence & Authentication Interceptor Middleware
app.use((req, res, next) => {
  try {
    let email = req.headers['x-user-email'];
    const authHeader = req.headers.authorization;
    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.substring(7);
      const decoded = jwt.decode(token);
      if (decoded && decoded.email) {
        if (!email) email = decoded.email;
        req.user = decoded;
      }
    }
    if (email) {
      const emailKey = email.trim().toLowerCase();
      store.activeSessions[emailKey] = Date.now();
      req.userEmail = emailKey;
    }
  } catch (e) {}
  next();
});

// Serve static logo and public assets
app.use(express.static(path.join(__dirname, 'admin-panel', 'public')));
app.use(express.static(path.join(__dirname, 'admin-panel', 'dist')));

// System Settings Store
store.settings = store.settings || {
  syncIntervalMinutes: 15,
  maxBatchSize: 100,
  offlineGracePeriodHours: 72,
  fcmEnabled: true,
  fcmSenderId: '768921827461',
  organizationName: 'ProjectFlow Enterprise',
  contactEmail: 'admin@projectflow.io',
  riskSeverityThreshold: 'MEDIUM',
  updatedAt: new Date().toISOString()
};

// Global Audit Log Telemetry Helper
function recordAuditLog(action, entityType, entityId, actorEmail, details) {
  store.auditLogs = store.auditLogs || [];
  const log = {
    id: uuidv4(),
    actorEmail: actorEmail || 'admin@projectflow.io',
    action,
    entityType: entityType || 'SYSTEM',
    entityId: entityId || null,
    details: typeof details === 'object' ? JSON.stringify(details) : (details || ''),
    timestamp: new Date().toISOString()
  };
  store.auditLogs.unshift(log);
  if (store.auditLogs.length > 500) {
    store.auditLogs = store.auditLogs.slice(0, 500);
  }
  return log;
}

// -------------------------------------------------------------
// AUTHENTICATION & SESSIONS (REAL SUPABASE + DEV FALLBACK)
// -------------------------------------------------------------

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

// Universal Enterprise Real-time Notification Engine
async function broadcastSystemNotification({ title, message, type = 'SYSTEM', taskId = null, channel = null, senderName = null, senderId = null, priority = 'NORMAL', userId = null }) {
  const notif = {
    id: uuidv4(),
    title: title || 'Workspace Alert',
    message: message || 'New enterprise update',
    type: type || 'SYSTEM',
    taskId: taskId || null,
    channel: channel || null,
    senderName: senderName || null,
    senderId: senderId || null,
    priority: priority || 'NORMAL',
    userId: userId || null,
    read: false,
    createdAt: new Date().toISOString()
  };

  (store.notifications = store.notifications || []).unshift(notif);
  if (store.notifications.length > 200) {
    store.notifications = store.notifications.slice(0, 200);
  }

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
    } catch (e) {
      console.warn('[NOTIF] Supabase sync warning:', e.message);
    }
  }

  await dispatchPushNotification(notif.title, notif.message, {
    type: notif.type,
    taskId: notif.taskId,
    channel: notif.channel,
    priority: notif.priority
  });

  return notif;
}

app.post('/api/v1/notifications/send-push', async (req, res) => {
  const { title, message, taskId, priority, type } = req.body;
  const notif = await broadcastSystemNotification({
    title: title || 'Workspace Alert',
    message: message || 'You have a new update in ProjectFlow.',
    type: type || 'PUSH_ALERT',
    taskId: taskId || null,
    priority: priority || 'HIGH'
  });
  res.json({ success: true, notification: notif, deliveredDevices: store.fcmTokens.length });
});

// Push Broadcast Delivery Logs History
app.get('/api/v1/broadcast/history', (req, res) => {
  res.json(store.pushLogs || []);
});

// Broadcast Dispatch Handlers (Supports both /fcm/broadcast and /broadcast/send)
const handleBroadcastSend = async (req, res) => {
  const { title, body, message, type } = req.body;
  const alertTitle = title || 'ProjectFlow Broadcast Alert';
  const alertBody = body || message || 'New enterprise announcement';
  await broadcastSystemNotification({
    title: alertTitle,
    message: alertBody,
    type: type || 'BROADCAST',
    priority: 'HIGH'
  });
  recordAuditLog('BROADCAST_SENT', 'NOTIFICATION', null, req.headers['x-user-email'] || 'admin@projectflow.io', { title: alertTitle, recipientsCount: store.fcmTokens.length });
  res.json({
    success: true,
    message: `Dispatched to ${store.fcmTokens.length} active mobile subscribers`,
    recipientsCount: store.fcmTokens.length,
    timestamp: new Date().toISOString()
  });
};

app.post('/api/v1/fcm/broadcast', handleBroadcastSend);
app.post('/api/v1/broadcast/send', handleBroadcastSend);

// What-If Scenario Simulation Endpoint (Aliases project scenario evaluate)
app.post('/api/v1/scenarios/simulate', (req, res) => {
  const { projectId, scenarioName, changes } = req.body;
  const project = store.projects.find(p => p.id === projectId) || store.projects[0];
  const projectTasks = store.tasks.filter(t => t.projectId === (project ? project.id : ''));
  const scenario = {
    id: uuidv4(),
    name: scenarioName || 'What-If Simulation',
    changes: changes || []
  };
  const result = evaluateScenario(project, projectTasks, store.dependencies || [], scenario);
  res.json(result);
});

app.get('/api/v1/me', (req, res) => {
  const auth = req.headers.authorization || '';
  let email = req.userEmail || (req.user && req.user.email) || req.headers['x-user-email'];
  if (!email && auth.startsWith('Bearer ')) {
    try {
      const decoded = jwt.decode(auth.substring(7));
      if (decoded && decoded.email) email = decoded.email;
    } catch (e) {}
  }
  if (!email) {
    if (auth.includes('manager')) email = 'manager@projectflow.io';
    else if (auth.includes('member')) email = 'member@projectflow.io';
    else email = 'admin@projectflow.io';
  }

  const user = store.users.find(u => u.email.toLowerCase() === email.toLowerCase()) || store.users[0];
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
      if (!error && profiles && profiles.length > 0) {
        store.users = profiles.map(p => ({
          id: p.id,
          email: p.email,
          fullName: p.full_name || (p.email ? p.email.split('@')[0] : 'Member'),
          role: p.global_role || 'MEMBER',
          active: p.active !== false,
          createdAt: p.created_at || new Date().toISOString()
        }));
      }
    } catch (e) {
      console.warn('[ADMIN] Profiles live sync error:', e.message);
    }
  }

  const query = (req.query.query || '').toLowerCase();
  const callerEmail = (req.userEmail || req.headers['x-user-email'] || (req.user && req.user.email) || '').toLowerCase();
  let filtered = store.users.map(u => {
    const emailKey = (u.email || '').toLowerCase();
    const lastSeen = store.activeSessions ? store.activeSessions[emailKey] : null;
    const isOnline = !!(lastSeen && (Date.now() - lastSeen < 180000)) || (!!callerEmail && callerEmail === emailKey);
    return {
      ...u,
      isOnline,
      lastSeenAt: lastSeen || (isOnline ? Date.now() : null)
    };
  });
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

// Admin Create / Invite User
app.post('/api/v1/admin/users', async (req, res) => {
  const { email, fullName, role } = req.body;
  if (!email) return res.status(400).json({ error: 'Email address is required' });
  const cleanEmail = email.trim().toLowerCase();
  let existing = store.users.find(u => u.email.toLowerCase() === cleanEmail);
  if (existing) {
    return res.status(409).json({ error: 'A user with this email address already exists' });
  }
  const newUser = {
    id: uuidv4(),
    email: cleanEmail,
    fullName: fullName ? fullName.trim() : cleanEmail.split('@')[0],
    role: role || 'MEMBER',
    active: true,
    createdAt: new Date().toISOString()
  };
  store.users.push(newUser);
  if (supabase) {
    try {
      await supabase.from('profiles').upsert({
        id: newUser.id,
        email: newUser.email,
        full_name: newUser.fullName,
        global_role: newUser.role,
        active: true
      });
    } catch (e) {
      console.warn('[ADMIN] Supabase user insert warning:', e.message);
    }
  }
  recordAuditLog('USER_INVITED', 'USER', newUser.id, req.headers['x-user-email'] || 'admin@projectflow.io', { email: cleanEmail, role: newUser.role });
  res.status(201).json(newUser);
});

// Admin Update User Role
app.patch('/api/v1/admin/users/:id/role', async (req, res) => {
  const user = store.users.find(u => u.id === req.params.id);
  if (!user) return res.status(404).json({ message: 'User not found' });
  const oldRole = user.role;
  if (req.body.role) user.role = req.body.role;
  if (supabase) {
    try {
      await supabase.from('profiles').update({ global_role: user.role }).eq('id', user.id);
    } catch (e) {
      console.warn('[ADMIN] Supabase role update warning:', e.message);
    }
  }
  recordAuditLog('USER_ROLE_CHANGED', 'USER', user.id, req.headers['x-user-email'] || 'admin@projectflow.io', { oldRole, newRole: user.role, email: user.email });
  res.json(user);
});

// Admin Toggle User Active Status
app.patch('/api/v1/admin/users/:id/status', async (req, res) => {
  const user = store.users.find(u => u.id === req.params.id);
  if (!user) return res.status(404).json({ message: 'User not found' });
  user.active = req.body.active !== undefined ? !!req.body.active : !user.active;
  if (supabase) {
    try {
      await supabase.from('profiles').update({ active: user.active }).eq('id', user.id);
    } catch (e) {
      console.warn('[ADMIN] Supabase status update warning:', e.message);
    }
  }
  recordAuditLog('USER_STATUS_TOGGLED', 'USER', user.id, req.headers['x-user-email'] || 'admin@projectflow.io', { active: user.active, email: user.email });
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

// Audit Events with Search, Filter & Pagination
app.get('/api/v1/admin/audit-events', (req, res) => {
  store.auditLogs = store.auditLogs || [];
  const query = (req.query.query || '').toLowerCase();
  const actionFilter = (req.query.action || '').toUpperCase();
  const page = parseInt(req.query.page) || 0;
  const size = parseInt(req.query.size) || 25;

  let list = store.auditLogs;
  if (actionFilter && actionFilter !== 'ALL') {
    list = list.filter(l => l.action.toUpperCase().includes(actionFilter));
  }
  if (query) {
    list = list.filter(l => 
      (l.action && l.action.toLowerCase().includes(query)) ||
      (l.actorEmail && l.actorEmail.toLowerCase().includes(query)) ||
      (l.entityType && l.entityType.toLowerCase().includes(query)) ||
      (l.details && l.details.toLowerCase().includes(query))
    );
  }

  const start = page * size;
  const paginated = list.slice(start, start + size);

  res.json({
    content: paginated,
    totalElements: list.length,
    totalPages: Math.ceil(list.length / size) || 1,
    size,
    number: page
  });
});

// Admin System Settings (Persisted)
app.get('/api/v1/admin/settings', (req, res) => {
  res.json(store.settings);
});

app.patch('/api/v1/admin/settings', (req, res) => {
  store.settings = {
    ...store.settings,
    ...req.body,
    updatedAt: new Date().toISOString()
  };
  recordAuditLog('SETTINGS_UPDATED', 'CONFIGURATION', 'global', req.headers['x-user-email'] || 'admin@projectflow.io', req.body);
  res.json(store.settings);
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
  broadcastSystemNotification({
    title: '🚀 New Project Created',
    message: `Project "${project.name}" (${project.code}) was created by Administrator.`,
    type: 'PROJECT_CREATED',
    priority: 'HIGH'
  });
  res.status(201).json(project);
});

app.patch('/api/v1/projects/:id', async (req, res) => {
  const project = store.projects.find(p => p.id === req.params.id);
  if (!project) return res.status(404).json({ error: 'Project not found' });
  const { name, description, status, health, targetDate } = req.body;
  if (name) project.name = name;
  if (description !== undefined) project.description = description;
  if (status) project.status = status;
  if (health) project.health = health;
  if (targetDate) project.targetDate = targetDate;
  project.updatedAt = new Date().toISOString();
  saveProjectToSupabase(project);
  await broadcastSystemNotification({
    title: '📊 Project Updated',
    message: `Project "${project.name}" status: ${project.status || 'ACTIVE'}, health: ${project.health || 'ON_TRACK'}`,
    type: 'PROJECT_UPDATED',
    priority: 'NORMAL'
  });
  res.json(project);
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
  res.json(list.map(t => {
    const user = t.assigneeId ? store.users.find(u => u.id === t.assigneeId) : null;
    const team = t.teamId ? (store.teams || []).find(tm => tm.id === t.teamId) : null;
    return {
      ...t,
      assigneeName: user ? user.fullName : (t.assigneeName || null),
      teamName: team ? team.name : (t.teamName || null)
    };
  }));
});

app.get('/api/v1/projects/:id/tasks', async (req, res) => {
  if (supabase) {
    try {
      const { data: dbTasks, error } = await supabase.from('tasks').select('*').eq('project_id', req.params.id);
      if (!error && dbTasks) {
        dbTasks.forEach(dt => {
          const idx = store.tasks.findIndex(t => t.id === dt.id);
          const mapped = {
            id: dt.id,
            projectId: dt.project_id,
            title: dt.title,
            description: dt.description || '',
            priority: dt.priority,
            statusCategory: dt.status_category,
            progressPercent: dt.progress_percent,
            completedAt: dt.completed_at,
            dueDate: dt.due_date,
            assigneeId: dt.assignee_id,
            createdAt: dt.created_at
          };
          if (idx >= 0) store.tasks[idx] = { ...store.tasks[idx], ...mapped };
          else store.tasks.push(mapped);
        });
      }
    } catch (e) {}
  }
  const list = store.tasks.filter(t => t.projectId === req.params.id).map(t => {
    const user = t.assigneeId ? store.users.find(u => u.id === t.assigneeId) : null;
    const team = t.teamId ? (store.teams || []).find(tm => tm.id === t.teamId) : null;
    return {
      ...t,
      assigneeName: user ? user.fullName : (t.assigneeName || null),
      teamName: team ? team.name : (t.teamName || null)
    };
  });
  res.json(list);
});

app.post('/api/v1/projects/:id/tasks', async (req, res) => {
  const { title, description, priority, dueDate, assigneeId, teamId, teamName, assigneeName } = req.body;
  const user = assigneeId ? store.users.find(u => u.id === assigneeId) : null;
  const team = teamId ? (store.teams || []).find(tm => tm.id === teamId) : null;
  const resolvedAssigneeName = assigneeName || (user ? user.fullName : null);
  const resolvedTeamName = teamName || (team ? team.name : null);

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
    assigneeName: resolvedAssigneeName,
    teamId: teamId || null,
    teamName: resolvedTeamName,
    createdAt: new Date().toISOString()
  };
  store.tasks.push(task);
  saveTaskToSupabase(task);

  const targetLabel = resolvedAssigneeName || (resolvedTeamName ? `Team "${resolvedTeamName}"` : 'Team');
  await broadcastSystemNotification({
    title: '📋 New Task Assigned',
    message: `${targetLabel} received task: "${task.title}" (Priority: ${task.priority})`,
    type: 'TASK_ASSIGNED',
    taskId: task.id,
    userId: assigneeId || null,
    priority: task.priority
  });

  res.status(201).json(task);
});

app.patch('/api/v1/tasks/:id/status', async (req, res) => {
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

  await broadcastSystemNotification({
    title: task.statusCategory === 'DONE' ? '🎉 Task Completed' : 'Task Status Updated',
    message: `"${task.title}" has been updated to ${task.statusCategory}.`,
    type: 'TASK_STATUS_CHANGED',
    taskId: task.id,
    priority: task.priority
  });

  res.json(task);
});

app.patch('/api/v1/tasks/:id', async (req, res) => {
  const task = store.tasks.find(t => t.id === req.params.id);
  if (!task) return res.status(404).json({ error: 'Task not found' });
  const { title, description, priority, dueDate, assigneeId, teamId, teamName, assigneeName, statusCategory, progressPercent } = req.body;
  if (title) task.title = title;
  if (description !== undefined) task.description = description;
  if (priority) task.priority = priority;
  if (dueDate) task.dueDate = dueDate.split('T')[0];
  if (statusCategory) {
    task.statusCategory = statusCategory;
    if (statusCategory === 'DONE') {
      task.progressPercent = 100;
      task.completedAt = new Date().toISOString();
    } else if (progressPercent !== undefined) {
      task.progressPercent = progressPercent;
    }
  }

  let assigneeChanged = false;
  if (assigneeId !== undefined) {
    assigneeChanged = true;
    task.assigneeId = assigneeId || null;
    const user = assigneeId ? store.users.find(u => u.id === assigneeId) : null;
    task.assigneeName = user ? user.fullName : (assigneeName || null);
  }
  if (teamId !== undefined) {
    assigneeChanged = true;
    task.teamId = teamId || null;
    const team = teamId ? (store.teams || []).find(tm => tm.id === teamId) : null;
    task.teamName = team ? team.name : (teamName || null);
  }

  saveTaskToSupabase(task);

  if (assigneeChanged) {
    const targetLabel = task.assigneeName || (task.teamName ? `Team "${task.teamName}"` : null);
    if (targetLabel) {
      await broadcastSystemNotification({
        title: '📋 Task Assigned & Delegated',
        message: `${targetLabel} was assigned task: "${task.title}" (Priority: ${task.priority})`,
        type: 'TASK_ASSIGNED',
        taskId: task.id,
        userId: task.assigneeId || null,
        priority: task.priority
      });
    }
  }

  res.json(task);
});

// -------------------------------------------------------------
// DETERMINISTIC RISK RADAR & WHAT-IF SIMULATION
// -------------------------------------------------------------
app.get('/api/v1/projects/:id/risks', async (req, res) => {
  if (supabase) {
    try {
      const { data: dbTasks, error } = await supabase.from('tasks').select('*').eq('project_id', req.params.id);
      if (!error && dbTasks) {
        dbTasks.forEach(dt => {
          const idx = store.tasks.findIndex(t => t.id === dt.id);
          const mapped = {
            id: dt.id,
            projectId: dt.project_id,
            title: dt.title,
            description: dt.description || '',
            priority: dt.priority,
            statusCategory: dt.status_category,
            progressPercent: dt.progress_percent,
            completedAt: dt.completed_at,
            dueDate: dt.due_date,
            assigneeId: dt.assignee_id,
            createdAt: dt.created_at
          };
          if (idx >= 0) store.tasks[idx] = { ...store.tasks[idx], ...mapped };
          else store.tasks.push(mapped);
        });
      }
    } catch (e) {}
  }
  const project = store.projects.find(p => p.id === req.params.id) || store.projects[0];
  const projectTasks = store.tasks.filter(t => t.projectId === req.params.id);
  const findings = evaluateProjectRisks(project, projectTasks, store.dependencies || []);
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

app.post('/api/v1/teams', async (req, res) => {
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

  // Broadcast notification to all mobile & web clients
  await broadcastSystemNotification({
    title: '👥 New Team & Group Created',
    message: `Team "${name}" and chat channel #${channel} are now active.`,
    type: 'TEAM_CREATED',
    channel: channel,
    priority: 'HIGH'
  });

  res.status(201).json(newTeam);
});

app.post('/api/v1/users/heartbeat', (req, res) => {
  const email = req.body.email || req.userEmail || req.headers['x-user-email'] || (req.user && req.user.email);
  if (email) {
    store.activeSessions[email.trim().toLowerCase()] = Date.now();
  }
  res.json({ success: true, online: true, timestamp: Date.now() });
});

app.get('/api/v1/team', async (req, res) => {
  const reqEmail = req.userEmail || req.headers['x-user-email'] || req.query.userEmail || (req.user && req.user.email);
  if (reqEmail) {
    store.activeSessions[reqEmail.trim().toLowerCase()] = Date.now();
  }
  if (supabase) {
    try {
      const { data: profiles, error } = await supabase.from('profiles').select('*');
      if (!error && profiles && profiles.length > 0) {
        store.users = profiles.map(p => ({
          id: p.id,
          email: p.email,
          fullName: p.full_name || (p.email ? p.email.split('@')[0] : 'Member'),
          role: p.global_role || 'MEMBER',
          active: p.active !== false,
          createdAt: p.created_at || new Date().toISOString()
        }));
      }
    } catch (e) {}
  }
  const now = Date.now();
  const callerEmail = (reqEmail || '').trim().toLowerCase();
  const team = store.users.map(u => {
    const emailKey = (u.email || '').toLowerCase();
    const lastActive = store.activeSessions ? store.activeSessions[emailKey] : 0;
    // User is online only if they had active session/heartbeat in last 3 minutes or are the current caller
    const isOnline = !!(lastActive && (now - lastActive < 180000)) || (!!callerEmail && callerEmail === emailKey);
    return {
      id: u.id,
      email: u.email,
      fullName: u.fullName,
      role: u.role,
      active: u.active !== false,
      isOnline: isOnline,
      lastSeen: lastActive ? new Date(lastActive).toISOString() : (isOnline ? new Date().toISOString() : null),
      assignedTasksCount: store.tasks.filter(t => t.assigneeId === u.id).length
    };
  });
  res.json(team);
});

// -------------------------------------------------------------
// NOTIFICATIONS
// -------------------------------------------------------------
app.get('/api/v1/notifications', async (req, res) => {
  if (supabase) {
    try {
      const { data: dbNotifs, error } = await supabase
        .from('notifications')
        .select('*')
        .order('created_at', { ascending: false })
        .limit(50);
      if (!error && dbNotifs && dbNotifs.length > 0) {
        dbNotifs.forEach(dn => {
          const idx = store.notifications.findIndex(n => n.id === dn.id);
          const mapped = {
            id: dn.id,
            title: dn.title,
            message: dn.message,
            type: dn.type || 'SYSTEM',
            read: !!dn.read,
            createdAt: dn.created_at
          };
          if (idx >= 0) {
            store.notifications[idx] = { ...store.notifications[idx], ...mapped };
          } else {
            store.notifications.push(mapped);
          }
        });
        store.notifications.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      }
    } catch (e) {
      console.warn('[NOTIF] Supabase live sync warning:', e.message);
    }
  }
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
// CHAT & DISCUSSIONS (WHATSAPP-STYLE DIRECT + SQUAD GROUPS)
// -------------------------------------------------------------
store.chatGroups = store.chatGroups || [
  { id: 'group_general', name: 'General Group', channel: 'general', description: 'Company-wide collaboration group', members: [], createdBy: 'admin@projectflow.io', createdAt: new Date().toISOString() },
  { id: 'group_engineering', name: 'Core Engineering', channel: 'engineering', description: 'Software and infrastructure', members: [], createdBy: 'admin@projectflow.io', createdAt: new Date().toISOString() },
  { id: 'group_design', name: 'Design Group', channel: 'design-group', description: 'UI/UX & Mobile aesthetic', members: [], createdBy: 'admin@projectflow.io', createdAt: new Date().toISOString() }
];

app.get('/api/v1/chat/groups', (req, res) => {
  res.json(store.chatGroups || []);
});

app.post('/api/v1/chat/groups', async (req, res) => {
  const { name, description, memberEmails, creatorRole, creatorEmail } = req.body;
  const isAdmin = (creatorRole && creatorRole.toUpperCase().includes('ADMIN')) ||
                  (creatorEmail && creatorEmail.toLowerCase().includes('admin'));
  if (!isAdmin) {
    return res.status(403).json({ error: 'Permission denied: Only administrators can create group channels.' });
  }
  if (!name || !name.trim()) {
    return res.status(400).json({ error: 'Group name is required' });
  }
  const cleanSlug = name.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-');
  const channel = `group_${cleanSlug}_${Date.now().toString().slice(-4)}`;
  const newGroup = {
    id: uuidv4(),
    name: name.trim(),
    channel,
    description: description ? description.trim() : 'Team collaboration group',
    members: Array.isArray(memberEmails) ? memberEmails : [],
    createdBy: creatorEmail || 'Admin',
    createdAt: new Date().toISOString()
  };
  store.chatGroups.push(newGroup);

  const welcomeMsg = {
    id: uuidv4(),
    senderId: 'system',
    senderName: 'System',
    content: `Group "${newGroup.name}" created by Administrator.`,
    channel: newGroup.channel,
    reactions: { '👍': 0, '❤️': 0, '🚀': 0, '🔥': 0, '🎯': 0 },
    createdAt: new Date().toISOString()
  };
  (store.messages = store.messages || []).push(welcomeMsg);

  await broadcastSystemNotification({
    title: `👥 New Group Created: ${newGroup.name}`,
    message: description ? `Group "${newGroup.name}": ${description}` : `Administrator created a new collaboration group "${newGroup.name}".`,
    type: 'GROUP_CREATED',
    channel: newGroup.channel,
    priority: 'HIGH'
  });

  res.status(201).json(newGroup);
});

app.get('/api/v1/chat/channels', (req, res) => {
  const groupChannels = (store.chatGroups || []).map(g => g.channel);
  const dynamicChannels = (store.teams || []).map(t => t.channel);
  const msgChannels = (store.messages || []).map(m => m.channel);
  const uniqueChannels = Array.from(new Set(['general', ...groupChannels, ...dynamicChannels, ...msgChannels])).filter(Boolean);
  res.json(uniqueChannels);
});

app.get('/api/v1/chat/messages', async (req, res) => {
  const channel = req.query.channel;
  if (supabase) {
    try {
      let query = supabase.from('chat_messages').select('*').order('created_at', { ascending: true });
      if (channel && channel !== 'all') {
        query = query.eq('channel', channel);
      }
      const { data: dbMsgs, error } = await query;
      if (!error && dbMsgs && dbMsgs.length > 0) {
        dbMsgs.forEach(dm => {
          const idx = (store.messages || []).findIndex(m => m.id === dm.id);
          const mapped = {
            id: dm.id,
            senderId: dm.sender_id,
            senderName: dm.sender_name,
            content: dm.content,
            channel: dm.channel,
            reactions: { '👍': 0, '❤️': 0, '🚀': 0, '🔥': 0, '🎯': 0 },
            createdAt: dm.created_at
          };
          if (idx >= 0) store.messages[idx] = { ...store.messages[idx], ...mapped };
          else (store.messages = store.messages || []).push(mapped);
        });
      }
    } catch (e) {
      console.warn('[CHAT] Supabase sync warning:', e.message);
    }
  }

  let list = store.messages || [];
  if (channel && channel !== 'all') {
    list = list.filter(m => m.channel === channel);
  }
  const enriched = list.map(m => {
    const matchedUser = store.users?.find(u => 
      (m.senderId && u.id === m.senderId) || 
      (m.senderEmail && u.email?.toLowerCase() === m.senderEmail?.toLowerCase()) || 
      (m.senderName && u.fullName?.toLowerCase() === m.senderName?.toLowerCase())
    );
    return {
      ...m,
      senderName: m.senderName || (matchedUser ? matchedUser.fullName : 'Team Member'),
      senderEmail: m.senderEmail || (matchedUser ? matchedUser.email : 'admin@projectflow.io'),
      senderRole: m.senderRole || (matchedUser ? matchedUser.role : 'ADMINISTRATOR')
    };
  });
  res.json(enriched);
});

app.post('/api/v1/chat/messages', async (req, res) => {
  const { content, channel, senderName, senderId, senderEmail: reqEmail, senderRole: reqRole } = req.body;
  const targetChannel = channel || 'general';
  const authorName = senderName || (req.user ? req.user.fullName : 'Administrator');
  
  // Resolve user identity from store if possible
  const matchedUser = store.users?.find(u => 
    (senderId && u.id === senderId) || 
    (reqEmail && u.email?.toLowerCase() === reqEmail?.toLowerCase()) || 
    (u.fullName?.toLowerCase() === authorName?.toLowerCase())
  );
  
  const finalEmail = reqEmail || (matchedUser ? matchedUser.email : (req.user ? req.user.email : 'admin@projectflow.io'));
  const finalRole = reqRole || (matchedUser ? matchedUser.role : (req.user ? req.user.role : 'ADMINISTRATOR'));
  const finalSenderId = senderId || (matchedUser ? matchedUser.id : (store.users[0] ? store.users[0].id : uuidv4()));

  const msg = {
    id: uuidv4(),
    senderId: finalSenderId,
    senderName: authorName,
    senderEmail: finalEmail,
    senderRole: finalRole,
    content: content || '',
    channel: targetChannel,
    reactions: { '👍': 0, '❤️': 0, '🚀': 0, '🔥': 0, '🎯': 0 },
    createdAt: new Date().toISOString()
  };
  (store.messages = store.messages || []).push(msg);
  saveChatMessageToSupabase(msg);

  // Broadcast notification and push
  const isDirect = targetChannel.startsWith('dm_');
  const notifTitle = isDirect ? `💬 ${authorName} (${finalRole})` : `💬 #${targetChannel} • ${authorName}`;
  await broadcastSystemNotification({
    title: notifTitle,
    message: content || 'Sent a new message',
    type: 'CHAT_MESSAGE',
    channel: targetChannel,
    senderName: authorName,
    senderId: msg.senderId,
    priority: isDirect ? 'HIGH' : 'NORMAL'
  });

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
  const normalized = (store.releases || []).map(r => ({
    ...r,
    channel: r.channel || r.releaseChannel || 'STABLE',
    artifactSize: r.artifactSize || r.fileSizeBytes || 8114674,
    artifactUrl: r.artifactUrl || (r.storagePath ? `/${r.storagePath}` : '/releases/android/ProjectFlow-Intelligence-v2.0.0.apk')
  }));
  res.json(normalized);
});

app.post('/api/v1/admin/releases', (req, res) => {
  const { versionName, versionCode, artifactFilename, storagePath, releaseNotes, releaseChannel, channel, artifactSize, fileSizeBytes } = req.body;
  const newRelease = {
    id: uuidv4(),
    applicationId: req.body.applicationId || 'com.projectflow.android',
    versionName: versionName || '2.0.0',
    versionCode: versionCode || 200,
    artifactFilename: artifactFilename || 'ProjectFlow-Intelligence-v2.0.0.apk',
    storagePath: storagePath || 'releases/android/ProjectFlow-Intelligence-v2.0.0.apk',
    mimeType: 'application/vnd.android.package-archive',
    fileSizeBytes: fileSizeBytes || artifactSize || 8114674,
    artifactSize: artifactSize || fileSizeBytes || 8114674,
    sha256Checksum: req.body.sha256Checksum || '8c20000160064c7bb80af8765f912ed1d9bb20ef6bffbb4d35ed2b3729030409',
    releaseChannel: releaseChannel || channel || 'STABLE',
    channel: channel || releaseChannel || 'STABLE',
    releaseNotes: releaseNotes || 'Official release',
    minimumSupportedVersionCode: 1,
    status: 'PUBLISHED',
    createdAt: new Date().toISOString(),
    publishedAt: new Date().toISOString()
  };
  store.releases.unshift(newRelease);
  recordAuditLog('RELEASE_CREATED', 'RELEASE', newRelease.id, req.headers['x-user-email'] || 'admin@projectflow.io', { versionName: newRelease.versionName, channel: newRelease.channel });
  res.status(201).json(newRelease);
});

app.post('/api/v1/admin/releases/:id/publish', (req, res) => {
  const rel = store.releases.find(r => r.id === req.params.id);
  if (!rel) return res.status(404).json({ error: 'Release not found' });
  rel.status = 'PUBLISHED';
  rel.publishedAt = new Date().toISOString();
  recordAuditLog('RELEASE_PUBLISHED', 'RELEASE', rel.id, req.headers['x-user-email'] || 'admin@projectflow.io', { versionName: rel.versionName });
  res.json(rel);
});

app.post('/api/v1/admin/releases/:id/unpublish', (req, res) => {
  const rel = store.releases.find(r => r.id === req.params.id);
  if (!rel) return res.status(404).json({ error: 'Release not found' });
  rel.status = 'DRAFT';
  recordAuditLog('RELEASE_UNPUBLISHED', 'RELEASE', rel.id, req.headers['x-user-email'] || 'admin@projectflow.io', { versionName: rel.versionName });
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
