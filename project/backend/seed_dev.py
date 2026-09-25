"""
Development seed script.
RUN EXPLICITLY ONLY DURING LOCAL OFFLINE DEVELOPMENT TESTING:
    python seed_dev.py
Do NOT run in production.
"""
import asyncio
from datetime import datetime, timezone
from database import (
    devices_collection,
    network_activity_collection,
    chrome_searches_collection,
    pairing_codes_collection,
    init_indexes
)

def utc_now():
    return datetime.now(timezone.utc).isoformat()

async def seed():
    await init_indexes()
    now = utc_now()
    print("[DEV SEED] Inserting authorized development test records...")

    devices = [
        {
            "device_id": "laptop-01",
            "device_name": "Dev Laptop-01",
            "ip_address": "192.168.1.20",
            "mac_address": "00:1A:2B:3C:4D:5E",
            "status": "online",
            "first_seen": now,
            "last_seen": now,
            "created_at": now
        },
        {
            "device_id": "phone-01",
            "device_name": "Dev Phone-01",
            "ip_address": "192.168.1.21",
            "mac_address": "3A:4B:5C:6D:7E:8F",
            "status": "online",
            "first_seen": now,
            "last_seen": now,
            "created_at": now
        }
    ]

    for d in devices:
        await devices_collection.update_one({"device_id": d["device_id"]}, {"$set": d}, upsert=True)

    print("[DEV SEED] Done. Test records inserted into MongoDB.")

if __name__ == "__main__":
    asyncio.run(seed())
