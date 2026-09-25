package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ChromeSearchActivity
import com.example.data.model.Device
import com.example.data.model.NetworkDomainActivity
import com.example.data.model.PairingCode
import com.example.data.model.SystemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ActivityRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("netmon_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    // Server URL stored persistently in SharedPreferences
    private val _serverUrl = MutableStateFlow(
        prefs.getString("server_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
    )
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    // Real data collections (strictly empty initially, NEVER populated with dummy/mock data)
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _networkActivities = MutableStateFlow<List<NetworkDomainActivity>>(emptyList())
    val networkActivities: StateFlow<List<NetworkDomainActivity>> = _networkActivities.asStateFlow()

    private val _chromeSearches = MutableStateFlow<List<ChromeSearchActivity>>(emptyList())
    val chromeSearches: StateFlow<List<ChromeSearchActivity>> = _chromeSearches.asStateFlow()

    private val _systemStatus = MutableStateFlow(SystemStatus())
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateServerUrl(url: String) {
        val cleanUrl = url.trim().removeSuffix("/")
        _serverUrl.value = cleanUrl
        prefs.edit().putString("server_url", cleanUrl).apply()
    }

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        _isRefreshing.value = true
        val baseUrl = _serverUrl.value

        try {
            // 1. Check system status & database connectivity
            val statusReq = Request.Builder().url("$baseUrl/system/status").get().build()
            try {
                client.newCall(statusReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: "{}"
                        val obj = JSONObject(body)
                        val dbConnected = obj.optBoolean("database_connected", false)
                        val dbErr = if (obj.has("database_error") && !obj.isNull("database_error")) obj.getString("database_error") else null

                        val discObj = obj.optJSONObject("discovery")
                        val discConfigured = discObj?.optBoolean("configured", false) ?: false
                        val discErr = if (discObj != null && discObj.has("error_message") && !discObj.isNull("error_message")) {
                            discObj.getString("error_message")
                        } else null

                        _systemStatus.value = SystemStatus(
                            databaseConnected = dbConnected,
                            databaseError = dbErr,
                            discoveryConfigured = discConfigured,
                            discoveryError = discErr
                        )
                    } else if (resp.code == 503) {
                        _systemStatus.value = SystemStatus(
                            databaseConnected = false,
                            databaseError = "Database connection unavailable."
                        )
                    }
                }
            } catch (e: Exception) {
                _systemStatus.value = SystemStatus(
                    databaseConnected = false,
                    databaseError = "Database connection unavailable."
                )
            }

            // 2. Fetch Devices
            val devReq = Request.Builder().url("$baseUrl/devices").get().build()
            client.newCall(devReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: "[]"
                    val arr = JSONArray(body)
                    val devList = mutableListOf<Device>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        devList.add(
                            Device(
                                deviceId = o.optString("device_id"),
                                deviceName = o.optString("device_name"),
                                ipAddress = o.optString("ip_address"),
                                macAddress = if (o.has("mac_address") && !o.isNull("mac_address")) o.getString("mac_address") else null,
                                status = o.optString("status", "unknown"),
                                firstSeen = o.optString("first_seen"),
                                lastSeen = o.optString("last_seen"),
                                pairedCode = if (o.has("paired_code") && !o.isNull("paired_code")) o.getString("paired_code") else null
                            )
                        )
                    }
                    _devices.value = devList
                    _errorMessage.value = null
                } else if (resp.code == 503) {
                    _errorMessage.value = "Database connection unavailable."
                } else {
                    _errorMessage.value = "Server returned error: HTTP ${resp.code}"
                }
            }

            // 3. Fetch Network Activity
            val netReq = Request.Builder().url("$baseUrl/network-activity?limit=50").get().build()
            client.newCall(netReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: "[]"
                    val arr = JSONArray(body)
                    val netList = mutableListOf<NetworkDomainActivity>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        netList.add(
                            NetworkDomainActivity(
                                id = o.optString("id", i.toString()),
                                deviceId = o.optString("device_id"),
                                ipAddress = o.optString("ip_address"),
                                domain = o.optString("domain"),
                                timestamp = o.optString("timestamp")
                            )
                        )
                    }
                    _networkActivities.value = netList
                }
            }

            // 4. Fetch Chrome Searches
            val searchReq = Request.Builder().url("$baseUrl/chrome-searches?limit=50").get().build()
            client.newCall(searchReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: "[]"
                    val arr = JSONArray(body)
                    val searchList = mutableListOf<ChromeSearchActivity>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        searchList.add(
                            ChromeSearchActivity(
                                id = o.optString("id", i.toString()),
                                deviceId = o.optString("device_id"),
                                browser = o.optString("browser", "Chrome"),
                                searchEngine = o.optString("search_engine"),
                                searchQuery = o.optString("search_query"),
                                timestamp = o.optString("timestamp")
                            )
                        )
                    }
                    _chromeSearches.value = searchList
                }
            }

        } catch (e: Exception) {
            _errorMessage.value = "Unable to connect to backend server at $baseUrl: ${e.message}"
        } finally {
            _isRefreshing.value = false
        }
    }

    suspend fun triggerSync(): String = withContext(Dispatchers.IO) {
        val baseUrl = _serverUrl.value
        try {
            val emptyBody = "".toRequestBody(JSON_MEDIA)
            val req = Request.Builder().url("$baseUrl/devices/sync").post(emptyBody).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    refreshAll()
                    return@withContext "Network synchronization complete"
                } else if (resp.code == 503) {
                    return@withContext "Database connection unavailable."
                } else {
                    return@withContext "Sync failed: HTTP ${resp.code}"
                }
            }
        } catch (e: Exception) {
            return@withContext "Sync failed: Cannot connect to $baseUrl (${e.message})"
        }
    }

    suspend fun generatePairingCode(deviceId: String): Result<String> = withContext(Dispatchers.IO) {
        val baseUrl = _serverUrl.value
        try {
            val json = JSONObject().put("device_id", deviceId).toString()
            val body = json.toRequestBody(JSON_MEDIA)
            val req = Request.Builder().url("$baseUrl/pairing-codes").post(body).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val respObj = JSONObject(resp.body?.string() ?: "{}")
                    val code = respObj.optString("code")
                    refreshAll()
                    return@withContext Result.success(code)
                } else {
                    return@withContext Result.failure(Exception("Failed to generate code (HTTP ${resp.code})"))
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun registerDevice(id: String, name: String, ip: String, mac: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = _serverUrl.value
        try {
            val json = JSONObject().apply {
                put("device_id", id)
                put("device_name", name)
                put("ip_address", ip)
                if (mac != null) put("mac_address", mac)
            }.toString()
            val body = json.toRequestBody(JSON_MEDIA)
            val req = Request.Builder().url("$baseUrl/devices/register").post(body).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    refreshAll()
                    return@withContext Result.success(Unit)
                } else {
                    return@withContext Result.failure(Exception("Registration failed: HTTP ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
}
