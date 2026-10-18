const { v4: uuidv4 } = require('uuid');
const { store, supabase, saveTaskToSupabase } = require('../supabase');
const { broadcastSystemNotification } = require('./notificationController');

function getAllTasks(req, res) {
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
}

async function getProjectTasks(req, res) {
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
}

async function createTask(req, res) {
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
}

async function updateTask(req, res) {
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
}

async function updateTaskStatus(req, res) {
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
}

module.exports = {
  getAllTasks,
  getProjectTasks,
  createTask,
  updateTask,
  updateTaskStatus
};
