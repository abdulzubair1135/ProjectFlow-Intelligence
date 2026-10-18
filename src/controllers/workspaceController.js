const { v4: uuidv4 } = require('uuid');
const { store, saveWorkspaceToSupabase, saveProjectToSupabase } = require('../supabase');
const { broadcastSystemNotification } = require('./notificationController');

function getWorkspaces(req, res) {
  res.json(store.workspaces);
}

function createWorkspace(req, res) {
  const { name, slug, description } = req.body;
  const ws = {
    id: uuidv4(),
    name: name || 'Default Workspace',
    slug: slug || 'default-ws',
    description: description || '',
    ownerId: store.users[0] ? store.users[0].id : uuidv4(),
    archived: false,
    createdAt: new Date().toISOString()
  };
  store.workspaces.push(ws);
  saveWorkspaceToSupabase(ws);
  res.status(201).json(ws);
}

function getWorkspaceProjects(req, res) {
  const list = store.projects.filter(p => p.workspaceId === req.params.id);
  res.json(list);
}

async function createWorkspaceProject(req, res) {
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
  await broadcastSystemNotification({
    title: '🚀 New Project Created',
    message: `Project "${project.name}" (${project.code}) was created by Administrator.`,
    type: 'PROJECT_CREATED',
    priority: 'HIGH'
  });
  res.status(201).json(project);
}

module.exports = {
  getWorkspaces,
  createWorkspace,
  getWorkspaceProjects,
  createWorkspaceProject
};
