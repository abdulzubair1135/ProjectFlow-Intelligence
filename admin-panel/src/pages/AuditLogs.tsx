import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { AuditLog } from '../types';
import { Activity, RefreshCw, ChevronLeft, ChevronRight } from 'lucide-react';

export const AuditLogs: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchLogs = async (p: number) => {
    setLoading(true);
    setError(null);
    try {
      const resp = await api.getAuditEvents(p, 25);
      setLogs(resp.content || []);
      setTotalPages(resp.totalPages || 1);
      setTotalElements(resp.totalElements || 0);
    } catch (err: any) {
      setError(err.message || 'Failed to load audit logs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs(page);
  }, [page]);

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Title */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-[#292D28] tracking-tight">Security & Audit Trail</h2>
          <p className="text-sm text-[#78866B]">
            Tamper-evident logs of administrative actions, access escalations, and state transitions.
          </p>
        </div>
        <button
          onClick={() => fetchLogs(page)}
          className="flex items-center gap-2 px-3.5 py-2 rounded-lg bg-[#FAFAF7] border border-[#E2E0D8] text-xs font-semibold text-[#5A6258] hover:text-[#292D28] hover:bg-[#EFECE4] transition-colors"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-[#56664C]' : ''}`} />
          Refresh Logs
        </button>
      </div>

      {error && (
        <div className="p-3.5 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs">
          {error}
        </div>
      )}

      {/* Audit Table */}
      <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#F4F2E9] border-b border-[#E2E0D8] text-[#5A6258] font-semibold uppercase tracking-wider">
              <tr>
                <th className="px-6 py-3.5">Timestamp</th>
                <th className="px-6 py-3.5">Action</th>
                <th className="px-6 py-3.5">Target Entity</th>
                <th className="px-6 py-3.5">Actor ID</th>
                <th className="px-6 py-3.5">Metadata / Payload</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#E2E0D8] text-[#292D28]">
              {logs.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-[#5A6258]">
                    <div className="w-10 h-10 mx-auto mb-2 rounded-full bg-[#E2E0D8] text-[#56664C] flex items-center justify-center">
                      <Activity className="w-5 h-5" />
                    </div>
                    No audit records recorded yet.
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="hover:bg-[#F4F2E9]/60 transition-colors">
                    <td className="px-6 py-4 font-mono text-[11px] text-[#78866B] whitespace-nowrap">
                      {new Date(log.timestamp).toLocaleString()}
                    </td>
                    <td className="px-6 py-4">
                      <span className="font-mono text-[11px] font-bold px-2 py-0.5 rounded bg-[#56664C]/10 text-[#56664C]">
                        {log.action}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="font-semibold text-[#292D28]">{log.entityType}</div>
                      {log.entityId && (
                        <div className="font-mono text-[10px] text-[#78866B] truncate max-w-xs">
                          {log.entityId}
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 font-mono text-[11px] text-[#78866B]">
                      {log.actorId ? log.actorId.substring(0, 8) + '...' : 'SYSTEM'}
                    </td>
                    <td className="px-6 py-4">
                      <span className="font-mono text-[10px] text-[#5A6258] bg-white px-2 py-1 rounded border border-[#E2E0D8] inline-block max-w-sm truncate">
                        {log.metadata || '{}'}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="px-6 py-4 border-t border-[#E2E0D8] flex items-center justify-between text-xs text-[#5A6258]">
          <div>
            Showing page <span className="font-semibold text-[#292D28]">{page + 1}</span> of{' '}
            <span className="font-semibold text-[#292D28]">{totalPages}</span> ({totalElements} total entries)
          </div>
          <div className="flex items-center gap-2">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="p-1.5 rounded-lg border border-[#E2E0D8] hover:bg-[#EFECE4] disabled:opacity-40"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="p-1.5 rounded-lg border border-[#E2E0D8] hover:bg-[#EFECE4] disabled:opacity-40"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
