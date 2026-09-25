import os
import logging
from motor.motor_asyncio import AsyncIOMotorClient
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger("network-monitor.database")

MONGODB_URL = os.getenv("MONGODB_URL", "mongodb://localhost:27017")
DATABASE_NAME = os.getenv("DATABASE_NAME", "network_monitor_db")

client = AsyncIOMotorClient(
    MONGODB_URL,
    serverSelectionTimeoutMS=2000,
    connectTimeoutMS=2000
)
db = client[DATABASE_NAME]

# Collections
devices_collection = db["devices"]
network_activity_collection = db["network_activity"]
chrome_searches_collection = db["chrome_searches"]
admin_users_collection = db["admin_users"]
pairing_codes_collection = db["pairing_codes"]

async def check_db_connection() -> bool:
    """Checks whether MongoDB is alive and responding."""
    try:
        await client.admin.command('ping')
        return True
    except Exception as e:
        logger.warning("[DATABASE] MongoDB connection unavailable at %s: %s", MONGODB_URL, e)
        return False

async def init_indexes():
    """Create all required MongoDB indexes."""
    try:
        # devices indexes
        await devices_collection.create_index("device_id", unique=True)
        await devices_collection.create_index("ip_address")
        await devices_collection.create_index("mac_address")
        await devices_collection.create_index("last_seen")
        await devices_collection.create_index("status")

        # network activity indexes
        await network_activity_collection.create_index("device_id")
        await network_activity_collection.create_index("timestamp")

        # chrome searches indexes
        await chrome_searches_collection.create_index("device_id")
        await chrome_searches_collection.create_index("timestamp")

        # pairing codes indexes
        await pairing_codes_collection.create_index("code", unique=True)
        await pairing_codes_collection.create_index("device_id")
        logger.info("[DATABASE] Verified MongoDB indexes for database '%s'", DATABASE_NAME)
    except Exception as e:
        logger.error("[DATABASE] Warning: Index initialization failed: %s", e)
