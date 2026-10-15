import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Project, Task, TaskPriority, Team, UserProfile } from '../types';
import { CheckSquare, Plus, RefreshCw, Calendar, AlertCircle, Users, User, Hash, Check } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';

export const Tasks: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string>(searchParams.get('projectId') || '');
  const [tasks, setTasks] = useState<Task[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Create Task Modal
  const [showModal, setShowModal] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM');
  const [dueDate, setDueDate] = useState('');
  const [assignType, setAssignType] = useState<'USER' | 'TEAM' | 'NONE'>('NONE');
  const [selectedUserId, setSelectedUserId] = useState('');
  const [selectedTeamId, setSelectedTeamId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Create Team Modal
  const [showTeamModal, setShowTeamModal] = useState(false);
  const [newTeamName, setNewTeamName] = useState('');
  const [newTeamDesc, setNewTeamDesc] = useState('');
  const [teamSubmitting, setTeamSubmitting] = useState(false);
  const [teamSuccessMsg, setTeamSuccessMsg] = useState<string | null>(null);

  useEffect(() => {
    async function loadInitialData() {
      try {
        const [prjs, tms, usrs] = await Promise.all([
          api.getProjects().catch(() => []),
          api.getTeams().catch(() => []),
          api.getUsers('', 0, 50).catch(() => ({ content: [] }))
        ]);
        setProjects(prjs);
        setTeams(tms);
        setUsers(usrs.content || []);
        if (prjs.length > 0 && !selectedProjectId) {
          setSelectedProjectId(prjs[0].id);
        }
      } catch (err: any) {
        console.warn('Failed loading initial data', err);
      }
    }
    loadInitialData();
  }, []);

  const fetchTasks = async (projId: string) => {
    if (!projId) {
      setTasks([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await api.getProjectTasks(projId);
      setTasks(data || []);
    } catch (err: any) {
      setError(err.message || 'Failed to load project tasks');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedProjectId) {
      fetchTasks(selectedProjectId);
      setSearchParams({ projectId: selectedProjectId });
    }
  }, [selectedProjectId]);

  const handleCreateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProjectId) return;
    setSubmitting(true);
    try {
      let assigneeId: string | undefined;
      let assigneeName: string | undefined;
      let teamId: string | undefined;
      let teamName: string | undefined;

      if (assignType === 'USER' && selectedUserId) {
        const found = users.find(u => u.id === selectedUserId);
        assigneeId = selectedUserId;
        assigneeName = found ? found.fullName : undefined;
      } else if (assignType === 'TEAM' && selectedTeamId) {
        const found = teams.find(t => t.id === selectedTeamId);
        teamId = selectedTeamId;
        teamName = found ? found.name : undefined;
      }

      const newTask = await api.createTask(selectedProjectId, {
        title,
        description,
        priority,
        dueDate: dueDate ? new Date(dueDate).toISOString() : undefined,
        assigneeId,
        assigneeName,
        teamId,
        teamName
      });
      setTasks((prev) => [newTask, ...prev]);
      setShowModal(false);
      setTitle('');
      setDescription('');
      setDueDate('');
      setAssignType('NONE');
      setSelectedUserId('');
      setSelectedTeamId('');
    } catch (err: any) {
      alert(`Task creation failed: ${err.message}`);
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateTeam = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTeamName.trim()) return;
    setTeamSubmitting(true);
    try {
      const created = await api.createTeam({
        name: newTeamName.trim(),
        description: newTeamDesc.trim()
      });
      setTeams((prev) => [...prev, created]);
      setTeamSuccessMsg(`Team "${created.name}" and chat channel #${created.channelName || created.name.toLowerCase().replace(/\\s+/g, '-')} created successfully!`);
      setNewTeamName('');
      setNewTeamDesc('');
      setTimeout(() => {
        setTeamSuccessMsg(null);
        setShowTeamModal(false);
      }, 1500);
    } catch (err: any) {
      alert(`Team creation failed: ${err.message}`);
    } finally {
      setTeamSubmitting(false);
    }
  };

  const getPriorityBadge = (p: TaskPriority) => {
    switch (p) {
      case 'CRITICAL':
        return <span className="bg-[#C84B31]/15 text-[#C84B31] px-2 py-0.5 rounded text-[10px] font-mono font-bold">CRITICAL</span>;
      case 'HIGH':
        return <span className="bg-[#D97706]/15 text-[#D97706] px-2 py-0.5 rounded text-[10px] font-mono font-bold">HIGH</span>;
      case 'MEDIUM':
        return <span className="bg-[#56664C]/15 text-[#56664C] px-2 py-0.5 rounded text-[10px] font-mono font-semibold">MEDIUM</span>;
      default:
        return <span className="bg-[#E2E0D8] text-[#5A6258] px-2 py-0.5 rounded text-[10px] font-mono">LOW</span>;
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Title & Filter */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-[#292D28] tracking-tight">Task Operations</h2>
          <p className="text-sm text-[#78866B]">
            Work item scheduling, status tracking, and dependency chain monitoring.
          </p>
        </div>
        <div className="flex items-center gap-3">
          {projects.length > 0 && (
            <select
              value={selectedProjectId}
              onChange={(e) => setSelectedProjectId(e.target.value)}
              className="px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs font-semibold text-[#292D28] focus:outline-none focus:border-[#56664C]"
            >
              {projects.map((p) => (
                <option key={p.id} value={p.id}>
                  [{p.code}] {p.name}
                </option>
              ))}
            </select>
          )}

          <button
            onClick={() => fetchTasks(selectedProjectId)}
            className="flex items-center gap-2 px-3 py-2 rounded-lg bg-[#FAFAF7] border border-[#E2E0D8] text-xs font-semibold text-[#5A6258] hover:text-[#292D28] hover:bg-[#EFECE4] transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-[#56664C]' : ''}`} />
          </button>

          <button
            onClick={() => setShowTeamModal(true)}
            className="flex items-center gap-2 px-3.5 py-2 rounded-lg bg-[#EFECE4] border border-[#E2E0D8] text-xs font-semibold text-[#56664C] hover:bg-[#E2E0D8] transition-colors shadow-sm"
          >
            <Users className="w-3.5 h-3.5" />
            + New Team
          </button>

          <button
            disabled={!selectedProjectId}
            onClick={() => setShowModal(true)}
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-[#56664C] text-[#F4F2E9] text-xs font-semibold hover:bg-[#485640] transition-colors shadow-sm disabled:opacity-50"
          >
            <Plus className="w-4 h-4" />
            Add Task
          </button>
        </div>
      </div>

      {error && (
        <div className="p-3.5 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs">
          {error}
        </div>
      )}

      {/* Task List */}
      <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#F4F2E9] border-b border-[#E2E0D8] text-[#5A6258] font-semibold uppercase tracking-wider">
              <tr>
                <th className="px-6 py-3.5">Title</th>
                <th className="px-6 py-3.5">Assigned To</th>
                <th className="px-6 py-3.5">Priority</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5">Progress</th>
                <th className="px-6 py-3.5">Due Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#E2E0D8] text-[#292D28]">
              {tasks.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-[#5A6258]">
                    <div className="w-10 h-10 mx-auto mb-2 rounded-full bg-[#E2E0D8] text-[#56664C] flex items-center justify-center">
                      <CheckSquare className="w-5 h-5" />
                    </div>
                    {projects.length === 0
                      ? 'No projects available. Create a project first.'
                      : 'No tasks found for this project. Click "Add Task" to create one.'}
                  </td>
                </tr>
              ) : (
                tasks.map((t) => (
                  <tr key={t.id} className="hover:bg-[#F4F2E9]/60 transition-colors">
                    <td className="px-6 py-4">
                      <div className="font-semibold text-[#292D28]">{t.title}</div>
                      {t.description && (
                        <div className="text-[11px] text-[#78866B] line-clamp-1">{t.description}</div>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      {t.teamName ? (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-semibold bg-[#56664C]/15 text-[#56664C] border border-[#56664C]/30">
                          <Users className="w-3 h-3" />
                          {t.teamName}
                        </span>
                      ) : t.assigneeName ? (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-medium bg-[#EFECE4] text-[#292D28] border border-[#E2E0D8]">
                          <User className="w-3 h-3 text-[#5A6258]" />
                          {t.assigneeName}
                        </span>
                      ) : (
                        <span className="text-[10px] text-[#78866B] italic">Unassigned</span>
                      )}
                    </td>
                    <td className="px-6 py-4">{getPriorityBadge(t.priority)}</td>
                    <td className="px-6 py-4">
                      <span className="font-mono text-[10px] font-semibold px-2 py-0.5 rounded bg-[#E2E0D8] text-[#292D28]">
                        {t.statusCategory || 'TODO'}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="w-24 bg-[#E2E0D8] rounded-full h-1.5 overflow-hidden">
                        <div
                          className="bg-[#56664C] h-1.5 rounded-full"
                          style={{ width: `${t.progressPercent || 0}%` }}
                        ></div>
                      </div>
                      <span className="text-[10px] font-mono text-[#78866B]">{t.progressPercent || 0}%</span>
                    </td>
                    <td className="px-6 py-4 font-mono text-[11px] text-[#78866B]">
                      {t.dueDate ? (
                        <div className="flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5" />
                          {new Date(t.dueDate).toLocaleDateString()}
                        </div>
                      ) : (
                        'No deadline'
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Task Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl max-w-md w-full p-6 shadow-xl space-y-4">
            <h3 className="text-base font-bold text-[#292D28]">Add New Task</h3>
            <p className="text-xs text-[#5A6258]">
              Create an operational task within the selected project and assign it to an individual or team squad.
            </p>

            <form onSubmit={handleCreateTask} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                  Task Title
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Implement OAuth Flow"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                  Priority
                </label>
                <select
                  value={priority}
                  onChange={(e) => setPriority(e.target.value as TaskPriority)}
                  className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
                >
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                  <option value="CRITICAL">CRITICAL</option>
                </select>
              </div>

              {/* Assignment Selector */}
              <div>
                <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1.5">
                  Assign To
                </label>
                <div className="grid grid-cols-3 gap-2 mb-2">
                  <button
                    type="button"
                    onClick={() => setAssignType('NONE')}
                    className={`py-1.5 text-xs font-medium rounded-lg border transition-all ${
                      assignType === 'NONE'
                        ? 'bg-[#56664C] text-[#F4F2E9] border-[#56664C]'
                        : 'bg-white text-[#5A6258] border-[#E2E0D8] hover:bg-[#EFECE4]'
                    }`}
                  >
                    Unassigned
                  </button>
                  <button
                    type="button"
                    onClick={() => setAssignType('TEAM')}
                    className={`flex items-center justify-center gap-1.5 py-1.5 text-xs font-medium rounded-lg border transition-all ${
                      assignType === 'TEAM'
                        ? 'bg-[#56664C] text-[#F4F2E9] border-[#56664C]'
                        : 'bg-white text-[#5A6258] border-[#E2E0D8] hover:bg-[#EFECE4]'
                    }`}
                  >
                    <Users className="w-3.5 h-3.5" />
                    Team Squad
                  </button>
                  <button
                    type="button"
                    onClick={() => setAssignType('USER')}
                    className={`flex items-center justify-center gap-1.5 py-1.5 text-xs font-medium rounded-lg border transition-all ${
                      assignType === 'USER'
                        ? 'bg-[#56664C] text-[#F4F2E9] border-[#56664C]'
                        : 'bg-white text-[#5A6258] border-[#E2E0D8] hover:bg-[#EFECE4]'
                    }`}
                  >
                    <User className="w-3.5 h-3.5" />
                    Member
                  </button>
                </div>

                {assignType === 'TEAM' && (
                  <select
                    value={selectedTeamId}
                    onChange={(e) => setSelectedTeamId(e.target.value)}
                    required
                    className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
                  >
                    <option value="">Select Team / Squad...</option>
                    {teams.map((tm) => (
                      <option key={tm.id} value={tm.id}>
                        {tm.name} (#{tm.channelName || tm.name.toLowerCase().replace(/\s+/g, '-')})
                      </option>
                    ))}
                  </select>
                )}

                {assignType === 'USER' && (
                  <select
                    value={selectedUserId}
                    onChange={(e) => setSelectedUserId(e.target.value)}
                    required
                    className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
                  >
                    <option value="">Select Member...</option>
                    {users.map((u) => (
                      <option key={u.id} value={u.id}>
                        {u.fullName} ({u.role}) - {u.email}
                      </option>
                    ))}
                  </select>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                  Due Date (Optional)
                </label>
                <input
                  type="date"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                  className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                  Description
                </label>
                <textarea
                  rows={2}
                  placeholder="Detailed criteria for completion."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-3.5 py-1.5 rounded-lg border border-[#E2E0D8] text-xs font-semibold text-[#5A6258] hover:bg-[#EFECE4]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-1.5 rounded-lg bg-[#56664C] text-[#F4F2E9] text-xs font-semibold hover:bg-[#485640] disabled:opacity-50"
                >
                  {submitting ? 'Creating & Notifying...' : 'Create Task'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Create Team Modal */}
      {showTeamModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-lg bg-[#56664C]/15 text-[#56664C] flex items-center justify-center">
                <Users className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-base font-bold text-[#292D28]">Create New Team</h3>
                <p className="text-[11px] text-[#78866B]">
                  Automatically provisions a dedicated discussion channel for the team.
                </p>
              </div>
            </div>

            {teamSuccessMsg && (
              <div className="p-3 rounded-lg bg-green-50 border border-green-200 text-green-800 text-xs flex items-center gap-2">
                <Check className="w-4 h-4 text-green-600" />
                {teamSuccessMsg}
              </div>
            )}

            <form onSubmit={handleCreateTeam} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                  Team Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Mobile Dev Squad, Quality Assurance"
                  value={newTeamName}
                  onChange={(e) => setNewTeamName(e.target.value)}
                  className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
                />
              </div>

              {newTeamName && (
                <div className="px-3 py-2 bg-[#F4F2E9] rounded-lg text-[11px] text-[#56664C] flex items-center gap-2 border border-[#E2E0D8]">
                  <Hash className="w-3.5 h-3.5" />
                  <span>Channel: <strong>#{newTeamName.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '')}</strong></span>
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                  Description / Focus Area
                </label>
                <textarea
                  rows={2}
                  placeholder="e.g. Cross-platform Android and desktop client engineering."
                  value={newTeamDesc}
                  onChange={(e) => setNewTeamDesc(e.target.value)}
                  className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowTeamModal(false)}
                  className="px-3.5 py-1.5 rounded-lg border border-[#E2E0D8] text-xs font-semibold text-[#5A6258] hover:bg-[#EFECE4]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={teamSubmitting || !newTeamName.trim()}
                  className="px-4 py-1.5 rounded-lg bg-[#56664C] text-[#F4F2E9] text-xs font-semibold hover:bg-[#485640] disabled:opacity-50 flex items-center gap-1.5"
                >
                  {teamSubmitting ? 'Creating Team & Channel...' : 'Create Team'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
