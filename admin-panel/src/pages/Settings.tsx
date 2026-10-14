import React, { useState } from 'react';
import { Settings as SettingsIcon, ShieldCheck, Database, RefreshCw, Save } from 'lucide-react';

export const Settings: React.FC = () => {
  const [syncInterval, setSyncInterval] = useState('15');
  const [maxBatchSize, setMaxBatchSize] = useState('100');
  const [offlineGracePeriod, setOfflineGracePeriod] = useState('72');
  const [saved, setSaved] = useState(false);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Title */}
      <div>
        <h2 className="text-2xl font-bold text-[#292D28] tracking-tight">System Configuration</h2>
        <p className="text-sm text-[#78866B]">
          Global engine thresholds, security rules, and offline synchronization parameters.
        </p>
      </div>

      {saved && (
        <div className="p-3.5 rounded-lg bg-[#56664C]/10 border border-[#56664C]/30 text-[#56664C] text-xs font-semibold">
          Configuration parameters updated successfully.
        </div>
      )}

      <form onSubmit={handleSave} className="space-y-6">
        {/* Security & Isolation */}
        <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl p-6 shadow-sm space-y-4">
          <div className="flex items-center gap-2 text-sm font-bold text-[#292D28]">
            <ShieldCheck className="w-4 h-4 text-[#56664C]" />
            Tenant Security & Access Rules
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div className="p-4 rounded-lg bg-white border border-[#E2E0D8]">
              <div className="font-semibold text-[#292D28]">Workspace Isolation</div>
              <div className="text-[#5A6258] mt-1">Cross-tenant queries are blocked at the repository layer.</div>
              <div className="mt-3 text-[11px] font-mono text-[#56664C] font-semibold">ACTIVE (STRICT)</div>
            </div>
            <div className="p-4 rounded-lg bg-white border border-[#E2E0D8]">
              <div className="font-semibold text-[#292D28]">Stateless JWT Authentication</div>
              <div className="text-[#5A6258] mt-1">Cryptographic token validation with Bearer token header.</div>
              <div className="mt-3 text-[11px] font-mono text-[#56664C] font-semibold">ENABLED</div>
            </div>
          </div>
        </div>

        {/* Sync & Offline Engine */}
        <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl p-6 shadow-sm space-y-4">
          <div className="flex items-center gap-2 text-sm font-bold text-[#292D28]">
            <RefreshCw className="w-4 h-4 text-[#56664C]" />
            Offline Synchronization Engine
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                Periodic Sync (Minutes)
              </label>
              <input
                type="number"
                min="5"
                max="120"
                value={syncInterval}
                onChange={(e) => setSyncInterval(e.target.value)}
                className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs font-mono text-[#292D28] focus:outline-none focus:border-[#56664C]"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                Batch Change Limit
              </label>
              <input
                type="number"
                min="10"
                max="500"
                value={maxBatchSize}
                onChange={(e) => setMaxBatchSize(e.target.value)}
                className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs font-mono text-[#292D28] focus:outline-none focus:border-[#56664C]"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
                Offline Grace (Hours)
              </label>
              <input
                type="number"
                min="24"
                max="720"
                value={offlineGracePeriod}
                onChange={(e) => setOfflineGracePeriod(e.target.value)}
                className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-xs font-mono text-[#292D28] focus:outline-none focus:border-[#56664C]"
              />
            </div>
          </div>
        </div>

        {/* Database Configuration */}
        <div className="bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl p-6 shadow-sm space-y-4">
          <div className="flex items-center gap-2 text-sm font-bold text-[#292D28]">
            <Database className="w-4 h-4 text-[#56664C]" />
            Database & Persistence Dialect
          </div>
          <div className="text-xs text-[#5A6258] space-y-2">
            <p>
              Primary: <span className="font-mono font-semibold text-[#292D28]">PostgreSQL 15+ / Supabase</span> with connection pooling and SSL mode.
            </p>
            <p>
              Local Dev / Test Mode: <span className="font-mono font-semibold text-[#292D28]">H2 in PostgreSQL compatibility mode</span> enabled when <code className="font-mono bg-[#E2E0D8] px-1 py-0.5 rounded">SUPABASE_DB_URL</code> is unset.
            </p>
          </div>
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            className="flex items-center gap-2 px-5 py-2.5 rounded-lg bg-[#56664C] text-[#F4F2E9] text-xs font-semibold hover:bg-[#485640] transition-colors shadow-sm"
          >
            <Save className="w-4 h-4" />
            Save Configuration
          </button>
        </div>
      </form>
    </div>
  );
};
