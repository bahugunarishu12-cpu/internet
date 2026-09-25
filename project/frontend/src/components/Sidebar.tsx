import React from "react";
import { LayoutDashboard, Laptop, Globe, Search, Settings, ShieldCheck, LogOut } from "lucide-react";

interface SidebarProps {
  currentTab: string;
  setCurrentTab: (tab: string) => void;
  onLogout: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentTab, setCurrentTab, onLogout }) => {
  const menuItems = [
    { id: "dashboard", label: "Dashboard", icon: LayoutDashboard },
    { id: "devices", label: "Devices", icon: Laptop },
    { id: "network", label: "Network Activity", icon: Globe },
    { id: "searches", label: "Chrome Searches", icon: Search },
    { id: "settings", label: "Settings & Privacy", icon: Settings },
  ];

  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col justify-between p-4">
      <div>
        <div className="flex items-center gap-3 px-2 py-4 mb-6 border-b border-slate-800">
          <div className="p-2 bg-indigo-600 rounded-lg text-white">
            <ShieldCheck size={24} />
          </div>
          <div>
            <h1 className="text-sm font-bold text-white tracking-wide">NET & CHROME</h1>
            <p className="text-xs text-slate-400">Activity Monitor</p>
          </div>
        </div>

        <nav className="space-y-1">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const active = currentTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setCurrentTab(item.id)}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  active
                    ? "bg-indigo-600/20 text-indigo-400 border border-indigo-500/30"
                    : "text-slate-400 hover:bg-slate-800 hover:text-slate-200"
                }`}
              >
                <Icon size={18} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>
      </div>

      <div className="pt-4 border-t border-slate-800">
        <div className="px-3 py-2 bg-slate-950/60 rounded-lg mb-3">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            <span className="text-xs text-slate-300 font-medium">Privacy Guard Active</span>
          </div>
          <p className="text-[10px] text-slate-500 mt-1 leading-tight">
            Domains & Searches only. Zero HTTPS interception or PII.
          </p>
        </div>
        <button
          onClick={onLogout}
          className="w-full flex items-center gap-2 px-3 py-2 text-xs font-medium text-rose-400 hover:bg-rose-500/10 rounded-lg transition-colors"
        >
          <LogOut size={16} />
          <span>Sign Out Admin</span>
        </button>
      </div>
    </aside>
  );
};
