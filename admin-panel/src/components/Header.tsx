import React from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, ShieldCheck, User } from 'lucide-react';

export const Header: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <header className="h-16 bg-[#FAFAF7] border-b border-[#E2E0D8] px-8 flex items-center justify-between sticky top-0 z-10">
      <div className="flex items-center gap-3">
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-[#56664C]/10 text-[#56664C] border border-[#56664C]/20">
          <span className="w-2 h-2 rounded-full bg-[#56664C] animate-pulse"></span>
          Operational
        </span>
        <span className="text-xs text-[#78866B] font-mono">Workspace Isolation Enforced</span>
      </div>

      <div className="flex items-center gap-4">
        {user ? (
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-[#E2E0D8] text-[#56664C] flex items-center justify-center font-semibold text-xs border border-[#D5D2C7]">
                {user.fullName ? user.fullName.substring(0, 2).toUpperCase() : <User className="w-4 h-4" />}
              </div>
              <div className="text-left">
                <div className="text-xs font-semibold text-[#292D28]">{user.fullName || user.email}</div>
                <div className="text-[10px] flex items-center gap-1 font-medium text-[#78866B]">
                  <ShieldCheck className="w-3 h-3 text-[#56664C]" />
                  {user.role}
                </div>
              </div>
            </div>

            <button
              onClick={logout}
              title="Sign Out"
              className="p-1.5 rounded-lg text-[#5A6258] hover:text-[#C84B31] hover:bg-[#FBEAE5] transition-colors"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        ) : (
          <div className="text-xs text-[#5A6258]">Not authenticated</div>
        )}
      </div>
    </header>
  );
};
