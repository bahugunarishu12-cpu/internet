import React, { useEffect, useState } from "react";
import { ChromeSearch } from "../types";
import { api } from "../services/api";
import { Search, RefreshCw, ShieldCheck, Laptop } from "lucide-react";

export const ChromeSearches: React.FC = () => {
  const [searches, setSearches] = useState<ChromeSearch[]>([]);
  const [queryFilter, setQueryFilter] = useState("");
  const [deviceFilter, setDeviceFilter] = useState("");
  const [engineFilter, setEngineFilter] = useState("");
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    setLoading(true);
    try {
      const data = await api.getChromeSearches({
        query: queryFilter || undefined,
        device_id: deviceFilter || undefined,
        search_engine: engineFilter || undefined
      });
      setSearches(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [engineFilter]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white">Authorized Chrome Searches</h2>
          <p className="text-sm text-slate-400">Explicit search query keywords received from paired Chrome extensions</p>
        </div>
        <button
          onClick={loadData}
          className="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-medium rounded-lg border border-slate-700 transition"
        >
          <RefreshCw size={16} className={loading ? "animate-spin" : ""} />
          <span>Refresh</span>
        </button>
      </div>

      <div className="bg-amber-950/30 border border-amber-500/20 rounded-xl p-3 flex items-center gap-3">
        <ShieldCheck size={18} className="text-amber-400 shrink-0" />
        <p className="text-xs text-amber-200/80">
          <strong>Explicit Search Only:</strong> Collected solely from user-consented extensions on search engine URLs. Never captures form fields, password inputs, personal email, or private messages.
        </p>
      </div>

      {/* Filters */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="relative">
          <Search size={16} className="absolute left-3 top-2.5 text-slate-500" />
          <input
            type="text"
            placeholder="Search keywords..."
            value={queryFilter}
            onChange={(e) => setQueryFilter(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && loadData()}
            className="w-full pl-9 pr-3 py-2 bg-slate-900 border border-slate-800 rounded-lg text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-amber-500"
          />
        </div>

        <div className="relative">
          <Laptop size={16} className="absolute left-3 top-2.5 text-slate-500" />
          <input
            type="text"
            placeholder="Device ID..."
            value={deviceFilter}
            onChange={(e) => setDeviceFilter(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && loadData()}
            className="w-full pl-9 pr-3 py-2 bg-slate-900 border border-slate-800 rounded-lg text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-amber-500"
          />
        </div>

        <div>
          <select
            value={engineFilter}
            onChange={(e) => setEngineFilter(e.target.value)}
            className="w-full px-3 py-2 bg-slate-900 border border-slate-800 rounded-lg text-sm text-slate-200 focus:outline-none focus:border-amber-500"
          >
            <option value="">All Search Engines</option>
            <option value="Google">Google</option>
            <option value="Bing">Bing</option>
            <option value="DuckDuckGo">DuckDuckGo</option>
            <option value="Yahoo">Yahoo</option>
            <option value="Ecosia">Ecosia</option>
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-950/40 text-slate-400 text-xs uppercase tracking-wider font-semibold">
                <th className="py-3 px-4">Search Query</th>
                <th className="py-3 px-4">Search Engine</th>
                <th className="py-3 px-4">Device ID</th>
                <th className="py-3 px-4">Browser</th>
                <th className="py-3 px-4">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {loading ? (
                <tr>
                  <td colSpan={5} className="text-center py-8 text-slate-500 text-sm">Loading searches...</td>
                </tr>
              ) : searches.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-center py-8 text-slate-500 text-sm">No Chrome searches recorded.</td>
                </tr>
              ) : (
                searches.map((item, idx) => (
                  <tr key={idx} className="hover:bg-slate-800/30 transition">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2">
                        <Search size={16} className="text-amber-400" />
                        <span className="text-slate-100 font-medium text-sm">&quot;{item.search_query}&quot;</span>
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <span className="px-2 py-0.5 bg-slate-800 text-amber-300 rounded font-mono text-xs font-semibold">
                        {item.search_engine}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-xs font-mono text-slate-300">{item.device_id}</td>
                    <td className="py-3 px-4 text-xs text-slate-400">{item.browser}</td>
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
