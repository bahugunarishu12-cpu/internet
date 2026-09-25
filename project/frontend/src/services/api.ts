import { Device, NetworkActivity, ChromeSearch, DashboardStats, CombinedActivity, PairingCode } from "../types";

const API_BASE = localStorage.getItem("backend_server_url") || "http://localhost:8000";

let authToken: string | null = localStorage.getItem("admin_token");

export const setAuthToken = (token: string | null) => {
  authToken = token;
  if (token) {
    localStorage.setItem("admin_token", token);
  } else {
    localStorage.removeItem("admin_token");
  }
};

export const getAuthToken = () => authToken;

export const setServerBaseUrl = (url: string) => {
  const clean = url.trim().replace(/\/$/, "");
  localStorage.setItem("backend_server_url", clean);
};

export const getServerBaseUrl = () => {
  return localStorage.getItem("backend_server_url") || "http://localhost:8000";
};

const authHeaders = () => ({
  "Content-Type": "application/json",
  ...(authToken ? { Authorization: `Bearer ${authToken}` } : {})
});

async function handleResponse(res: Response) {
  if (res.status === 503) {
    throw new Error("Database connection unavailable.");
  }
  if (!res.ok) {
    const err = await res.json().catch(() => ({ detail: `HTTP ${res.status}` }));
    throw new Error(err.detail || `Request failed with HTTP ${res.status}`);
  }
  return res.json();
}

export const api = {
  async getSystemStatus() {
    const res = await fetch(`${getServerBaseUrl()}/system/status`);
    return handleResponse(res);
  },

  async getSystemHealth() {
    const res = await fetch(`${getServerBaseUrl()}/system/health`);
    return handleResponse(res);
  },

  async login(username: string, password: string) {
    const res = await fetch(`${getServerBaseUrl()}/admin/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });
    const data = await handleResponse(res);
    setAuthToken(data.access_token);
    return data;
  },

  async getStats(): Promise<DashboardStats> {
    const res = await fetch(`${getServerBaseUrl()}/stats`, { headers: authHeaders() });
    return handleResponse(res);
  },

  async getDevices(params?: { status?: string; search?: string }): Promise<Device[]> {
    const query = new URLSearchParams();
    if (params?.status) query.append("status", params.status);
    if (params?.search) query.append("search", params.search);
    const res = await fetch(`${getServerBaseUrl()}/devices?${query.toString()}`, { headers: authHeaders() });
    return handleResponse(res);
  },

  async getOnlineDevices(): Promise<Device[]> {
    const res = await fetch(`${getServerBaseUrl()}/devices/online`, { headers: authHeaders() });
    return handleResponse(res);
  },

  async triggerSync() {
    const res = await fetch(`${getServerBaseUrl()}/devices/sync`, {
      method: "POST",
      headers: authHeaders()
    });
    return handleResponse(res);
  },

  async getDevice(deviceId: string): Promise<Device> {
    const res = await fetch(`${getServerBaseUrl()}/devices/${deviceId}`, { headers: authHeaders() });
    return handleResponse(res);
  },

  async getNetworkActivity(params?: { device_id?: string; domain?: string }): Promise<NetworkActivity[]> {
    const query = new URLSearchParams();
    if (params?.device_id) query.append("device_id", params.device_id);
    if (params?.domain) query.append("domain", params.domain);
    const res = await fetch(`${getServerBaseUrl()}/network-activity?${query.toString()}`, { headers: authHeaders() });
    return handleResponse(res);
  },

  async getChromeSearches(params?: { device_id?: string; search_engine?: string; query?: string }): Promise<ChromeSearch[]> {
    const query = new URLSearchParams();
    if (params?.device_id) query.append("device_id", params.device_id);
    if (params?.search_engine) query.append("search_engine", params.search_engine);
    if (params?.query) query.append("query", params.query);
    const res = await fetch(`${getServerBaseUrl()}/chrome-searches?${query.toString()}`, { headers: authHeaders() });
    return handleResponse(res);
  },

  async getCombinedActivity(deviceId: string): Promise<CombinedActivity> {
    const res = await fetch(`${getServerBaseUrl()}/devices/${deviceId}/activity`, { headers: authHeaders() });
    return handleResponse(res);
  },

  async generatePairingCode(deviceId: string): Promise<PairingCode> {
    const res = await fetch(`${getServerBaseUrl()}/pairing-codes`, {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify({ device_id: deviceId })
    });
    return handleResponse(res);
  },

  async registerDevice(data: { device_id: string; device_name: string; ip_address: string; mac_address?: string }) {
    const res = await fetch(`${getServerBaseUrl()}/devices/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    });
    return handleResponse(res);
  }
};
