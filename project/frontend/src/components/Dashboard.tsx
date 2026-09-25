import React, { useEffect, useState } from "react";
import { DashboardStats, NetworkActivity, ChromeSearch } from "../types";
import { api } from "../services/api";
import { Laptop, Wifi, WifiOff, Globe, Search, RefreshCw, AlertTriangle, ArrowUpRight, Router, Database } from "lucide-react";

interface DashboardProps {
  onSelectDevice: (deviceId: string) => void;
  onNavigate: (tab: string) => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ onSelectDevice, onNavigate }) => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recentNetwork, setRecentNetwork] = useState<NetworkActivity[]>([]);
  const [recentSearches, setRecentSearches] = useState<ChromeSearch[]>([]);
  const [systemStatus, setSystemStatus] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadData = async () => {
    try {
      const [s, net, searches, sys] = await Promise.all([
        api.getStats().catch(err => {
          setErrorMessage(err.message);
          return null;
        }),
        api.getNetworkActivity().catch(() => []),
        api.getChromeSearches().catch(() => []),
        api.getSystemStatus().catch(err => ({
          database_connected: false,
          database_error: "Database connection unavailable.",
          discovery: { error_message: "Cannot reach backend server" }
        }))
      ]);

      if (s) {
        setStats(s);
        setErrorMessage(null);
      }
      setRecentNetwork(net.slice(0, 6));
      setRecentSearches(searches.slice(0, 6));
      setSystemStatus(sys);
    } catch (e: any) {
      setErrorMessage(e.message || "Unable to reach backend");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // Poll every 10 seconds for real-time telemetry
    const interval = setInterval(loadData, 10000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white">Network & Chrome Activity Overview</h2>
          <p className="text-sm text-slate-400">Live network telemetry and explicit Chrome search logs (Auto-polls every 10s)</p>
        </div>
        <button
          onClick={loadData}
          disabled={loading}
          className="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-medium rounded-lg border border-slate-700 transition"
        >
          <RefreshCw size={16} className={loading ? "animate-spin" : ""} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Database Error Banner */}
      {systemStatus && (!systemStatus.database_connected || systemStatus.database_error) && (
        <div className="bg-rose-950/60 border border-rose-500/40 rounded-xl p-4 flex items-start gap-3">
          <Database size={20} className="text-rose-400 shrink-0 mt-0.5" />
          <div className="text-xs text-rose-200 leading-relaxed">
            <strong className="block text-rose-300 font-bold mb-0.5">Database connection unavailable.</strong>
            Cannot persist or retrieve network activity. Ensure MongoDB is running and reachable by the backend.
          </div>
        </div>
      )}

      {/* Router Integration Error Banner */}
      {systemStatus?.discovery?.error_message && (
        <div className="bg-amber-950/50 border border-amber-500/40 rounded-xl p-4 flex items-start gap-3">
          <Router size={20} className="text-amber-400 shrink-0 mt-0.5" />
          <div className="text-xs text-amber-200 leading-relaxed">
            <strong className="block text-amber-300 font-bold mb-0.5">Router / Discovery Integration Required</strong>
            {systemStatus.discovery.error_message}
          </div>
        </div>
      )}

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-medium uppercase tracking-wider">Total Devices</span>
            <Laptop size={18} className="text-slate-400" />
          </div>
          <div className="text-2xl font-bold text-white">{stats?.total_devices ?? 0}</div>
          <p className="text-[11px] text-slate-500 mt-1">Discovered hosts</p>
        </div>

        <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-medium uppercase tracking-wider">Online</span>
            <Wifi size={18} className="text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-emerald-400">{stats?.online_devices ?? 0}</div>
          <p className="text-[11px] text-emerald-500/70 mt-1">Detected active</p>
        </div>

        <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-medium uppercase tracking-wider">Offline</span>
            <WifiOff size={18} className="text-slate-500" />
          </div>
          <div className="text-2xl font-bold text-slate-400">{stats?.offline_devices ?? 0}</div>
          <p className="text-[11px] text-slate-500 mt-1">Known but absent</p>
        </div>

        <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-medium uppercase tracking-wider">Domains</span>
            <Globe size={18} className="text-cyan-400" />
          </div>
          <div className="text-2xl font-bold text-cyan-400">{stats?.domain_activity_count ?? 0}</div>
          <p className="text-[11px] text-slate-500 mt-1">Network events</p>
        </div>

        <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-medium uppercase tracking-wider">Searches</span>
            <Search size={18} className="text-amber-400" />
          </div>
          <div className="text-2xl font-bold text-amber-400">{stats?.chrome_search_count ?? 0}</div>
          <p className="text-[11px] text-slate-500 mt-1">Extension searches</p>
        </div>
      </div>

      {/* Split Feeds: Network Activity & Chrome Searches */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Network Activity Table */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Globe size={18} className="text-cyan-400" />
              <h3 className="font-semibold text-white text-base">Network Domain Activity</h3>
            </div>
            <button
              onClick={() => onNavigate("network")}
              className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center gap-1"
            >
              <span>View All</span>
              <ArrowUpRight size={14} />
            </button>
          </div>

          <div className="space-y-2">
            {recentNetwork.length === 0 ? (
              <p className="text-xs text-slate-500 py-8 text-center">No domain activity recorded in database yet.</p>
            ) : (
              recentNetwork.map((net, i) => (
                <div
                  key={i}
                  className="flex items-center justify-between p-2.5 bg-slate-950/60 rounded-lg hover:bg-slate-800/50 transition cursor-pointer"
                  onClick={() => onSelectDevice(net.device_id)}
                >
                  <div className="flex items-center gap-3">
                    <span className="w-1.5 h-1.5 rounded-full bg-cyan-400"></span>
                    <div>
                      <div className="text-sm font-medium text-slate-200">{net.domain}</div>
                      <div className="text-xs text-slate-500">{net.ip_address} • {net.device_id}</div>
                    </div>
                  </div>
                  <div className="text-[11px] text-slate-500 font-mono">
                    {new Date(net.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Chrome Searches Feed */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Search size={18} className="text-amber-400" />
              <h3 className="font-semibold text-white text-base">Authorized Chrome Searches</h3>
            </div>
            <button
              onClick={() => onNavigate("searches")}
              className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center gap-1"
            >
              <span>View All</span>
              <ArrowUpRight size={14} />
            </button>
          </div>

          <div className="space-y-2">
            {recentSearches.length === 0 ? (
              <p className="text-xs text-slate-500 py-8 text-center">No Chrome search activity recorded yet.</p>
            ) : (
              recentSearches.map((s, i) => (
                <div
                  key={i}
                  className="flex items-center justify-between p-2.5 bg-slate-950/60 rounded-lg hover:bg-slate-800/50 transition cursor-pointer"
                  onClick={() => onSelectDevice(s.device_id)}
                >
                  <div className="flex items-center gap-3 overflow-hidden">
                    <span className="w-1.5 h-1.5 rounded-full bg-amber-400 shrink-0"></span>
                    <div className="truncate">
                      <div className="text-sm font-medium text-slate-200 truncate">
                        &quot;{s.search_query}&quot;
                      </div>
                      <div className="text-xs text-slate-500">
                        {s.search_engine} • {s.device_id}
                      </div>
                    </div>
                  </div>
                  <div className="text-[11px] text-slate-500 font-mono shrink-0 ml-2">
                    {new Date(s.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
