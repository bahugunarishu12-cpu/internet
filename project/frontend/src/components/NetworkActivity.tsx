import React, { useEffect, useState } from "react";
import { NetworkActivity as NetItem } from "../types";
import { api } from "../services/api";
import { Globe, Search, RefreshCw, ShieldCheck } from "lucide-react";

export const NetworkActivity: React.FC = () => {
  const [items, setItems] = useState<NetItem[]>([]);
  const [domainFilter, setDomainFilter] = useState("");
  const [deviceFilter, setDeviceFilter] = useState("");
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    setLoading(true);
    try {
      const data = await api.getNetworkActivity({
        domain: domainFilter || undefined,
        device_id: deviceFilter || undefined
      });
      setItems(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white">Network Domain Activity</h2>
          <p className="text-sm text-slate-400">DNS & domain-level destination queries (No payload or HTTPS decryption)</p>
        </div>
        <button
          onClick={loadData}
          className="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-medium rounded-lg border border-slate-700 transition"
        >
          <RefreshCw size={16} className={loading ? "animate-spin" : ""} />
          <span>Refresh</span>
        </button>
      </div>

      <div className="bg-cyan-950/30 border border-cyan-500/20 rounded-xl p-3 flex items-center gap-3">
        <ShieldCheck size={18} className="text-cyan-400 shrink-0" />
        <p className="text-xs text-cyan-200/80">
          <strong>Domain-Level Only:</strong> Records host addresses (e.g. <code>google.com</code>, <code>youtube.com</code>). URL parameters, session cookies, and message contents are never accessed.
        </p>
      </div>

      {/* Filters */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div className="relative">
          <Search size={16} className="absolute left-3 top-2.5 text-slate-500" />
          <input
            type="text"
            placeholder="Filter by domain..."
            value={domainFilter}
            onChange={(e) => setDomainFilter(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && loadData()}
            className="w-full pl-9 pr-3 py-2 bg-slate-900 border border-slate-800 rounded-lg text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500"
          />
        </div>
        <div className="relative">
          <Search size={16} className="absolute left-3 top-2.5 text-slate-500" />
          <input
            type="text"
            placeholder="Filter by device ID..."
            value={deviceFilter}
            onChange={(e) => setDeviceFilter(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && loadData()}
            className="w-full pl-9 pr-3 py-2 bg-slate-900 border border-slate-800 rounded-lg text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500"
          />
        </div>
      </div>

      {/* Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-950/40 text-slate-400 text-xs uppercase tracking-wider font-semibold">
                <th className="py-3 px-4">Domain Name</th>
                <th className="py-3 px-4">Device ID</th>
                <th className="py-3 px-4">Source IP</th>
                <th className="py-3 px-4">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {loading ? (
                <tr>
                  <td colSpan={4} className="text-center py-8 text-slate-500 text-sm">Loading activity...</td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={4} className="text-center py-8 text-slate-500 text-sm">No domain activity found.</td>
                </tr>
              ) : (
                items.map((item, idx) => (
                  <tr key={idx} className="hover:bg-slate-800/30 transition">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2">
                        <Globe size={16} className="text-cyan-400" />
                        <span className="font-mono text-cyan-300 font-medium text-sm">{item.domain}</span>
                      </div>
                    </td>
                    <td className="py-3 px-4 text-xs font-mono text-slate-300">{item.device_id}</td>
                    <td className="py-3 px-4 text-xs font-mono text-slate-400">{item.ip_address}</td>
                    <td className="py-3 px-4 text-xs text-slate-400 font-mono">
                      {new Date(item.timestamp).toLocaleString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
