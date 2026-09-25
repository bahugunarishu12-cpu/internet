from datetime import datetime, timezone
from typing import Optional, List
from pydantic import BaseModel, Field

def utc_now_str():
    return datetime.now(timezone.utc).isoformat()

# Device models
class DeviceRegisterRequest(BaseModel):
    device_id: str = Field(..., min_length=2, max_length=64, description="Unique authorized device identifier")
    device_name: str = Field(..., min_length=1, max_length=100, description="Friendly device name (e.g. Laptop-01)")
    ip_address: str = Field(..., min_length=7, max_length=45, description="Network IP address")
    mac_address: Optional[str] = Field(None, max_length=30, description="Legitimately acquired MAC address if available")

class DeviceHeartbeatRequest(BaseModel):
    device_id: str
    ip_address: Optional[str] = None
    status: str = "online"

class DeviceResponse(BaseModel):
    device_id: str
    device_name: str
    ip_address: str
    mac_address: Optional[str] = None
    status: str = "online"
    first_seen: str
    last_seen: str
    created_at: str

# Network domain activity models
class NetworkActivityCreate(BaseModel):
    device_id: str = Field(..., description="Device ID initiating the connection")
    ip_address: str = Field(..., description="Source IP address")
    domain: str = Field(..., min_length=3, max_length=255, description="Domain name (e.g. google.com). Never path/payload.")
    timestamp: Optional[str] = None

class NetworkActivityResponse(BaseModel):
    id: Optional[str] = None
    device_id: str
    ip_address: str
    domain: str
    timestamp: str

# Chrome search activity models (privacy-focused)
class ChromeSearchCreate(BaseModel):
    device_id: str = Field(..., description="Paired device ID")
    browser: str = Field(default="Chrome", description="Browser identifier")
    search_engine: str = Field(..., description="Search engine name (e.g. Google, Bing, DuckDuckGo)")
    search_query: str = Field(..., min_length=1, max_length=500, description="Search term only. No credentials/PII.")
    timestamp: Optional[str] = None

class ChromeSearchResponse(BaseModel):
    id: Optional[str] = None
    device_id: str
    browser: str
    search_engine: str
    search_query: str
    timestamp: str

# Pairing codes
class PairingCodeCreate(BaseModel):
    device_id: str = Field(..., description="Target device to pair with extension")

class PairingCodeVerify(BaseModel):
    code: str = Field(..., min_length=5, max_length=20)

class PairingCodeResponse(BaseModel):
    code: str
    device_id: str
    is_used: bool
    created_at: str
    expires_at: str

# Admin user & Auth
class AdminLoginRequest(BaseModel):
    username: str
    password: str

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    username: str

# Combined Activity Response
class CombinedActivityResponse(BaseModel):
    device: DeviceResponse
    network_activity: List[NetworkActivityResponse]
    chrome_searches: List[ChromeSearchResponse]

# Dashboard Stats
class DashboardStatsResponse(BaseModel):
    total_devices: int
    online_devices: int
    offline_devices: int
    domain_activity_count: int
    chrome_search_count: int
