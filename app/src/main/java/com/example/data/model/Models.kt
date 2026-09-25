package com.example.data.model

data class Device(
    val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
    val macAddress: String? = null,
    val status: String = "online", // "online", "offline", or "unknown"
    val firstSeen: String,
    val lastSeen: String,
    val pairedCode: String? = null
)

data class NetworkDomainActivity(
    val id: String,
    val deviceId: String,
    val ipAddress: String,
    val domain: String,
    val timestamp: String
)

data class ChromeSearchActivity(
    val id: String,
    val deviceId: String,
    val browser: String = "Chrome",
    val searchEngine: String,
    val searchQuery: String,
    val timestamp: String
)

data class PairingCode(
    val code: String,
    val deviceId: String,
    val isUsed: Boolean = false,
    val createdAt: String,
    val expiresAt: String
)

data class SystemStatus(
    val databaseConnected: Boolean = true,
    val databaseError: String? = null,
    val discoveryConfigured: Boolean = true,
    val discoveryError: String? = null,
    val syncStatus: String? = null
)
