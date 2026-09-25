import React, { useEffect, useState } from "react";
import { Device, PairingCode } from "../types";
import { api } from "../services/api";
import { Laptop, Wifi, WifiOff, Key, Plus, Search, ExternalLink, Copy, Check, RefreshCw } from "lucide-react";

interface DevicesProps {
  onSelectDevice: (deviceId: string) => void;
}

export const Devices: React.FC<DevicesProps> = ({ onSelectDevice }) => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);

  // Register modal
  const [showRegisterModal, setShowRegisterModal] = useState(false);
  const [regDeviceId, setRegDeviceId] = useState("");
  const [regDeviceName, setRegDeviceName] = useState("");
  const [regIpAddress, setRegIpAddress] = useState("");
  const [regMacAddress, setRegMacAddress] = useState("");

  // Pairing modal
  const [pairingModalData, setPairingModalData] = useState<{ deviceId: string; code: string } | null>(null);
  const [copied, setCopied] = useState(false);

  const loadDevices = async () => {
    setLoading(true);
    try {
      const data = await api.getDevices({
        search: search || undefined,
        status: statusFilter || undefined
      });
      setDevices(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDevices();
    const interval = setInterval(loadDevices, 10000);
    return () => clearInterval(interval);
  }, [statusFilter]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    loadDevices();
  };

  const handleSyncNow = async () => {
    setSyncing(true);
    try {
      await api.triggerSync();
      await loadDevices();
    } catch (e: any) {
      alert(e.message || "Sync failed");
    } finally {
      setSyncing(false);
    }
  };

  const handleGeneratePairing = async (deviceId: string) => {
    try {
      const pairing = await api.generatePairingCode(deviceId);
      setPairingModalData({ deviceId, code: pairing.code });
      setCopied(false);
    } catch (err: any) {
      alert(err.message || "Failed to generate pairing code");
    }
  };

  const handleRegisterDevice = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.registerDevice({
        device_id: regDeviceId,
        device_name: regDeviceName,
        ip_address: regIpAddress,
        mac_address: regMacAddress || undefined
      });
      setShowRegisterModal(false);
      setRegDeviceId("");
      setRegDeviceName("");
      setRegIpAddress("");
      setRegMacAddress("");
      loadDevices();
    } catch (err: any) {
      alert(err.message || "Failed to register device");
    }
  };

  const copyPairingCode = () => {
    if (pairingModalData) {
      navigator.clipboard.writeText(pairingModalData.code);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white">Authorized Network Devices</h2>
          <p className="text-sm text-slate-400">Discovered hosts, IP addresses, and Chrome extension pairing codes (Auto-synced)</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={handleSyncNow}
            disabled={syncing}
            className="flex items-center gap-2 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-semibold rounded-lg border border-slate-700 transition"
          >
            <RefreshCw size={15} className={syncing ? "animate-spin" : ""} />
            <span>{syncing ? "Syncing..." : "Sync Network"}</span>
          </button>
          <button
            onClick={() => setShowRegisterModal(true)}
            className="flex items-center gap-2 px-3 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold rounded-lg shadow-sm transition"
          >
            <Plus size={16} />
            <span>Register Device</span>
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-col sm:flex-row gap-3 items-center justify-between">
        <form onSubmit={handleSearchSubmit} className="relative w-full sm:w-80">
          <Search size={16} className="absolute left-3 top-2.5 text-slate-500" />
          <input
            type="text"
            placeholder="Search by ID, name, or IP..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 bg-slate-900 border border-slate-800 rounded-lg text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500"
          />
        </form>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <span className="text-xs text-slate-400">Filter:</span>
          {["", "online", "offline"].map((status) => (
            <button
              key={status}
              onClick={() => setStatusFilter(status)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${
                statusFilter === status
                  ? "bg-slate-800 text-indigo-400 border border-indigo-500/40"
                  : "bg-slate-900 text-slate-400 hover:bg-slate-800"
              }`}
            >
              {status === "" ? "All Status" : status.toUpperCase()}
            </button>
          ))}
        </div>
      </div>

      {/* Device Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-950/40 text-slate-400 text-xs uppercase tracking-wider font-semibold">
                <th className="py-3 px-4">Device</th>
                <th className="py-3 px-4">IP Address</th>
                <th className="py-3 px-4">MAC Address</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Last Seen</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {loading ? (
                <tr>
                  <td colSpan={6} className="text-center py-8 text-slate-500 text-sm">Loading devices...</td>
                </tr>
              ) : devices.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-slate-500 text-sm">
                    No authorized devices found in database. Click &apos;Sync Network&apos; or register a device.
                  </td>
                </tr>
              ) : (
                devices.map((device) => {
                  const isOnline = device.status === "online";
                  return (
                    <tr key={device.device_id} className="hover:bg-slate-800/30 transition">
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-3">
                          <div className="p-2 bg-slate-800 rounded-lg text-slate-300">
                            <Laptop size={18} />
                          </div>
                          <div>
                            <div className="font-semibold text-slate-200">{device.device_name}</div>
                            <div className="text-xs text-slate-500 font-mono">{device.device_id}</div>
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 font-mono text-slate-300 text-xs">
                        {device.ip_address}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-slate-400 text-xs">
                        {device.mac_address || "—"}
                      </td>
                      <td className="py-3.5 px-4">
                        <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${
                          isOnline
                            ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
                            : "bg-slate-800 text-slate-400"
                        }`}>
                          {isOnline ? <Wifi size={12} /> : <WifiOff size={12} />}
                          <span className="uppercase">{device.status}</span>
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-slate-400 text-xs">
                        {new Date(device.last_seen).toLocaleString()}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleGeneratePairing(device.device_id)}
                            title="Generate Chrome Extension Pairing Code"
                            className="p-1.5 bg-slate-800 hover:bg-slate-700 text-amber-400 hover:text-amber-300 rounded-lg border border-slate-700 text-xs flex items-center gap-1 transition"
                          >
                            <Key size={14} />
                            <span className="hidden md:inline">Pair</span>
                          </button>
                          <button
                            onClick={() => onSelectDevice(device.device_id)}
                            className="p-1.5 bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-400 rounded-lg border border-indigo-500/30 text-xs flex items-center gap-1 transition"
                          >
                            <span>Details</span>
                            <ExternalLink size={14} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Register Modal */}
      {showRegisterModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 w-full max-w-md shadow-2xl">
            <h3 className="text-lg font-bold text-white mb-2">Register Authorized Device</h3>
            <p className="text-xs text-slate-400 mb-4">
              Add a verified host to the authorized monitoring registry in MongoDB.
            </p>
            <form onSubmit={handleRegisterDevice} className="space-y-3">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Device ID *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. dev-laptop-01"
                  value={regDeviceId}
                  onChange={(e) => setRegDeviceId(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-200"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Device Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Workstation 01"
                  value={regDeviceName}
                  onChange={(e) => setRegDeviceName(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-200"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">IP Address *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. 192.168.1.50"
                  value={regIpAddress}
                  onChange={(e) => setRegIpAddress(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-200"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">MAC Address (optional)</label>
                <input
                  type="text"
                  placeholder="e.g. 00:1A:2B:3C:4D:5E"
                  value={regMacAddress}
                  onChange={(e) => setRegMacAddress(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-200"
                />
              </div>

              <div className="flex justify-end gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setShowRegisterModal(false)}
                  className="px-4 py-2 bg-slate-800 text-slate-300 rounded-lg text-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 text-white font-medium rounded-lg text-sm"
                >
                  Save Device
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Pairing Code Modal */}
      {pairingModalData && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 w-full max-w-sm text-center shadow-2xl">
            <div className="w-12 h-12 bg-amber-500/10 border border-amber-500/20 text-amber-400 rounded-full flex items-center justify-center mx-auto mb-3">
              <Key size={24} />
            </div>
            <h3 className="text-lg font-bold text-white mb-1">Chrome Extension Pairing Code</h3>
            <p className="text-xs text-slate-400 mb-4">
              Enter this single-use code in the user&apos;s Chrome extension popup to link it with <strong className="text-slate-200">{pairingModalData.deviceId}</strong>.
            </p>

            <div className="p-3 bg-slate-950 border border-dashed border-amber-500/40 rounded-lg flex items-center justify-between gap-2 mb-4">
              <span className="font-mono text-xl font-bold tracking-widest text-amber-400">
                {pairingModalData.code}
              </span>
              <button
                onClick={copyPairingCode}
                className="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded"
              >
                {copied ? <Check size={16} className="text-emerald-400" /> : <Copy size={16} />}
              </button>
            </div>

            <p className="text-[11px] text-slate-500 mb-5">
              Code valid for 24 hours in MongoDB. No passwords or cookies are shared.
            </p>

            <button
              onClick={() => setPairingModalData(null)}
              className="w-full py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-medium rounded-lg"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
