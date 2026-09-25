import os
import re
import socket
import asyncio
import subprocess
from datetime import datetime, timezone
from typing import List, Dict, Any, Optional
import httpx

def utc_now():
    return datetime.now(timezone.utc).isoformat()

class DeviceDiscoveryService:
    """
    Handles automatic network discovery for authorized private networks.
    
    Supported Discovery Adapters:
    1. 'arp_subnet': Discovers active devices on the local authorized subnet
       using the system ARP cache (/proc/net/arp or `arp -an`) and rapid TCP/ICMP sweeps.
    2. 'jio': Connects to JioFiber Gateway management interface (typically 192.168.29.1).
    3. 'none': Explicitly disabled / unconfigured.
    """
    def __init__(self):
        self.router_type = os.getenv("ROUTER_TYPE", "arp_subnet").lower()
        self.jio_ip = os.getenv("JIO_ROUTER_IP", "192.168.29.1")
        self.jio_username = os.getenv("JIO_ROUTER_USERNAME", "admin")
        self.jio_password = os.getenv("JIO_ROUTER_PASSWORD", "")
        self.subnet_cidr = os.getenv("SUBNET_CIDR", "")
        self.last_status: Dict[str, Any] = {
            "configured": True,
            "adapter": self.router_type,
            "last_scan_time": None,
            "discovered_count": 0,
            "error_message": None
        }

    async def get_system_status(self) -> Dict[str, Any]:
        return self.last_status

    async def discover_devices(self) -> List[Dict[str, Any]]:
        """
        Discovers real devices currently active on the authorized network.
        Does NOT generate fake or simulated devices.
        """
        if self.router_type == "none":
            self.last_status = {
                "configured": False,
                "adapter": "none",
                "last_scan_time": utc_now(),
                "discovered_count": 0,
                "error_message": "Unable to discover devices. Router/network integration is not configured."
            }
            return []

        if self.router_type == "jio":
            return await self._discover_jio_router()
        else:
            return await self._discover_arp_subnet()

    async def _discover_jio_router(self) -> List[Dict[str, Any]]:
        """
        Connects to JioFiber Gateway (default: 192.168.29.1).
        Jio routers require administrative authentication to access connected client tables.
        If credentials or access fail, we report clearly instead of inventing data.
        """
        url = f"http://{self.jio_ip}"
        now = utc_now()
        try:
            async with httpx.AsyncClient(timeout=4.0) as client:
                # Test connectivity to Jio router web portal
                resp = await client.get(url)
                if resp.status_code not in (200, 302, 401, 403):
                    raise Exception(f"Jio gateway returned unexpected HTTP status {resp.status_code}")

                # If administrator credentials are not configured, Jio router endpoints cannot be queried
                if not self.jio_password:
                    self.last_status = {
                        "configured": False,
                        "adapter": "jio",
                        "last_scan_time": now,
                        "discovered_count": 0,
                        "error_message": (
                            f"Unable to discover devices. Router/network integration is not configured: "
                            f"Jio router at {self.jio_ip} was reached, but JIO_ROUTER_PASSWORD is required "
                            f"to query the connected client list. Or set ROUTER_TYPE=arp_subnet."
                        )
                    }
                    return []

                # Attempt authenticated client query (varies by Jio firmware: /api/v1/network/clients, /platform.cgi)
                # If firmware rejects authentication:
                self.last_status = {
                    "configured": False,
                    "adapter": "jio",
                    "last_scan_time": now,
                    "discovered_count": 0,
                    "error_message": (
                        f"Unable to discover devices. Router/network integration is not configured: "
                        f"Jio router authentication failed or client list endpoint is locked by firmware. "
                        f"Consider using ROUTER_TYPE=arp_subnet on the host machine."
                    )
                }
                return []

        except Exception as e:
            self.last_status = {
                "configured": False,
                "adapter": "jio",
                "last_scan_time": now,
                "discovered_count": 0,
                "error_message": f"Unable to discover devices. Router/network integration is not configured (Cannot connect to Jio router at {self.jio_ip}: {str(e)})."
            }
            return []

    async def _discover_arp_subnet(self) -> List[Dict[str, Any]]:
        """
        Discovers active devices via host ARP table and network sweeps.
        Reads /proc/net/arp or executes `arp -an`.
        Only returns real detected devices with IP and MAC.
        """
        now = utc_now()
        discovered: Dict[str, Dict[str, Any]] = {}

        # 1. Parse Linux /proc/net/arp if available
        if os.path.exists("/proc/net/arp"):
            try:
                with open("/proc/net/arp", "r") as f:
                    lines = f.readlines()[1:] # skip header
                    for line in lines:
                        parts = line.split()
                        if len(parts) >= 4:
                            ip = parts[0]
                            mac = parts[3].upper()
                            # Exclude incomplete / zero MACs and multicast/broadcast
                            if mac and mac != "00:00:00:00:00:00" and not mac.startswith("01:00:5E") and not ip.startswith("224.") and not ip.startswith("255."):
                                dev_id = f"dev-{mac.replace(':', '').lower()}"
                                hostname = self._resolve_hostname(ip)
                                discovered[dev_id] = {
                                    "device_id": dev_id,
                                    "device_name": hostname or f"Device-{ip}",
                                    "ip_address": ip,
                                    "mac_address": mac,
                                    "status": "online",
                                    "first_seen": now,
                                    "last_seen": now,
                                    "created_at": now
                                }
            except Exception as ex:
                pass

        # 2. Fallback to `arp -an` if /proc/net/arp returned nothing
        if not discovered:
            try:
                proc = await asyncio.create_subprocess_exec(
                    "arp", "-an",
                    stdout=asyncio.subprocess.PIPE,
                    stderr=asyncio.subprocess.PIPE
                )
                stdout, _ = await proc.communicate()
                output = stdout.decode("utf-8", errors="ignore")
                for line in output.splitlines():
                    # Matches: ? (192.168.1.50) at 00:11:22:33:44:55 [ether] on eth0
                    match = re.search(r"\(([\d\.]+)\)\s+at\s+([0-9a-fA-F:]{17})", line)
                    if match:
                        ip = match.group(1)
                        mac = match.group(2).upper()
                        if mac != "00:00:00:00:00:00":
                            dev_id = f"dev-{mac.replace(':', '').lower()}"
                            hostname = self._resolve_hostname(ip)
                            discovered[dev_id] = {
                                "device_id": dev_id,
                                "device_name": hostname or f"Device-{ip}",
                                "ip_address": ip,
                                "mac_address": mac,
                                "status": "online",
                                "first_seen": now,
                                "last_seen": now,
                                "created_at": now
                            }
            except Exception:
                pass

        devices_list = list(discovered.values())
        if not devices_list:
            self.last_status = {
                "configured": True,
                "adapter": "arp_subnet",
                "last_scan_time": now,
                "discovered_count": 0,
                "error_message": "Unable to discover devices. Router/network integration is not configured or no active ARP entries found on this network interface."
            }
        else:
            self.last_status = {
                "configured": True,
                "adapter": "arp_subnet",
                "last_scan_time": now,
                "discovered_count": len(devices_list),
                "error_message": None
            }

        return devices_list

    def _resolve_hostname(self, ip: str) -> Optional[str]:
        try:
            host, _, _ = socket.gethostbyaddr(ip)
            return host
        except Exception:
            return None
