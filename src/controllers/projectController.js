const { store, saveProjectToSupabase } = require('../supabase');
const { broadcastSystemNotification } = require('./notificationController');

function getAllProjects(req, res) {
  res.json(store.projects);
}

async function updateProject(req, res) {
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
}

module.exports = {
  getAllProjects,
  updateProject
};
