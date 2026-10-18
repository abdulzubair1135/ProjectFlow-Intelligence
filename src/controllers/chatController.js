const { v4: uuidv4 } = require('uuid');
const { store, supabase, saveChatMessageToSupabase } = require('../supabase');
const { broadcastSystemNotification } = require('./notificationController');

store.chatGroups = store.chatGroups || [
  { id: 'group_general', name: 'General Group', channel: 'general', description: 'Company-wide collaboration group', members: [], createdBy: 'admin@projectflow.io', createdAt: new Date().toISOString() },
  { id: 'group_engineering', name: 'Core Engineering', channel: 'engineering', description: 'Software and infrastructure', members: [], createdBy: 'admin@projectflow.io', createdAt: new Date().toISOString() },
  { id: 'group_design', name: 'Design Group', channel: 'design-group', description: 'UI/UX & Mobile aesthetic', members: [], createdBy: 'admin@projectflow.io', createdAt: new Date().toISOString() }
];

function getGroups(req, res) {
  res.json(store.chatGroups || []);
}

async function createGroup(req, res) {
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
  saveChatMessageToSupabase(welcomeMsg);

  await broadcastSystemNotification({
    title: `👥 New Group Created: ${newGroup.name}`,
    message: description ? `Group "${newGroup.name}": ${description}` : `Administrator created a new collaboration group "${newGroup.name}".`,
    type: 'GROUP_CREATED',
    channel: newGroup.channel,
    priority: 'HIGH'
  });

  res.status(201).json(newGroup);
}

function getChannels(req, res) {
  const groupChannels = (store.chatGroups || []).map(g => g.channel);
  const dynamicChannels = (store.teams || []).map(t => t.channel);
  const msgChannels = (store.messages || []).map(m => m.channel);
  const uniqueChannels = Array.from(new Set(['general', ...groupChannels, ...dynamicChannels, ...msgChannels])).filter(Boolean);
  res.json(uniqueChannels);
}

async function getMessages(req, res) {
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
}

async function sendMessage(req, res) {
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
}

function reactMessage(req, res) {
  const { emoji } = req.body;
  const msg = (store.messages || []).find(m => m.id === req.params.id);
  if (!msg) return res.status(404).json({ error: 'Message not found' });
  msg.reactions = msg.reactions || {};
  const emo = emoji || '👍';
  msg.reactions[emo] = (msg.reactions[emo] || 0) + 1;
  res.json(msg);
}

module.exports = {
  getGroups,
  createGroup,
  getChannels,
  getMessages,
  sendMessage,
  reactMessage
};
