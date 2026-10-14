import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { UserProfile, Role } from '../types';
import { Users, Search, ShieldCheck, ShieldAlert, CheckCircle2, XCircle, RefreshCw, Edit3 } from 'lucide-react';

export const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Edit Role Modal State
  const [selectedUser, setSelectedUser] = useState<UserProfile | null>(null);
  const [newRole, setNewRole] = useState<Role>('MEMBER');
  const [updating, setUpdating] = useState(false);

  const fetchUsers = async () => {
    setLoading(true);
    setError(null);
    try {
      const resp = await api.getUsers(query, 0, 50);
      setUsers(resp.content || []);
      setTotal(resp.totalElements || (resp.content ? resp.content.length : 0));
    } catch (err: any) {
      setError(err.message || 'Failed to fetch users');
      // Fallback display if dev server offline
      setUsers([
        {
          id: '00000000-0000-0000-0000-000000000001',
          email: 'admin@projectflow.io',
          fullName: 'System Administrator',
          role: 'ADMINISTRATOR',
          active: true,
          createdAt: new Date().toISOString()
        }
      ]);
      setTotal(1);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [query]);

  const handleRoleChange = async () => {
    if (!selectedUser) return;
    setUpdating(true);
    try {
      const updated = await api.updateUserRole(selectedUser.id, newRole);
      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
      setSelectedUser(null);
    } catch (err: any) {
      alert(`Role change failed: ${err.message}`);
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Page Title */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-[#292D28] tracking-tight">User Directory</h2>
          <p className="text-sm text-[#78866B]">
            Global role assignment, account status, and tenant authorization across ProjectFlow.
          </p>
        </div>
        <button
          onClick={fetchUsers}
          className="flex items-center gap-2 px-3.5 py-2 rounded-lg bg-[#FAFAF7] border border-[#E2E0D8] text-xs font-semibold text-[#5A6258] hover:text-[#292D28] hover:bg-[#EFECE4] transition-colors"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-[#56664C]' : ''}`} />
          Refresh
        </button>
      </div>

      {error && (
        <div className="p-3.5 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs">
          {error}
        </div>
      )}

      {/* Filter and Search */}
      <div className="bg-[#FAFAF7] border border-[#E2E0D8] p-4 rounded-xl flex items-center justify-between gap-4">
        <div className="relative flex-1 max-w-md">
          <Search className="w-4 h-4 text-[#78866B] absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search users by name or email..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs text-[#292D28] focus:outline-none focus:border-[#56664C] focus:ring-1 focus:ring-[#56664C]"
          />
        </div>
        <div className="text-xs font-medium text-[#78866B]">
          Showing {users.length} of {total} accounts
        </div>
      </div>

      {/* Users Table */}
      <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#F4F2E9] border-b border-[#E2E0D8] text-[#5A6258] font-semibold uppercase tracking-wider">
              <tr>
                <th className="px-6 py-3.5">User</th>
                <th className="px-6 py-3.5">Global Role</th>
                <th className="px-6 py-3.5">Account Status</th>
                <th className="px-6 py-3.5">Registered</th>
                <th className="px-6 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#E2E0D8] text-[#292D28]">
              {users.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-[#5A6258]">
                    No users match search criteria.
                  </td>
                </tr>
              ) : (
                users.map((u) => (
                  <tr key={u.id} className="hover:bg-[#F4F2E9]/60 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-[#E2E0D8] text-[#56664C] flex items-center justify-center font-bold text-xs">
                          {u.fullName ? u.fullName.charAt(0).toUpperCase() : u.email.charAt(0).toUpperCase()}
                        </div>
                        <div>
                          <div className="font-semibold text-[#292D28]">{u.fullName || 'Unnamed User'}</div>
                          <div className="text-[11px] text-[#78866B] font-mono">{u.email}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span
                        className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded text-[11px] font-semibold ${
                          u.role === 'ADMINISTRATOR'
                            ? 'bg-[#56664C]/15 text-[#56664C]'
                            : u.role === 'MANAGER'
                            ? 'bg-[#78866B]/20 text-[#56664C]'
                            : 'bg-[#E2E0D8] text-[#5A6258]'
                        }`}
                      >
                        <ShieldCheck className="w-3 h-3" />
                        {u.role}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      {u.active ? (
                        <span className="inline-flex items-center gap-1 text-[11px] text-[#56664C] font-medium">
                          <CheckCircle2 className="w-3.5 h-3.5" /> Active
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-[11px] text-[#C84B31] font-medium">
                          <XCircle className="w-3.5 h-3.5" /> Deactivated
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 font-mono text-[11px] text-[#78866B]">
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : 'Initial Setup'}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => {
                          setSelectedUser(u);
                          setNewRole(u.role);
                        }}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-[#E2E0D8] bg-white hover:bg-[#EFECE4] text-[#292D28] font-semibold text-xs transition-colors"
                      >
                        <Edit3 className="w-3 h-3 text-[#56664C]" />
                        Edit Role
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Edit Role Modal */}
      {selectedUser && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl max-w-sm w-full p-6 shadow-xl space-y-4">
            <h3 className="text-base font-bold text-[#292D28]">Edit Global User Role</h3>
            <p className="text-xs text-[#5A6258]">
              Assigning new privileges for <span className="font-semibold text-[#292D28]">{selectedUser.email}</span>.
            </p>

            <div>
              <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                Select Role
              </label>
              <select
                value={newRole}
                onChange={(e) => setNewRole(e.target.value as Role)}
                className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs font-semibold text-[#292D28] focus:outline-none focus:border-[#56664C]"
              >
                <option value="ADMINISTRATOR">ADMINISTRATOR (Full platform control)</option>
                <option value="MANAGER">MANAGER (Project & scenario authority)</option>
                <option value="MEMBER">MEMBER (Standard task contributor)</option>
                <option value="VIEWER">VIEWER (Read-only observer)</option>
              </select>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => setSelectedUser(null)}
                className="px-3.5 py-1.5 rounded-lg border border-[#E2E0D8] text-xs font-semibold text-[#5A6258] hover:bg-[#EFECE4]"
              >
                Cancel
              </button>
              <button
                disabled={updating}
                onClick={handleRoleChange}
                className="px-4 py-1.5 rounded-lg bg-[#56664C] text-[#F4F2E9] text-xs font-semibold hover:bg-[#485640] disabled:opacity-50"
              >
                {updating ? 'Saving...' : 'Apply Role'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
