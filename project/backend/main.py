import os
import random
import string
import re
import logging
from datetime import datetime, timezone, timedelta
from typing import Optional, List, Dict, Any
from fastapi import FastAPI, Depends, HTTPException, Query, status
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

load_dotenv()

# Configure production logging format
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [%(name)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)
logger = logging.getLogger("network-monitor")

from database import (
    get_database,
    devices_collection,
    network_activity_collection,
    chrome_searches_collection,
    admin_users_collection,
    pairing_codes_collection,
    check_db_connection,
    init_indexes,
    DATABASE_NAME,
    MONGODB_URL
)
from models import (
    DeviceRegisterRequest,
    DeviceHeartbeatRequest,
    DeviceResponse,
    NetworkActivityCreate,
    NetworkActivityResponse,
    ChromeSearchCreate,
    ChromeSearchResponse,
    PairingCodeCreate,
    PairingCodeVerify,
    PairingCodeResponse,
    AdminLoginRequest,
    TokenResponse,
    CombinedActivityResponse,
    DashboardStatsResponse,
    utc_now_str
)
from auth import (
    verify_password,
    get_password_hash,
    create_access_token,
    get_current_admin
)
from discovery import DeviceDiscoveryService
from sync_service import DeviceSyncService

SERVER_HOST = os.getenv("SERVER_HOST", "0.0.0.0")
SERVER_PORT = int(os.getenv("SERVER_PORT", os.getenv("PORT", "8000")))

server_start_time = datetime.now(timezone.utc)

app = FastAPI(
    title="Network & Chrome Activity Monitor API",
    description="Authorized network device discovery & privacy-compliant activity monitor. Running as a production system service with MongoDB persistence and automatic background synchronization.",
    version="2.1.0"
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

discovery_service = DeviceDiscoveryService()
sync_service = DeviceSyncService(discovery_service)

def clean_doc_id(doc: dict) -> dict:
    if "_id" in doc:
        doc["id"] = str(doc.pop("_id"))
    return doc

def sanitize_domain(raw_domain: str) -> str:
    """Sanitize domain so only host destination is captured (no path, query, or payload)."""
    d = raw_domain.strip().lower()
    d = re.sub(r"^https?://", "", d)
    d = d.split("/")[0].split("?")[0].split(":")[0]
    return d

def sanitize_query(raw_query: str) -> str:
    """Sanitize Chrome search query."""
    q = raw_query.strip()
    q = re.sub(r"\b(?:\d{4}[ -]?){3}\d{4}\b", "[REDACTED_CARD]", q)
    return q[:500]

async def ensure_db():
    if not await check_db_connection():
        logger.error("[DATABASE] Query aborted: Database connection unavailable.")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Database connection unavailable."
        )

@app.on_event("startup")
async def startup_event():
    logger.info("[STARTUP] Network & Chrome Activity Monitor starting on %s:%d", SERVER_HOST, SERVER_PORT)
    logger.info("[STARTUP] Connecting to MongoDB: %s (DB: %s)", MONGODB_URL.split("@")[-1], DATABASE_NAME)

    db_alive = await check_db_connection()
    if db_alive:
        logger.info("[DATABASE] Successfully connected to MongoDB database '%s'", DATABASE_NAME)
        await init_indexes()
        admin_count = await admin_users_collection.count_documents({})
        if admin_count == 0:
            await admin_users_collection.insert_one({
                "username": "admin",
                "hashed_password": get_password_hash("admin123"),
                "created_at": utc_now_str()
            })
            logger.info("[AUTH] Default admin initialized: admin / admin123 (Change in production)")
    else:
        logger.error("[DATABASE] Warning: MongoDB connection failed on startup. Service will retry upon queries.")

    # Start background device discovery loop
    await sync_service.start()

@app.on_event("shutdown")
async def shutdown_event():
    logger.info("[SHUTDOWN] Stopping background services and shutting down...")
    await sync_service.stop()

# ----------------- SYSTEM HEALTH & DIAGNOSTICS -----------------
@app.get("/system/health", tags=["System"])
async def system_health():
    """
    Production health check endpoint.
    Reports database connectivity, uptime, discovery adapter status, and background sync statistics.
    """
    db_alive = await check_db_connection()
    discovery_status = await discovery_service.get_system_status()
    sync_status = sync_service.last_sync_result
    uptime = (datetime.now(timezone.utc) - server_start_time).total_seconds()

    overall_status = "healthy" if db_alive else "unhealthy"

    return {
        "status": overall_status,
        "uptime_seconds": round(uptime, 1),
        "database": {
            "connected": db_alive,
            "database_name": DATABASE_NAME,
            "error": None if db_alive else "Database connection unavailable."
        },
        "discovery": {
            "adapter": discovery_status.get("adapter"),
            "configured": discovery_status.get("configured"),
            "last_scan_time": discovery_status.get("last_scan_time"),
            "discovered_count": discovery_status.get("discovered_count"),
            "error": discovery_status.get("error_message")
        },
        "sync": {
            "interval_seconds": sync_service.sync_interval,
            "last_sync_time": sync_status.get("last_sync_time"),
            "status": sync_status.get("status"),
            "online_devices": sync_status.get("online_count"),
            "offline_devices": sync_status.get("offline_count"),
            "error": sync_status.get("error")
        },
        "service": "Network & Chrome Activity Monitor API",
        "version": "2.1.0",
        "privacy_compliant": True
    }

@app.get("/health", tags=["System"])
async def health_check():
    db_alive = await check_db_connection()
    return {
        "status": "healthy" if db_alive else "degraded",
        "database": "connected" if db_alive else "Database connection unavailable.",
        "service": "Network & Chrome Activity Monitor API",
        "privacy_compliant": True
    }

@app.get("/system/status", tags=["System"])
async def system_status():
    db_alive = await check_db_connection()
    discovery_status = await discovery_service.get_system_status()
    sync_status = sync_service.last_sync_result

    return {
        "database_connected": db_alive,
        "database_error": None if db_alive else "Database connection unavailable.",
        "discovery": discovery_status,
        "sync": sync_status
    }

# ----------------- ADMIN AUTH -----------------
@app.post("/admin/login", response_model=TokenResponse, tags=["Authentication"])
async def admin_login(form_data: AdminLoginRequest):
    await ensure_db()
    user = await admin_users_collection.find_one({"username": form_data.username})
    if not user or not verify_password(form_data.password, user["hashed_password"]):
        logger.warning("[AUTH] Failed login attempt for user '%s'", form_data.username)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password"
        )
    token = create_access_token(data={"sub": user["username"]})
    logger.info("[AUTH] Successful login for user '%s'", user["username"])
    return TokenResponse(access_token=token, token_type="bearer", username=user["username"])

# ----------------- DEVICES -----------------
@app.get("/devices", response_model=List[DeviceResponse], tags=["Devices"])
async def list_devices(
    status_filter: Optional[str] = Query(None, alias="status"),
    search: Optional[str] = None,
    skip: int = 0,
    limit: int = 100
):
    await ensure_db()
    query: Dict[str, Any] = {}
    if status_filter:
        query["status"] = status_filter
    if search:
        query["$or"] = [
            {"device_name": {"$regex": search, "$options": "i"}},
            {"device_id": {"$regex": search, "$options": "i"}},
            {"ip_address": {"$regex": search, "$options": "i"}},
            {"mac_address": {"$regex": search, "$options": "i"}}
        ]

    cursor = devices_collection.find(query).skip(skip).limit(limit).sort("last_seen", -1)
    devices = []
    async for d in cursor:
        clean_doc_id(d)
        devices.append(DeviceResponse(**d))
    return devices

@app.get("/devices/online", response_model=List[DeviceResponse], tags=["Devices"])
async def get_online_devices():
    await ensure_db()
    cursor = devices_collection.find({"status": "online"}).sort("last_seen", -1)
    devices = []
    async for d in cursor:
        clean_doc_id(d)
        devices.append(DeviceResponse(**d))
    return devices

@app.get("/devices/{device_id}", response_model=DeviceResponse, tags=["Devices"])
async def get_device(device_id: str):
    await ensure_db()
    device = await devices_collection.find_one({"device_id": device_id})
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")
    clean_doc_id(device)
    return DeviceResponse(**device)

@app.post("/devices/sync", tags=["Devices"])
async def trigger_device_sync():
    """
    On-demand synchronization trigger.
    Note: Device discovery already runs automatically every 10s via background service.
    """
    await ensure_db()
    result = await sync_service.perform_sync()
    return result

@app.post("/devices/register", response_model=DeviceResponse, tags=["Devices"])
async def register_device(payload: DeviceRegisterRequest):
    await ensure_db()
    now = utc_now_str()
    existing = await devices_collection.find_one({"device_id": payload.device_id})
    if existing:
        await devices_collection.update_one(
            {"device_id": payload.device_id},
            {"$set": {
                "device_name": payload.device_name,
                "ip_address": payload.ip_address,
                "mac_address": payload.mac_address or existing.get("mac_address"),
                "status": "online",
                "last_seen": now
            }}
        )
        logger.info("[DEVICES] Updated registered device %s (%s)", payload.device_id, payload.device_name)
        updated = await devices_collection.find_one({"device_id": payload.device_id})
        clean_doc_id(updated)
        return DeviceResponse(**updated)

    new_device = {
        "device_id": payload.device_id,
        "device_name": payload.device_name,
        "ip_address": payload.ip_address,
        "mac_address": payload.mac_address,
        "status": "online",
        "first_seen": now,
        "last_seen": now,
        "created_at": now
    }
    await devices_collection.insert_one(new_device)
    logger.info("[DEVICES] Registered new authorized device: %s (%s, IP: %s)",
                payload.device_id, payload.device_name, payload.ip_address)
    clean_doc_id(new_device)
    return DeviceResponse(**new_device)

@app.post("/devices/heartbeat", tags=["Devices"])
async def device_heartbeat(payload: DeviceHeartbeatRequest):
    await ensure_db()
    now = utc_now_str()
    update_data = {"status": payload.status, "last_seen": now}
    if payload.ip_address:
        update_data["ip_address"] = payload.ip_address

    res = await devices_collection.update_one(
        {"device_id": payload.device_id},
        {"$set": update_data}
    )
    if res.matched_count == 0:
        raise HTTPException(status_code=404, detail="Device not registered in database")
    return {"message": "Heartbeat updated", "status": payload.status, "timestamp": now}

# ----------------- NETWORK DOMAIN ACTIVITY -----------------
@app.get("/network-activity", response_model=List[NetworkActivityResponse], tags=["Network Activity"])
async def get_network_activity(
    device_id: Optional[str] = None,
    domain: Optional[str] = None,
    skip: int = 0,
    limit: int = 100
):
    await ensure_db()
    query: Dict[str, Any] = {}
    if device_id:
        query["device_id"] = device_id
    if domain:
        query["domain"] = {"$regex": domain, "$options": "i"}

    cursor = network_activity_collection.find(query).skip(skip).limit(limit).sort("timestamp", -1)
    results = []
    async for doc in cursor:
        clean_doc_id(doc)
        results.append(NetworkActivityResponse(**doc))
    return results

@app.post("/network-activity", response_model=NetworkActivityResponse, tags=["Network Activity"])
async def record_network_activity(payload: NetworkActivityCreate):
    await ensure_db()
    clean_dom = sanitize_domain(payload.domain)
    now = payload.timestamp or utc_now_str()
    doc = {
        "device_id": payload.device_id,
        "ip_address": payload.ip_address,
        "domain": clean_dom,
        "timestamp": now
    }
    await network_activity_collection.insert_one(doc)
    await devices_collection.update_one(
        {"device_id": payload.device_id},
        {"$set": {"last_seen": now, "status": "online"}}
    )
    clean_doc_id(doc)
    return NetworkActivityResponse(**doc)

# ----------------- CHROME SEARCHES -----------------
@app.get("/chrome-searches", response_model=List[ChromeSearchResponse], tags=["Chrome Searches"])
async def get_chrome_searches(
    device_id: Optional[str] = None,
    search_engine: Optional[str] = None,
    query: Optional[str] = None,
    skip: int = 0,
    limit: int = 100
):
    await ensure_db()
    q: Dict[str, Any] = {}
    if device_id:
        q["device_id"] = device_id
    if search_engine:
        q["search_engine"] = search_engine
    if query:
        q["search_query"] = {"$regex": query, "$options": "i"}

    cursor = chrome_searches_collection.find(q).skip(skip).limit(limit).sort("timestamp", -1)
    results = []
    async for doc in cursor:
        clean_doc_id(doc)
        results.append(ChromeSearchResponse(**doc))
    return results

@app.post("/chrome-searches", response_model=ChromeSearchResponse, tags=["Chrome Searches"])
async def record_chrome_search(payload: ChromeSearchCreate):
    await ensure_db()
    now = payload.timestamp or utc_now_str()
    clean_q = sanitize_query(payload.search_query)

    doc = {
        "device_id": payload.device_id,
        "browser": payload.browser or "Chrome",
        "search_engine": payload.search_engine,
        "search_query": clean_q,
        "timestamp": now
    }
    await chrome_searches_collection.insert_one(doc)
    await devices_collection.update_one(
        {"device_id": payload.device_id},
        {"$set": {"last_seen": now, "status": "online"}}
    )
    logger.info("[CHROME] Recorded search from %s (%s: '%s')", payload.device_id, payload.search_engine, clean_q)
    clean_doc_id(doc)
    return ChromeSearchResponse(**doc)

# ----------------- PAIRING CODES -----------------
@app.post("/pairing-codes", response_model=PairingCodeResponse, tags=["Pairing"])
async def generate_pairing_code(payload: PairingCodeCreate):
    await ensure_db()
    device = await devices_collection.find_one({"device_id": payload.device_id})
    if not device:
        raise HTTPException(status_code=404, detail="Device must exist before pairing extension")

    code_chars = "".join(random.choices(string.ascii_uppercase + string.digits, k=5))
    code = f"PAIR-{code_chars}"
    now = datetime.now(timezone.utc)
    expires_at = (now + timedelta(hours=24)).isoformat()

    doc = {
        "code": code,
        "device_id": payload.device_id,
        "is_used": False,
        "created_at": now.isoformat(),
        "expires_at": expires_at
    }
    await pairing_codes_collection.insert_one(doc)
    logger.info("[PAIRING] Generated code %s for device %s", code, payload.device_id)
    clean_doc_id(doc)
    return PairingCodeResponse(**doc)

@app.post("/pairing-codes/verify", tags=["Pairing"])
async def verify_pairing_code(payload: PairingCodeVerify):
    await ensure_db()
    doc = await pairing_codes_collection.find_one({"code": payload.code.strip()})
    if not doc:
        raise HTTPException(status_code=400, detail="Invalid pairing code")
    if doc.get("is_used"):
        raise HTTPException(status_code=400, detail="Pairing code has already been used")

    await pairing_codes_collection.update_one(
        {"_id": doc["_id"]},
        {"$set": {"is_used": True}}
    )
    device = await devices_collection.find_one({"device_id": doc["device_id"]})
    logger.info("[PAIRING] Successfully verified pairing code %s for device %s", payload.code, doc["device_id"])
    return {
        "status": "success",
        "device_id": doc["device_id"],
        "device_name": device["device_name"] if device else doc["device_id"],
        "message": f"Successfully paired extension with device {doc['device_id']}"
    }

# ----------------- COMBINED ACTIVITY & STATS -----------------
@app.get("/devices/{device_id}/activity", response_model=CombinedActivityResponse, tags=["Activity"])
async def get_device_combined_activity(device_id: str, limit: int = 50):
    await ensure_db()
    device = await devices_collection.find_one({"device_id": device_id})
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")
    clean_doc_id(device)

    net_cursor = network_activity_collection.find({"device_id": device_id}).limit(limit).sort("timestamp", -1)
    net_list = []
    async for item in net_cursor:
        clean_doc_id(item)
        net_list.append(NetworkActivityResponse(**item))

    chrome_cursor = chrome_searches_collection.find({"device_id": device_id}).limit(limit).sort("timestamp", -1)
    chrome_list = []
    async for item in chrome_cursor:
        clean_doc_id(item)
        chrome_list.append(ChromeSearchResponse(**item))

    return CombinedActivityResponse(
        device=DeviceResponse(**device),
        network_activity=net_list,
        chrome_searches=chrome_list
    )

@app.get("/stats", response_model=DashboardStatsResponse, tags=["Dashboard"])
async def get_dashboard_stats():
    await ensure_db()
    total_devices = await devices_collection.count_documents({})
    online_devices = await devices_collection.count_documents({"status": "online"})
    offline_devices = await devices_collection.count_documents({"status": "offline"})
    domain_count = await network_activity_collection.count_documents({})
    search_count = await chrome_searches_collection.count_documents({})

    return DashboardStatsResponse(
        total_devices=total_devices,
        online_devices=online_devices,
        offline_devices=offline_devices,
        domain_activity_count=domain_count,
        chrome_search_count=search_count
    )

if __name__ == "__main__":
    import uvicorn
    logger.info("[STARTUP] Launching Uvicorn production server on %s:%d", SERVER_HOST, SERVER_PORT)
    uvicorn.run("main:app", host=SERVER_HOST, port=SERVER_PORT, reload=False)
