import React, { useState } from "react";
import { Sidebar } from "./components/Sidebar";
import { Dashboard } from "./components/Dashboard";
import { Devices } from "./components/Devices";
import { DeviceDetails } from "./components/DeviceDetails";
import { NetworkActivity } from "./components/NetworkActivity";
import { ChromeSearches } from "./components/ChromeSearches";
import { Settings } from "./components/Settings";
import { api, getAuthToken, setAuthToken } from "./services/api";
import { ShieldCheck, Lock, User, KeyRound } from "lucide-react";

export const App: React.FC = () => {
  const [token, setToken] = useState<string | null>(getAuthToken());
  const [currentTab, setCurrentTab] = useState<string>("dashboard");
  const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(null);

  // Login form state
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("admin123");
  const [loginError, setLoginError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setLoginError("");
    try {
      await api.login(username, password);
      setToken(getAuthToken());
    } catch (err: any) {
      setLoginError(err.message || "Invalid credentials");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleLogout = () => {
    setAuthToken(null);
    setToken(null);
    setSelectedDeviceId(null);
  };

  const handleSelectDevice = (deviceId: string) => {
    setSelectedDeviceId(deviceId);
    setCurrentTab("device_details");
  };

  if (!token) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4">
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 w-full max-w-md shadow-2xl">
          <div className="text-center mb-6">
            <div className="w-14 h-14 bg-indigo-600/10 border border-indigo-500/20 text-indigo-400 rounded-2xl flex items-center justify-center mx-auto mb-3">
              <ShieldCheck size={32} />
            </div>
            <h1 className="text-xl font-bold text-white tracking-tight">Admin Authentication</h1>
            <p className="text-xs text-slate-400 mt-1">Network & Chrome Activity Monitor Portal</p>
          </div>

          {loginError && (
            <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/20 rounded-lg text-xs text-rose-400">
              {loginError}
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">Username</label>
              <div className="relative">
                <User size={16} className="absolute left-3 top-2.5 text-slate-500" />
                <input
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">Password</label>
              <div className="relative">
                <Lock size={16} className="absolute left-3 top-2.5 text-slate-500" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm rounded-lg shadow-md transition disabled:opacity-50"
            >
              {isSubmitting ? "Authenticating..." : "Sign In to Admin Console"}
            </button>
          </form>

          <div className="mt-6 pt-4 border-t border-slate-800/80 text-center">
            <p className="text-[11px] text-slate-500">
              Default credentials: <span className="font-mono text-slate-400">admin</span> / <span className="font-mono text-slate-400">admin123</span>
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-slate-950 text-slate-100 overflow-hidden font-sans">
      <Sidebar
        currentTab={currentTab}
        setCurrentTab={(tab) => {
          setSelectedDeviceId(null);
          setCurrentTab(tab);
        }}
        onLogout={handleLogout}
      />

      <main className="flex-1 overflow-y-auto p-6 md:p-8">
        <div className="max-w-6xl mx-auto">
          {currentTab === "dashboard" && (
            <Dashboard
              onSelectDevice={handleSelectDevice}
              onNavigate={(tab) => setCurrentTab(tab)}
            />
          )}

          {currentTab === "devices" && (
            <Devices onSelectDevice={handleSelectDevice} />
          )}

          {currentTab === "device_details" && selectedDeviceId && (
            <DeviceDetails
              deviceId={selectedDeviceId}
              onBack={() => {
                setSelectedDeviceId(null);
                setCurrentTab("devices");
              }}
            />
          )}

          {currentTab === "network" && <NetworkActivity />}

          {currentTab === "searches" && <ChromeSearches />}

          {currentTab === "settings" && <Settings />}
        </div>
      </main>
    </div>
  );
};
export default App;
