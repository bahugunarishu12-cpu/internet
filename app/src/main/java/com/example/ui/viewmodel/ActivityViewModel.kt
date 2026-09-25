package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChromeSearchActivity
import com.example.data.model.Device
import com.example.data.model.NetworkDomainActivity
import com.example.data.model.SystemStatus
import com.example.data.repository.ActivityRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    DEVICES("Devices"),
    NETWORK("Network Activity"),
    SEARCHES("Chrome Searches"),
    SETTINGS("Settings")
}

class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ActivityRepository(application.applicationContext)

    val devices: StateFlow<List<Device>> = repository.devices
    val networkActivities: StateFlow<List<NetworkDomainActivity>> = repository.networkActivities
    val chromeSearches: StateFlow<List<ChromeSearchActivity>> = repository.chromeSearches
    val systemStatus: StateFlow<SystemStatus> = repository.systemStatus
    val isRefreshing: StateFlow<Boolean> = repository.isRefreshing
    val errorMessage: StateFlow<String?> = repository.errorMessage
    val serverUrl: StateFlow<String> = repository.serverUrl

    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId: StateFlow<String?> = _selectedDeviceId.asStateFlow()

    private val _deviceSearchQuery = MutableStateFlow("")
    val deviceSearchQuery: StateFlow<String> = _deviceSearchQuery.asStateFlow()

    private val _domainSearchQuery = MutableStateFlow("")
    val domainSearchQuery: StateFlow<String> = _domainSearchQuery.asStateFlow()

    private val _chromeSearchFilter = MutableStateFlow("")
    val chromeSearchFilter: StateFlow<String> = _chromeSearchFilter.asStateFlow()

    private val _newPairingCode = MutableStateFlow<String?>(null)
    val newPairingCode: StateFlow<String?> = _newPairingCode.asStateFlow()

    init {
        // Start automatic polling every 10 seconds against the real backend
        viewModelScope.launch {
            while (true) {
                repository.refreshAll()
                delay(10_000)
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectDevice(deviceId: String?) {
        _selectedDeviceId.value = deviceId
    }

    fun setDeviceSearch(query: String) {
        _deviceSearchQuery.value = query
    }

    fun setDomainSearch(query: String) {
        _domainSearchQuery.value = query
    }

    fun setChromeSearchFilter(query: String) {
        _chromeSearchFilter.value = query
    }

    fun updateServerUrl(url: String) {
        repository.updateServerUrl(url)
        viewModelScope.launch {
            repository.refreshAll()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refreshAll()
        }
    }

    fun triggerDeviceSync(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.triggerSync()
            onResult(res)
        }
    }

    fun registerDevice(id: String, name: String, ip: String, mac: String?, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.registerDevice(id, name, ip, mac)
            result.fold(
                onSuccess = { onComplete(true, "Device registered successfully") },
                onFailure = { onComplete(false, it.message ?: "Registration failed") }
            )
        }
    }

    fun generatePairingCode(deviceId: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.generatePairingCode(deviceId)
            result.fold(
                onSuccess = { code ->
                    _newPairingCode.value = code
                    onComplete(code)
                },
                onFailure = {
                    onComplete(null)
                }
            )
        }
    }

    fun clearPairingDialog() {
        _newPairingCode.value = null
    }
}
