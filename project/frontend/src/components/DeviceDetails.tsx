import React, { useEffect, useState } from "react";
import { CombinedActivity, PairingCode } from "../types";
import { api } from "../services/api";
import { ArrowLeft, Laptop, Globe, Search, Key, Wifi, WifiOff, RefreshCw, Copy, Check } from "lucide-react";

interface DeviceDetailsProps {
  deviceId: string;
  onBack: () => void;
}

export const DeviceDetails: React.FC<DeviceDetailsProps> = ({ deviceId, onBack }) => {
  const [data, setData] = useState<CombinedActivity | null>(null);
  const [loading, setLoading] = useState(true);
  const [filterDomain, setFilterDomain] = useState("");
  const [filterSearch, setFilterSearch] = useState("");
  const [pairingCode, setPairingCode] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const loadActivity = async () => {
    setLoading(true);
    try {
      const res = await api.getCombinedActivity(deviceId);
      setData(res);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadActivity();
  }, [deviceId]);

  const handleGeneratePairing = async () => {
    try {
      const p = await api.generatePairingCode(deviceId);
      setPairingCode(p.code);
      setCopied(false);
    } catch (err: any) {
      alert(err.message || "Failed to generate pairing code");
    }
  };

  const copyCode = () => {
    if (pairingCode) {
      navigator.clipboard.writeText(pairingCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  if (loading && !data) {
    return (
      <div className="py-20 text-center text-slate-500">
        <RefreshCw className="animate-spin inline-block mr-2" size={18} />
        Loading device activity...
      </div>
    );
  }

  if (!data) {
    return (
      <div className="text-center py-20">
        <p className="text-slate-400 mb-4">Device not found.</p>
        <button onClick={onBack} className="px-4 py-2 bg-slate-800 text-slate-200 rounded-lg">
          Back to Devices
        </button>
      </div>
    );
  }

  const { device, network_activity, chrome_searches } = data;
  const isOnline = device.status === "online";

  const filteredNetwork = network_activity.filter((n) =>
    n.domain.toLowerCase().includes(filterDomain.toLowerCase())
  );

  const filteredSearches = chrome_searches.filter((s) =>
    s.search_query.toLowerCase().includes(filterSearch.toLowerCase()) ||
    s.search_engine.toLowerCase().includes(filterSearch.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Top Header & Breadcrumb */}
      <div className="flex items-center justify-between">
        <button
          onClick={onBack}
          className="flex items-center gap-2 text-xs text-slate-400 hover:text-white transition"
        >
          <ArrowLeft size={16} />
          <span>Back to All Devices</span>
        </button>
        <button
          onClick={loadActivity}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs rounded-lg border border-slate-700"
        >
          <RefreshCw size={14} className={loading ? "animate-spin" : ""} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Device Overview Card */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <div className="flex items-center gap-4">
            <div className="p-3 bg-indigo-600/10 border border-indigo-500/20 text-indigo-400 rounded-xl">
              <Laptop size={32} />
            </div>
            <div>
              <div className="flex items-center gap-3">
                <h2 className="text-xl font-bold text-white">{device.device_name}</h2>
                <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                  isOnline
                    ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
                    : "bg-slate-800 text-slate-400"
                }`}>
                  {isOnline ? <Wifi size={12} /> : <WifiOff size={12} />}
                  <span className="capitalize">{device.status}</span>
                </span>
              </div>
              <p className="text-xs text-slate-400 font-mono mt-0.5">ID: {device.device_id}</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={handleGeneratePairing}
              className="flex items-center gap-2 px-3 py-2 bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 text-xs font-semibold rounded-lg transition"
            >
              <Key size={14} />
              <span>Generate Chrome Extension Pairing Code</span>
            </button>
          </div>
        </div>

        {pairingCode && (
          <div className="mt-4 p-3 bg-slate-950 border border-amber-500/40 rounded-lg flex items-center justify-between">
            <div className="text-xs text-slate-300">
              Active Pairing Code: <strong className="font-mono text-amber-400 text-sm">{pairingCode}</strong>
            </div>
            <button onClick={copyCode} className="text-xs text-slate-400 hover:text-white flex items-center gap-1">
              {copied ? <Check size={14} className="text-emerald-400" /> : <Copy size={14} />}
              <span>{copied ? "Copied" : "Copy"}</span>
            </button>
          </div>
        )}

        {/* Specs Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-6 pt-6 border-t border-slate-800">
          <div>
            <div className="text-xs text-slate-500">IP Address</div>
            <div className="text-sm font-mono text-slate-200 mt-1">{device.ip_address}</div>
          </div>
          <div>
            <div className="text-xs text-slate-500">MAC Address</div>
            <div className="text-sm font-mono text-slate-200 mt-1">{device.mac_address || "None"}</div>
          </div>
          <div>
            <div className="text-xs text-slate-500">First Seen</div>
            <div className="text-xs text-slate-300 mt-1">{new Date(device.first_seen).toLocaleString()}</div>
          </div>
          <div>
            <div className="text-xs text-slate-500">Last Seen</div>
            <div className="text-xs text-slate-300 mt-1">{new Date(device.last_seen).toLocaleString()}</div>
          </div>
        </div>
      </div>

      {/* Combined Activity Streams */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Network Domain Column */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Globe size={18} className="text-cyan-400" />
              <h3 className="font-bold text-white text-base">Network Domain Activity</h3>
            </div>
            <span className="text-xs bg-slate-800 text-slate-400 px-2 py-0.5 rounded">
              {filteredNetwork.length} events
            </span>
          </div>

          <input
            type="text"
            placeholder="Filter domains..."
            value={filterDomain}
            onChange={(e) => setFilterDomain(e.target.value)}
            className="w-full px-3 py-1.5 bg-slate-950 border border-slate-800 rounded-lg text-xs text-slate-200 placeholder-slate-500 focus:outline-none"
          />

          <div className="space-y-2 max-h-[400px] overflow-y-auto pr-1">
            {filteredNetwork.length === 0 ? (
              <p className="text-xs text-slate-500 text-center py-8">No domain events recorded.</p>
            ) : (
              filteredNetwork.map((item, idx) => (
                <div key={idx} className="p-3 bg-slate-950/60 rounded-lg border border-slate-800/40">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-cyan-300 text-sm font-semibold">{item.domain}</span>
                    <span className="text-[11px] text-slate-500">
                      {new Date(item.timestamp).toLocaleTimeString()}
                    </span>
                  </div>
                  <div className="text-[11px] text-slate-500 mt-1">
                    IP: {item.ip_address}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Chrome Searches Column */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Search size={18} className="text-amber-400" />
              <h3 className="font-bold text-white text-base">Chrome Search Queries</h3>
            </div>
            <span className="text-xs bg-slate-800 text-slate-400 px-2 py-0.5 rounded">
              {filteredSearches.length} queries
            </span>
          </div>

          <input
            type="text"
            placeholder="Filter search queries..."
            value={filterSearch}
            onChange={(e) => setFilterSearch(e.target.value)}
            className="w-full px-3 py-1.5 bg-slate-950 border border-slate-800 rounded-lg text-xs text-slate-200 placeholder-slate-500 focus:outline-none"
          />

          <div className="space-y-2 max-h-[400px] overflow-y-auto pr-1">
            {filteredSearches.length === 0 ? (
              <p className="text-xs text-slate-500 text-center py-8">No Chrome searches recorded.</p>
            ) : (
              filteredSearches.map((item, idx) => (
                <div key={idx} className="p-3 bg-slate-950/60 rounded-lg border border-slate-800/40">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-100 font-medium text-sm">
                      &quot;{item.search_query}&quot;
                    </span>
                    <span className="text-[11px] text-slate-500">
                      {new Date(item.timestamp).toLocaleTimeString()}
                    </span>
                  </div>
                  <div className="flex items-center gap-2 text-[11px] text-slate-400 mt-1">
                    <span className="px-1.5 py-0.5 bg-amber-500/10 text-amber-300 rounded font-mono text-[10px]">
                      {item.search_engine}
                    </span>
                    <span>{item.browser}</span>
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
