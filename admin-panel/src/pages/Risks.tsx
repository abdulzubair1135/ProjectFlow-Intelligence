import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Project, RiskFinding, RiskSeverity } from '../types';
import { ShieldAlert, Play, RefreshCw, AlertTriangle, ShieldCheck, ArrowRight, CheckCircle2 } from 'lucide-react';

export const Risks: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string>('');
  const [risks, setRisks] = useState<RiskFinding[]>([]);
  const [loading, setLoading] = useState(true);
  const [evaluating, setEvaluating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadProjects() {
      try {
        const prjs = await api.getProjects();
        setProjects(prjs);
        if (prjs.length > 0) {
          setSelectedProjectId(prjs[0].id);
        } else {
          setLoading(false);
        }
      } catch (err: any) {
        console.warn('Failed to load projects', err);
        setLoading(false);
      }
    }
    loadProjects();
  }, []);

  const fetchRisks = async (projId: string) => {
    if (!projId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.getProjectRisks(projId);
      setRisks(data || []);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch risk radar findings');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedProjectId) {
      fetchRisks(selectedProjectId);
    }
  }, [selectedProjectId]);

  const handleRunEvaluation = async () => {
    if (!selectedProjectId) return;
    setEvaluating(true);
    try {
      const results = await api.evaluateRisks(selectedProjectId);
      setRisks(results || []);
    } catch (err: any) {
      alert(`Evaluation failed: ${err.message}`);
    } finally {
      setEvaluating(false);
    }
  };

  const getSeverityBadge = (s: RiskSeverity) => {
    switch (s) {
      case 'CRITICAL':
        return <span className="bg-[#C84B31]/15 text-[#C84B31] px-2.5 py-0.5 rounded text-[11px] font-mono font-bold">CRITICAL</span>;
      case 'HIGH':
        return <span className="bg-[#D97706]/15 text-[#D97706] px-2.5 py-0.5 rounded text-[11px] font-mono font-bold">HIGH</span>;
      case 'MEDIUM':
        return <span className="bg-[#56664C]/15 text-[#56664C] px-2.5 py-0.5 rounded text-[11px] font-mono font-semibold">MEDIUM</span>;
      default:
        return <span className="bg-[#E2E0D8] text-[#5A6258] px-2.5 py-0.5 rounded text-[11px] font-mono">LOW</span>;
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      {/* Title & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-[#292D28] tracking-tight">Deterministic Risk Radar</h2>
          <p className="text-sm text-[#78866B]">
            Automated analysis evaluating 6 deterministic rules without subjective score fabrication.
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
            onClick={() => fetchRisks(selectedProjectId)}
            className="flex items-center gap-2 px-3.5 py-2 rounded-lg bg-[#FAFAF7] border border-[#E2E0D8] text-xs font-semibold text-[#5A6258] hover:text-[#292D28] hover:bg-[#EFECE4] transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-[#56664C]' : ''}`} />
          </button>

          <button
            disabled={!selectedProjectId || evaluating}
            onClick={handleRunEvaluation}
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-[#56664C] text-[#F4F2E9] text-xs font-semibold hover:bg-[#485640] transition-colors shadow-sm disabled:opacity-50"
          >
            <Play className={`w-3.5 h-3.5 fill-current ${evaluating ? 'animate-spin' : ''}`} />
            {evaluating ? 'Evaluating Rules...' : 'Run Risk Radar'}
          </button>
        </div>
      </div>

      {error && (
        <div className="p-3.5 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs">
          {error}
        </div>
      )}

      {/* 6 Rules Explanation Strip */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        {[
          { name: 'Overdue Task', id: 'OVERDUE_TASK' },
          { name: 'Deadline Approaching', id: 'DEADLINE_APPROACHING' },
          { name: 'Blocked Task', id: 'BLOCKED_TASK' },
          { name: 'Stalled Progress', id: 'STALLED_PROGRESS' },
          { name: 'Capacity Overload', id: 'CAPACITY_OVERLOAD' },
          { name: 'Project Deadline', id: 'PROJECT_DEADLINE_RISK' },
        ].map((rule) => {
          const matchCount = risks.filter((r) => r.ruleId === rule.id).length;
          return (
            <div
              key={rule.id}
              className={`p-3 rounded-lg border text-left transition-colors ${
                matchCount > 0
                  ? 'bg-red-50/50 border-red-200 text-[#C84B31]'
                  : 'bg-[#FAFAF7] border-[#E2E0D8] text-[#5A6258]'
              }`}
            >
              <div className="text-[10px] uppercase font-semibold font-mono tracking-wider truncate">
                {rule.name}
              </div>
              <div className="text-lg font-bold mt-1 text-[#292D28]">{matchCount}</div>
            </div>
          );
        })}
      </div>

      {/* Findings List */}
      <div className="space-y-4">
        {risks.length === 0 ? (
          <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl p-12 text-center space-y-3">
            <div className="w-12 h-12 mx-auto rounded-full bg-[#56664C]/10 text-[#56664C] flex items-center justify-center">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-[#292D28]">No Active Risk Findings</h3>
            <p className="text-xs text-[#5A6258] max-w-md mx-auto">
              All tasks in this project comply with scheduled deadlines and dependency constraints.
            </p>
          </div>
        ) : (
          risks.map((finding, idx) => (
            <div
              key={idx}
              className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl p-6 shadow-sm hover:border-[#56664C] transition-colors"
            >
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 mb-3">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    {getSeverityBadge(finding.severity)}
                    <span className="font-mono text-xs font-semibold text-[#78866B]">
                      {finding.ruleId}
                    </span>
                  </div>
                  <h3 className="text-base font-bold text-[#292D28]">{finding.title}</h3>
                </div>
                {finding.detectedAt && (
                  <span className="font-mono text-[11px] text-[#78866B]">
                    {new Date(finding.detectedAt).toLocaleString()}
                  </span>
                )}
              </div>

              <p className="text-xs text-[#5A6258] leading-relaxed mb-4">
                {finding.description}
              </p>

              {finding.recommendedAction && (
                <div className="p-3 bg-[#F4F2E9] rounded-lg border border-[#E2E0D8] flex items-start gap-2 text-xs">
                  <ArrowRight className="w-4 h-4 text-[#56664C] shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold text-[#292D28]">Recommended Action: </span>
                    <span className="text-[#5A6258]">{finding.recommendedAction}</span>
                  </div>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};
