import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Project, Workspace } from '../types';
import { FolderKanban, Plus, RefreshCw, Layers, Calendar, CheckCircle2, AlertTriangle, AlertOctagon } from 'lucide-react';
import { Link } from 'react-router-dom';

export const Projects: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modal State
  const [showModal, setShowModal] = useState(false);
  const [selectedWs, setSelectedWs] = useState<string>('');
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [description, setDescription] = useState('');
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
      const created = await api.createProject(selectedWs, { name, code, description });
      setProjects((prev) => [created, ...prev]);
      setShowModal(false);
      setName('');
      setCode('');
      setDescription('');
    } catch (err: any) {
      alert(`Project creation failed: ${err.message}`);
    } finally {
      setSubmitting(false);
    }
  };

  const getHealthBadge = (health: string) => {
    switch (health) {
      case 'ON_TRACK':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#56664C] bg-[#56664C]/10 px-2 py-0.5 rounded">
            <CheckCircle2 className="w-3 h-3" /> On Track
          </span>
        );
      case 'AT_RISK':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#D97706] bg-[#D97706]/10 px-2 py-0.5 rounded">
            <AlertTriangle className="w-3 h-3" /> At Risk
          </span>
        );
      case 'OFF_TRACK':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-[#C84B31] bg-[#C84B31]/10 px-2 py-0.5 rounded">
            <AlertOctagon className="w-3 h-3" /> Off Track
          </span>
        );
      default:
        return null;
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Title & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-[#292D28] tracking-tight">Projects Directory</h2>
          <p className="text-sm text-[#78866B]">
            Portfolio governance, schedule baseline monitoring, and execution status.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={fetchData}
            className="flex items-center gap-2 px-3.5 py-2 rounded-lg bg-[#FAFAF7] border border-[#E2E0D8] text-xs font-semibold text-[#5A6258] hover:text-[#292D28] hover:bg-[#EFECE4] transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-[#56664C]' : ''}`} />
            Refresh
          </button>
          <button
            onClick={() => setShowModal(true)}
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-[#56664C] text-[#F4F2E9] text-xs font-semibold hover:bg-[#485640] transition-colors shadow-sm"
          >
            <Plus className="w-4 h-4" />
            Create Project
          </button>
        </div>
      </div>

      {error && (
        <div className="p-3.5 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs">
          {error}
        </div>
      )}

      {/* Projects Table */}
      <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#F4F2E9] border-b border-[#E2E0D8] text-[#5A6258] font-semibold uppercase tracking-wider">
              <tr>
                <th className="px-6 py-3.5">Code & Name</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5">Health</th>
                <th className="px-6 py-3.5">Target Due</th>
                <th className="px-6 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#E2E0D8] text-[#292D28]">
              {projects.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-[#5A6258]">
                    <div className="w-10 h-10 mx-auto mb-2 rounded-full bg-[#E2E0D8] text-[#56664C] flex items-center justify-center">
                      <FolderKanban className="w-5 h-5" />
                    </div>
                    No projects found. Create a project to start managing tasks and risks.
                  </td>
                </tr>
              ) : (
                projects.map((p) => (
                  <tr key={p.id} className="hover:bg-[#F4F2E9]/60 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-[#56664C]/10 text-[#56664C] flex items-center justify-center font-mono font-bold text-xs">
                          {p.code ? p.code.substring(0, 3) : 'PRJ'}
                        </div>
                        <div>
                          <div className="font-semibold text-[#292D28]">{p.name}</div>
                          <div className="text-[11px] text-[#78866B] font-mono">{p.code}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className="inline-flex px-2 py-0.5 rounded text-[10px] font-mono font-semibold bg-[#E2E0D8] text-[#292D28]">
                        {p.status}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      {getHealthBadge(p.health)}
                    </td>
                    <td className="px-6 py-4 font-mono text-[11px] text-[#78866B]">
                      {p.targetDate ? (
                        <div className="flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5" />
                          {new Date(p.targetDate).toLocaleDateString()}
                        </div>
                      ) : (
                        'No deadline set'
                      )}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Link
                        to={`/tasks?projectId=${p.id}`}
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-[#E2E0D8] bg-white hover:bg-[#EFECE4] text-[#292D28] font-semibold text-xs transition-colors"
                      >
                        Explore Tasks
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
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl max-w-md w-full p-6 shadow-xl space-y-4">
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
                  className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
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
                  placeholder="e.g. Next-Gen Mobile App"
                  value={name}
                  onChange={(e) => handleNameChange(e.target.value)}
                  className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs text-[#292D28] focus:outline-none focus:border-[#56664C]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                  Project Code / Key
                </label>
                <input
                  type="text"
                  required
                  placeholder="MOB-APP"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs font-mono text-[#292D28] focus:outline-none focus:border-[#56664C]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                  Description
                </label>
                <textarea
                  rows={3}
                  placeholder="Project objectives and scope details."
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
