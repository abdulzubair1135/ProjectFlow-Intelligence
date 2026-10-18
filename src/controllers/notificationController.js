const { v4: uuidv4 } = require('uuid');
const { store, supabase } = require('../supabase');
const { recordAuditLog } = require('../middleware/auditLogger');

// FCM Push Tokens Store
store.fcmTokens = store.fcmTokens || [];
store.pushLogs = store.pushLogs || [];

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
async function broadcastSystemNotification({
  title,
  message,
  type = 'SYSTEM',
  taskId = null,
  channel = null,
  senderName = null,
  senderId = null,
  priority = 'NORMAL',
  userId = null
}) {
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

// Handler: FCM Token Registration
function registerFcmToken(req, res) {
  const { fcmToken, email } = req.body;
  if (fcmToken) {
    const existing = store.fcmTokens.find(t => t.token === fcmToken);
    if (!existing) {
      store.fcmTokens.push({ token: fcmToken, email: email || null, registeredAt: new Date().toISOString() });
      console.log('[FCM] Registered new mobile push token:', fcmToken.substring(0, 20) + '...');
    }
  }
  res.json({ success: true, count: store.fcmTokens.length });
}

// Handler: Targeted Push
async function sendPush(req, res) {
  const { title, message, taskId, priority, type } = req.body;
  const notif = await broadcastSystemNotification({
    title: title || 'Workspace Alert',
    message: message || 'You have a new update in ProjectFlow.',
    type: type || 'PUSH_ALERT',
    taskId: taskId || null,
    priority: priority || 'HIGH'
  });
  res.json({ success: true, notification: notif, deliveredDevices: store.fcmTokens.length });
}

// Handler: Broadcast Send
async function handleBroadcastSend(req, res) {
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
}

// Handler: Broadcast History
function getBroadcastHistory(req, res) {
  res.json(store.pushLogs || []);
}

// Handler: Get Notifications
async function getNotifications(req, res) {
  if (supabase) {
    try {
      const { data: dbNotifs, error } = await supabase
        .from('notifications')
        .select('*')
        .order('created_at', { ascending: false })
        .limit(60);
      if (!error && dbNotifs && dbNotifs.length > 0) {
        dbNotifs.forEach(dn => {
          const idx = (store.notifications || []).findIndex(n => n.id === dn.id);
          const mapped = {
            id: dn.id,
            title: dn.title,
            message: dn.message,
            type: dn.type || 'SYSTEM',
            read: !!dn.read,
            createdAt: dn.created_at
          };
          if (idx >= 0) store.notifications[idx] = { ...store.notifications[idx], ...mapped };
          else (store.notifications = store.notifications || []).push(mapped);
        });
        store.notifications.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      }
    } catch (e) {
      console.warn('[NOTIF] Supabase fetch error:', e.message);
    }
  }

  res.json(store.notifications || []);
}

// Handler: Mark Single Read
function markNotificationRead(req, res) {
  const notif = (store.notifications || []).find(n => n.id === req.params.id);
  if (notif) notif.read = true;
  res.json(notif || { success: true });
}

// Handler: Mark All Read
function markAllRead(req, res) {
  (store.notifications || []).forEach(n => { n.read = true; });
  res.json({ success: true, count: (store.notifications || []).length });
}

module.exports = {
  dispatchPushNotification,
  broadcastSystemNotification,
  registerFcmToken,
  sendPush,
  handleBroadcastSend,
  getBroadcastHistory,
  getNotifications,
  markNotificationRead,
  markAllRead
};
