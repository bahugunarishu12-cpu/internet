export interface Device {
  device_id: str;
  device_name: str;
  ip_address: str;
  mac_address?: string;
  status: "online" | "offline";
  first_seen: string;
  last_seen: string;
  created_at: string;
}

type str = string;

export interface NetworkActivity {
  id?: string;
  device_id: string;
  ip_address: string;
  domain: string;
  timestamp: string;
}

export interface ChromeSearch {
  id?: string;
  device_id: string;
  browser: string;
  search_engine: string;
  search_query: string;
  timestamp: string;
}

export interface DashboardStats {
  total_devices: number;
  online_devices: number;
  offline_devices: number;
  domain_activity_count: number;
  chrome_search_count: number;
}

export interface CombinedActivity {
  device: Device;
  network_activity: NetworkActivity[];
  chrome_searches: ChromeSearch[];
}

export interface PairingCode {
  code: string;
  device_id: string;
  is_used: boolean;
  created_at: string;
  expires_at: string;
}
