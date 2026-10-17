import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Project, Workspace, ProjectStatus } from '../types';
import {
  FolderKanban,
  Plus,
  RefreshCw,
  Calendar,
  CheckCircle2,
  AlertTriangle,
  AlertOctagon,
  Search,
  ArrowRight,
  Sparkles,
  SlidersHorizontal
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const Projects: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filter & Search
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [updatingProjectId, setUpdatingProjectId] = useState<string | null>(null);

  // Modal State
  const [showModal, setShowModal] = useState(false);
  const [selectedWs, setSelectedWs] = useState<string>('');
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [description, setDescription] = useState('');
  const [targetDate, setTargetDate] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [projList, wsList] = await Promise.all([
        api.getProjects().catch(() => []),
        api.getWorkspaces().catch(() => [])
      ]);
      setProjects(projList);
      setWorkspaces(wsList);
      if (wsList.length > 0 && !selectedWs) {
        setSelectedWs(wsList[0].id);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load projects');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleNameChange = (val: string) => {
    setName(val);
    const words = val.trim().split(/\s+/);
    if (words.length >= 2) {
      setCode((words[0].substring(0, 3) + '-' + words[1].substring(0, 3)).toUpperCase());
    } else if (val.length >= 3) {
      setCode(val.substring(0, 4).toUpperCase());
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedWs) {
      alert('Please select or create a workspace first');
      return;
    }
    setSubmitting(true);
    try {
      const created = await api.createProject(selectedWs, {
        name: name.trim(),
        code: code.trim().toUpperCase(),
        description: description.trim()
      });
      setProjects((prev) => [created, ...prev]);
      setShowModal(false);
      setName('');
      setCode('');
      setDescription('');
      setTargetDate('');
    } catch (err: any) {
      alert(`Project creation failed: ${err.message}`);
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusChange = async (projectId: string, newStatus: ProjectStatus) => {
    setUpdatingProjectId(projectId);
    try {
      await api.updateProject(projectId, { status: newStatus });
      setProjects(prev => prev.map(p => {
        if (p.id === projectId) {
          return { ...p, status: newStatus };
        }
        return p;
      }));
    } catch (err: any) {
      alert(`Failed to update project status: ${err.message}`);
    } finally {
      setUpdatingProjectId(null);
    }
  };

  const getHealthBadge = (health: string) => {
    switch (health) {
      case 'ON_TRACK':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#47745A] bg-[#47745A]/10 px-2 py-0.5 rounded-full">
            <CheckCircle2 className="w-3 h-3" /> On Track
          </span>
        );
      case 'AT_RISK':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#D97706] bg-[#D97706]/10 px-2 py-0.5 rounded-full">
            <AlertTriangle className="w-3 h-3" /> At Risk
          </span>
        );
      case 'OFF_TRACK':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#B7473C] bg-[#B7473C]/10 px-2 py-0.5 rounded-full">
            <AlertOctagon className="w-3 h-3" /> Off Track
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#56664C] bg-[#56664C]/10 px-2 py-0.5 rounded-full">
            <CheckCircle2 className="w-3 h-3" /> Stable
          </span>
        );
    }
  };

  const filteredProjects = projects.filter(p => {
    const matchesSearch = !searchQuery.trim() ||
      p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.code.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (p.description && p.description.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchesStatus = statusFilter === 'ALL' || p.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <div className="space-y-6 max-w-7xl mx-auto select-none">
      {/* Title & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-[#292D28] tracking-tight">Project Portfolios</h2>
          <p className="text-sm text-[#78866B]">
            Portfolio governance, milestone delivery targets, and schedule baseline status.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <button
            onClick={fetchData}
            title="Refresh Directory"
            className="p-2 rounded-xl bg-[#FAFAF7] border border-[#E2E0D8] text-[#5A6258] hover:text-[#292D28] hover:bg-[#EFECE4] transition-colors"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-[#56664C]' : ''}`} />
          </button>
          <button
            onClick={() => setShowModal(true)}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-[#56664C] text-[#F4F2E9] text-xs font-semibold hover:bg-[#485640] transition-colors shadow-sm"
          >
            <Plus className="w-4 h-4" />
            <span>Create Project</span>
          </button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-[#FAFAF7] p-3 rounded-2xl border border-[#E2E0D8]">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-[#78866B]" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search project name, code, description..."
            className="w-full pl-9 pr-3 py-1.5 bg-white border border-[#E2E0D8] rounded-xl text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
          />
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto overflow-x-auto">
          <span className="text-xs font-semibold text-[#78866B] flex items-center gap-1">
            <SlidersHorizontal className="w-3.5 h-3.5" />
            Status:
          </span>
          {[
            { key: 'ALL', label: 'All' },
            { key: 'PLANNING', label: 'Planning' },
            { key: 'IN_PROGRESS', label: 'In Progress' },
            { key: 'COMPLETED', label: 'Completed' },
            { key: 'ON_HOLD', label: 'On Hold' }
          ].map((st) => (
            <button
              key={st.key}
              onClick={() => setStatusFilter(st.key)}
              className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all whitespace-nowrap ${
                statusFilter === st.key
                  ? 'bg-[#56664C] text-white'
                  : 'bg-[#EFECE4] text-[#5A6258] hover:text-[#292D28]'
              }`}
            >
              {st.label}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div className="p-3.5 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs">
          {error}
        </div>
      )}

      {/* Projects Table */}
      <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-2xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#F4F2E9] border-b border-[#E2E0D8] text-[#5A6258] font-semibold uppercase tracking-wider">
              <tr>
                <th className="px-6 py-3.5">Code & Project</th>
                <th className="px-6 py-3.5">Status (Interactive)</th>
                <th className="px-6 py-3.5">Radar Health</th>
                <th className="px-6 py-3.5">Target Baseline</th>
                <th className="px-6 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#E2E0D8] text-[#292D28]">
              {filteredProjects.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-[#5A6258]">
                    <div className="w-10 h-10 mx-auto mb-2 rounded-full bg-[#E2E0D8] text-[#56664C] flex items-center justify-center">
                      <FolderKanban className="w-5 h-5" />
                    </div>
                    No projects found. Click "Create Project" to initialize your first plan.
                  </td>
                </tr>
              ) : (
                filteredProjects.map((p) => (
                  <tr key={p.id} className="hover:bg-[#F4F2E9]/60 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-xl bg-[#56664C]/10 text-[#56664C] flex items-center justify-center font-mono font-bold text-xs shrink-0">
                          {p.code ? p.code.substring(0, 3) : 'PRJ'}
                        </div>
                        <div>
                          <div className="font-bold text-xs text-[#292D28]">{p.name}</div>
                          <div className="text-[11px] text-[#78866B] font-mono">{p.code}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      {/* Interactive Status Selector */}
                      <select
                        disabled={updatingProjectId === p.id}
                        value={p.status || 'PLANNING'}
                        onChange={(e) => handleStatusChange(p.id, e.target.value as ProjectStatus)}
                        className={`font-mono text-[10px] font-bold px-2 py-1 rounded-lg border border-[#E2E0D8] focus:outline-none ${
                          p.status === 'COMPLETED'
                            ? 'bg-green-100 text-green-800'
                            : p.status === 'IN_PROGRESS'
                            ? 'bg-[#56664C]/15 text-[#56664C]'
                            : p.status === 'ON_HOLD'
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-gray-100 text-gray-800'
                        }`}
                      >
                        <option value="PLANNING">PLANNING</option>
                        <option value="IN_PROGRESS">IN_PROGRESS</option>
                        <option value="ON_HOLD">ON_HOLD</option>
                        <option value="COMPLETED">COMPLETED</option>
                      </select>
                    </td>
                    <td className="px-6 py-4">
                      {getHealthBadge(p.health)}
                    </td>
                    <td className="px-6 py-4 font-mono text-[11px] text-[#78866B]">
                      {p.targetDate ? (
                        <div className="flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5 text-[#56664C]" />
                          {new Date(p.targetDate).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' })}
                        </div>
                      ) : (
                        'Ongoing sprint'
                      )}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Link
                        to={`/tasks?projectId=${p.id}`}
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-[#E2E0D8] bg-white hover:bg-[#EFECE4] text-[#292D28] font-bold text-xs transition-colors shadow-xs"
                      >
                        <span>Tasks Pipeline</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <h3 className="text-base font-bold text-[#292D28]">Create New Project</h3>
            <p className="text-xs text-[#5A6258]">
              Initiates a new project plan within the specified tenant workspace.
            </p>

            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                  Tenant Workspace
                </label>
                <select
                  required
                  value={selectedWs}
                  onChange={(e) => setSelectedWs(e.target.value)}
                  className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-xl text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
                >
                  {workspaces.map((ws) => (
                    <option key={ws.id} value={ws.id}>
                      {ws.name} ({ws.slug})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                  Project Name
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => handleNameChange(e.target.value)}
                  placeholder="e.g. Mobile Client Redesign"
                  className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-xl text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                    Project Code
                  </label>
                  <input
                    type="text"
                    required
                    value={code}
                    onChange={(e) => setCode(e.target.value.toUpperCase())}
                    placeholder="e.g. MOB-APP"
                    className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-xl text-xs text-[#292D28] font-mono focus:outline-none focus:border-[#56664C]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                    Target Delivery Date
                  </label>
                  <input
                    type="date"
                    value={targetDate}
                    onChange={(e) => setTargetDate(e.target.value)}
                    className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-xl text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                  Scope & Deliverable Description
                </label>
                <textarea
                  rows={3}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Outline the core deliverables and objectives..."
                  className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-xl text-xs text-[#292D28] focus:outline-none focus:border-[#56664C] resize-none"
                />
              </div>

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="flex-1 py-2.5 rounded-xl border border-[#E2E0D8] text-xs font-semibold text-[#5A6258] hover:bg-[#EFECE4]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="flex-1 py-2.5 rounded-xl bg-[#56664C] text-[#F4F2E9] text-xs font-semibold hover:bg-[#485640] transition-colors shadow-sm disabled:opacity-50"
                >
                  {submitting ? 'Creating...' : 'Create Project'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
