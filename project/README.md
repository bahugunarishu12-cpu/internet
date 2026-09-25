# Network & Chrome Activity Monitor

A real, privacy-guaranteed activity monitoring and device discovery architecture for private networks where all monitored devices and users have provided explicit consent.

**STRICT REAL-DATA ONLY POLICY:**
This application does **not** generate fake devices, random search queries, or simulated stats. Data is stored persistently in MongoDB and fetched dynamically by both the Web and Android clients.

---

## Architecture

```
[ Real Network / Jio Router / Wi-Fi Subnet ]
                    │
            (ARP / HTTP Adapter)
                    ▼
     [ FastAPI Backend (Python) ] ── (Sync Every 10s) ──► [ MongoDB ]
                    ▲                                          ▲
                    │                                          │
       ┌────────────┴────────────┐                             │
       │                         │                             │
[ Android Admin Client ]  [ Web Dashboard (React) ]            │
       ▲                                                       │
       │                                                       │
[ Explicit Chrome Extension ] ── (POST /chrome-searches) ──────┘
```

- **Tablet/Phone Client:** The Android app is the **Admin Client** (not the router scanner). It connects to the FastAPI backend over HTTP to view real-time state.
- **FastAPI Server:** Runs on any machine on the authorized private network (e.g. PC, server, Raspberry Pi). It periodically synchronizes the active device list into MongoDB every 10 seconds (configurable via `DEVICE_SYNC_INTERVAL`).
- **MongoDB:** Persistently retains discovered devices, historical first_seen/last_seen dates, network domain hits, Chrome searches, and pairing codes across restarts.

---

## Jio Router Integration & Device Discovery

### Jio Router Technical Behavior
JioFiber Home Gateways (typically at `192.168.29.1` or `192.168.1.1`):
1. **No Open REST API:** Jio routers do not provide an unauthenticated open REST API for connected DHCP leases/clients on the LAN.
2. **Firmware Authentication:** The web UI runs session-based authentication (`admin` / user password).
3. **Recommended Discovery Approach:**
   - **`ROUTER_TYPE=arp_subnet` (Default):** Runs the backend on the local Wi-Fi/Ethernet network. It inspects the host kernel ARP table (`/proc/net/arp` and `arp -an`) and active local subnet sweeps. This discovers all real active devices (IP, MAC address, resolved hostname) without needing router admin passwords.
   - **`ROUTER_TYPE=jio`:** Configures direct gateway polling to `http://192.168.29.1` using `JIO_ROUTER_PASSWORD`. If the password is not set or the router is unreachable, the system clearly reports:
     `"Unable to discover devices. Router/network integration is not configured."`

---

## Environment Variables (`project/backend/.env`)

```env
# MongoDB Connection
MONGODB_URL=mongodb://localhost:27017
DATABASE_NAME=network_monitor_db

# Security & Admin JWT
SECRET_KEY=super-secret-admin-key-change-in-production-2026
ACCESS_TOKEN_EXPIRE_MINUTES=1440
PORT=8000

# Device Discovery & Sync
DEVICE_SYNC_INTERVAL=10
ROUTER_TYPE=arp_subnet
JIO_ROUTER_IP=192.168.29.1
JIO_ROUTER_USERNAME=admin
JIO_ROUTER_PASSWORD=
SUBNET_CIDR=
```

---

## MongoDB Setup & Proper Indexes

1. Start MongoDB via Docker:
```bash
docker run -d -p 27017:27017 --name mongo-netmon -v mongo_data:/data/db mongo:latest
```
*(The `-v mongo_data:/data/db` volume ensures data persists permanently across container restarts).*

2. The backend automatically creates the required indexes at startup:
- `devices.device_id` (unique)
- `devices.ip_address`
- `devices.mac_address`
- `devices.last_seen`
- `devices.status`
- `network_activity.device_id`
- `network_activity.timestamp`
- `chrome_searches.device_id`
- `chrome_searches.timestamp`
- `pairing_codes.code` (unique)

---

## How to Run

### 1. Run Backend (FastAPI)
```bash
cd project/backend
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
python3 main.py
```
Backend will start on `http://0.0.0.0:8000`. Swagger API docs: `http://localhost:8000/docs`.

### 2. Run Web Admin Dashboard (React + TypeScript)
```bash
cd project/frontend
npm install
npm run dev
```
Open `http://localhost:5173`. Default login: `admin` / `admin123`.

### 3. Run Android Admin Client (Phone / Tablet)
- When running in an emulator, the backend is reachable at `http://10.0.2.2:8000`.
- When running on a physical phone or tablet connected to your Wi-Fi, open the **Settings** tab in the app and set the **FastAPI Server Base URL** to your computer's LAN IP, e.g. `http://192.168.1.100:8000` (or `http://192.168.29.50:8000`).
- Tap **Save & Connect**. The Android client will immediately fetch real devices, domain activity, and Chrome searches from MongoDB, auto-refreshing every 10 seconds.

---

## How to Test

### 1. Testing Device Discovery
1. Ensure your backend machine is connected to your local network.
2. Trigger a sync from the terminal or dashboard:
```bash
curl -X POST http://localhost:8000/devices/sync
```
3. Check discovered devices:
```bash
curl -s http://localhost:8000/devices | jq .
```
Only real devices detected on the network will appear.

### 2. Testing Chrome Extension
1. Open Google Chrome at `chrome://extensions/`.
2. Enable **Developer mode** -> **Load unpacked** -> select `project/chrome-extension/`.
3. In the Admin Dashboard (Web or Android), click **Pair Code** on a device to generate a single-use code (e.g. `PAIR-8F42K`).
4. In the extension popup, enter the Server URL (`http://localhost:8000` or your LAN IP) and the pairing code, check the consent agreement, and click **Pair Extension**.
5. Perform a test Google search: `https://www.google.com/search?q=fastapi+tutorial`.
6. Refresh the dashboard or Android app: the exact search query keyword will appear under **Chrome Searches** for that paired device.

### 3. Verifying Data Remains After Restart
1. Stop the backend (`Ctrl + C`) and close the web/Android app.
2. Start the backend again (`python3 main.py`).
3. Reopen the dashboard or mobile app.
4. All previously discovered devices, their `first_seen` timestamps, historical domain events, and Chrome searches will be loaded directly from MongoDB.
