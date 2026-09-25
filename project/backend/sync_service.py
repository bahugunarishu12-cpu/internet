import os
import asyncio
import logging
from datetime import datetime, timezone
from typing import Dict, Any, List
from database import devices_collection, check_db_connection
from discovery import DeviceDiscoveryService, utc_now

logger = logging.getLogger("network-monitor.sync")

DEVICE_SYNC_INTERVAL = int(os.getenv("DEVICE_SYNC_INTERVAL", "10"))

class DeviceSyncService:
    def __init__(self, discovery_service: DeviceDiscoveryService):
        self.discovery_service = discovery_service
        self.sync_interval = max(3, DEVICE_SYNC_INTERVAL)
        self._is_running = False
        self._task = None
        self.last_sync_result: Dict[str, Any] = {
            "status": "idle",
            "last_sync_time": None,
            "detected_count": 0,
            "online_count": 0,
            "offline_count": 0,
            "error": None
        }

    async def start(self):
        if not self._is_running:
            self._is_running = True
            self._task = asyncio.create_task(self._sync_loop())
            logger.info("[SYNC] Background device synchronizer started (Interval: %ds, Adapter: %s)",
                        self.sync_interval, self.discovery_service.router_type)

    async def stop(self):
        self._is_running = False
        if self._task:
            self._task.cancel()
            logger.info("[SYNC] Background device synchronizer stopped")

    async def _sync_loop(self):
        while self._is_running:
            try:
                await self.perform_sync()
            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error("[SYNC] Unhandled exception in sync loop: %s", e, exc_info=True)
            await asyncio.sleep(self.sync_interval)

    async def perform_sync(self) -> Dict[str, Any]:
        """
        Executes a real device discovery synchronization cycle against MongoDB.
        Runs automatically in the background every DEVICE_SYNC_INTERVAL seconds.
        Does NOT invent dummy or fake data.
        """
        now = utc_now()

        # 1. Check DB connection first
        db_alive = await check_db_connection()
        if not db_alive:
            err_msg = "Database connection unavailable."
            logger.warning("[SYNC] Skipped cycle: %s", err_msg)
            self.last_sync_result = {
                "status": "error",
                "last_sync_time": now,
                "detected_count": 0,
                "online_count": 0,
                "offline_count": 0,
                "error": err_msg
            }
            return self.last_sync_result

        # 2. Run real discovery adapter
        detected_devices = await self.discovery_service.discover_devices()
        detected_ids = {d["device_id"] for d in detected_devices}

        # 3. Process detected devices and log newly discovered devices
        for dev in detected_devices:
            existing = await devices_collection.find_one({"device_id": dev["device_id"]})
            if not existing:
                logger.info("[DISCOVERY] Newly discovered device: %s (%s, IP: %s, MAC: %s)",
                            dev["device_id"], dev.get("device_name"), dev.get("ip_address"), dev.get("mac_address"))
            elif existing.get("status") != "online":
                logger.info("[STATUS] Device reconnected ONLINE: %s (%s, IP: %s)",
                            dev["device_id"], dev.get("device_name"), dev.get("ip_address"))

            await devices_collection.update_one(
                {"device_id": dev["device_id"]},
                {
                    "$set": {
                        "device_name": dev["device_name"],
                        "ip_address": dev["ip_address"],
                        "mac_address": dev.get("mac_address"),
                        "status": "online",
                        "last_seen": now
                    },
                    "$setOnInsert": {
                        "first_seen": now,
                        "created_at": now
                    }
                },
                upsert=True
            )

        # 4. Detect devices going offline (only if adapter executed without fatal error)
        sys_status = await self.discovery_service.get_system_status()
        if sys_status.get("configured") and sys_status.get("error_message") is None:
            offline_cursor = devices_collection.find({
                "device_id": {"$nin": list(detected_ids)},
                "status": "online"
            })
            offline_ids = []
            async for off_dev in offline_cursor:
                offline_ids.append(off_dev["device_id"])
                logger.info("[STATUS] Device went OFFLINE: %s (%s, IP: %s)",
                            off_dev["device_id"], off_dev.get("device_name"), off_dev.get("ip_address"))

            if offline_ids:
                await devices_collection.update_many(
                    {"device_id": {"$in": offline_ids}},
                    {"$set": {"status": "offline"}}
                )

        online_count = await devices_collection.count_documents({"status": "online"})
        offline_count = await devices_collection.count_documents({"status": "offline"})

        # Log cycle completion
        logger.info("[SYNC] Cycle complete: %d detected active | %d online total | %d offline total in DB",
                    len(detected_devices), online_count, offline_count)

        self.last_sync_result = {
            "status": "success",
            "last_sync_time": now,
            "detected_count": len(detected_devices),
            "online_count": online_count,
            "offline_count": offline_count,
            "error": sys_status.get("error_message")
        }
        return self.last_sync_result
