import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck, UserCheck, ShieldAlert } from 'lucide-react';

export const Login: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('admin@projectflow.io');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await login(email);
      navigate('/');
    } catch (err: any) {
      setError(err.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickLogin = async (targetEmail: string) => {
    setEmail(targetEmail);
    setLoading(true);
    setError(null);
    try {
      await login(targetEmail);
      navigate('/');
    } catch (err: any) {
      setError(err.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F4F2E9] flex flex-col items-center justify-center p-4 select-none">
      <div className="w-full max-w-md bg-[#FAFAF7] border border-[#E2E0D8] rounded-xl shadow-lg p-8">
        <div className="flex items-center gap-3 mb-6">
          <img src="/logo.jpg" alt="ProjectFlow Logo" style={{ width: '48px', height: '48px', maxWidth: '48px', maxHeight: '48px' }} className="w-12 h-12 rounded-xl object-contain bg-white border border-[#E2E0D8] shadow-sm shrink-0" />
          <div>
            <h1 className="text-xl font-bold text-[#292D28]">ProjectFlow Intelligence</h1>
            <p className="text-xs text-[#78866B] font-medium uppercase tracking-wider">Admin Control Panel</p>
          </div>
        </div>

        {error && (
          <div className="mb-5 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-[#5A6258] uppercase tracking-wider mb-1">
              Account Email
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="e.g. admin@projectflow.io"
              className="w-full px-3 py-2 bg-white border border-[#E2E0D8] rounded-lg text-sm text-[#292D28] focus:outline-none focus:border-[#56664C] focus:ring-1 focus:ring-[#56664C]"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 px-4 rounded-lg bg-[#56664C] hover:bg-[#485640] text-[#F4F2E9] text-sm font-semibold transition-colors disabled:opacity-50"
          >
            {loading ? 'Authenticating...' : 'Sign In to Console'}
          </button>
        </form>

        <div className="mt-8 pt-6 border-t border-[#E2E0D8]">
          <p className="text-xs font-medium text-[#78866B] uppercase tracking-wider mb-3 text-center">
            Quick Dev Sign-In
          </p>
          <div className="space-y-2">
            <button
              onClick={() => handleQuickLogin('admin@projectflow.io')}
              className="w-full flex items-center justify-between p-2.5 rounded-lg border border-[#E2E0D8] bg-[#F4F2E9]/70 hover:bg-[#EFECE4] transition-colors text-left"
            >
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-[#56664C]" />
                <div>
                  <div className="text-xs font-semibold text-[#292D28]">System Administrator</div>
                  <div className="text-[11px] text-[#5A6258]">admin@projectflow.io</div>
                </div>
              </div>
              <span className="text-[10px] font-mono font-semibold bg-[#56664C]/10 text-[#56664C] px-2 py-0.5 rounded">
                ADMIN
              </span>
            </button>

            <button
              onClick={() => handleQuickLogin('manager@projectflow.io')}
              className="w-full flex items-center justify-between p-2.5 rounded-lg border border-[#E2E0D8] bg-[#F4F2E9]/70 hover:bg-[#EFECE4] transition-colors text-left"
            >
              <div className="flex items-center gap-2">
                <UserCheck className="w-4 h-4 text-[#78866B]" />
                <div>
                  <div className="text-xs font-semibold text-[#292D28]">Project Manager</div>
                  <div className="text-[11px] text-[#5A6258]">manager@projectflow.io</div>
                </div>
              </div>
              <span className="text-[10px] font-mono font-semibold bg-[#78866B]/15 text-[#78866B] px-2 py-0.5 rounded">
                MANAGER
              </span>
            </button>

            <button
              onClick={() => handleQuickLogin('member@projectflow.io')}
              className="w-full flex items-center justify-between p-2.5 rounded-lg border border-[#E2E0D8] bg-[#F4F2E9]/70 hover:bg-[#EFECE4] transition-colors text-left"
            >
              <div className="flex items-center gap-2">
                <ShieldAlert className="w-4 h-4 text-[#5A6258]" />
                <div>
                  <div className="text-xs font-semibold text-[#292D28]">Team Member</div>
                  <div className="text-[11px] text-[#5A6258]">member@projectflow.io</div>
                </div>
              </div>
              <span className="text-[10px] font-mono font-semibold bg-[#E2E0D8] text-[#5A6258] px-2 py-0.5 rounded">
                MEMBER
              </span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
