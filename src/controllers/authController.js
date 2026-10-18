const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');
const { store, supabase } = require('../supabase');
const { JWT_SECRET } = require('../middleware/authMiddleware');
const { recordAuditLog } = require('../middleware/auditLogger');

async function login(req, res) {
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
      console.warn('[AUTH] Supabase signIn fallback:', e.message);
    }
  }

  // 2. Find or dynamically provision user
  let user = store.users.find(u => u.email.toLowerCase() === email);
  if (!user) {
    const role = email.includes('admin') ? 'ADMINISTRATOR' : (email.includes('manager') ? 'MANAGER' : 'MEMBER');
    const defaultName = email.split('@')[0];
    const formattedName = defaultName.charAt(0).toUpperCase() + defaultName.slice(1);
    user = {
      id: authUser ? authUser.id : uuidv4(),
      email,
      fullName: formattedName,
      role,
      active: true,
      createdAt: new Date().toISOString()
    };
    store.users.push(user);

    // Save to Supabase profiles
    if (supabase) {
      try {
        await supabase.from('profiles').upsert({
          id: user.id,
          email: user.email,
          full_name: user.fullName,
          role: user.role,
          active: true,
          created_at: user.createdAt
        });
      } catch (e) {
        console.warn('[AUTH] Supabase profile provision warning:', e.message);
      }
    }
  }

  // 3. Mark active session
  store.activeSessions[email] = Date.now();

  // 4. Generate JWT
  const token = jwt.sign(
    { userId: user.id, email: user.email, role: user.role, fullName: user.fullName },
    JWT_SECRET,
    { expiresIn: '30d' }
  );

  recordAuditLog('USER_LOGGED_IN', 'USER', user.id, user.email, { ip: req.ip, role: user.role });
  res.json({ token, user, message: 'Authentication successful' });
}

async function signup(req, res) {
  const email = (req.body && req.body.email) ? req.body.email.trim().toLowerCase() : '';
  const password = req.body && req.body.password ? req.body.password : '';
  const fullName = (req.body && req.body.fullName) ? req.body.fullName.trim() : (email ? email.split('@')[0] : 'Team Member');

  if (!email) {
    return res.status(400).json({ error: 'Email address is required' });
  }

  let userId = uuidv4();

  // 1. Create in Supabase Auth if connected
  if (supabase && password) {
    try {
      const { data, error } = await supabase.auth.signUp({
        email,
        password,
        options: { data: { full_name: fullName } }
      });
      if (!error && data && data.user) {
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
        role: user.role,
        active: true,
        created_at: user.createdAt
      });
    } catch (e) {
      console.warn('[AUTH] Supabase profile upsert warning:', e.message);
    }
  }

  store.activeSessions[email] = Date.now();

  const token = jwt.sign(
    { userId: user.id, email: user.email, role: user.role, fullName: user.fullName },
    JWT_SECRET,
    { expiresIn: '30d' }
  );

  res.status(201).json({ token, user, message: 'Account created successfully' });
}

function devLogin(req, res) {
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
}

function getMe(req, res) {
  const auth = req.headers.authorization || '';
  let email = req.userEmail || (req.user && req.user.email) || req.headers['x-user-email'];
  if (!email && auth.startsWith('Bearer ')) {
    try {
      const decoded = jwt.decode(auth.substring(7));
      if (decoded && decoded.email) email = decoded.email;
    } catch (e) {}
  }
  if (!email) email = 'admin@projectflow.io';

  const normalized = email.trim().toLowerCase();
  let user = store.users.find(u => u.email.toLowerCase() === normalized);
  if (!user) {
    const isAdm = normalized.includes('admin');
    const isMgr = normalized.includes('manager');
    const defaultName = normalized.split('@')[0];
    user = {
      id: req.user ? req.user.userId : uuidv4(),
      email: normalized,
      fullName: isAdm ? 'System Administrator' : (isMgr ? 'Project Manager' : defaultName.charAt(0).toUpperCase() + defaultName.slice(1)),
      role: isAdm ? 'ADMINISTRATOR' : (isMgr ? 'MANAGER' : 'MEMBER'),
      active: true
    };
    store.users.push(user);
  }
  res.json(user);
}

function updateProfile(req, res) {
  const email = req.userEmail || (req.user && req.user.email) || req.headers['x-user-email'] || 'admin@projectflow.io';
  const user = store.users.find(u => u.email.toLowerCase() === email.toLowerCase()) || store.users[0];
  if (req.body.fullName) user.fullName = req.body.fullName.trim();
  res.json(user);
}

function heartbeat(req, res) {
  const email = req.body.email || req.userEmail || req.headers['x-user-email'] || (req.user && req.user.email);
  if (email) {
    store.activeSessions[email.trim().toLowerCase()] = Date.now();
  }
  res.json({
    success: true,
    online: true,
    status: 'ALIVE',
    timestamp: Date.now(),
    activeCount: Object.keys(store.activeSessions).length
  });
}

module.exports = {
  login,
  signup,
  devLogin,
  getMe,
  updateProfile,
  heartbeat
};
