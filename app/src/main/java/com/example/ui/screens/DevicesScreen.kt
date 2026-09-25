package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Device
import com.example.ui.viewmodel.ActivityViewModel

@Composable
fun DevicesScreen(
    viewModel: ActivityViewModel,
    onSelectDevice: (String) -> Unit
) {
    val context = LocalContext.current
    val devices by viewModel.devices.collectAsState()
    val searchQuery by viewModel.deviceSearchQuery.collectAsState()
    val generatedPairingCode by viewModel.newPairingCode.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showRegisterDialog by remember { mutableStateOf(false) }
    var selectedDeviceForPairing by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    val filteredDevices = devices.filter {
        it.deviceName.contains(searchQuery, ignoreCase = true) ||
        it.deviceId.contains(searchQuery, ignoreCase = true) ||
        it.ipAddress.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Search and Sync Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setDeviceSearch(it) },
                        label = { Text("Search by name, ID or IP") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setDeviceSearch("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    FilledTonalButton(
                        onClick = {
                            isSyncing = true
                            viewModel.triggerDeviceSync { resultMsg ->
                                isSyncing = false
                                Toast.makeText(context, resultMsg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isSyncing && !isRefreshing,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync", fontSize = 12.sp)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discovered Devices (${filteredDevices.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Auto-syncs every 10s",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (filteredDevices.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No authorized devices discovered yet.",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap 'Sync' to trigger discovery or verify router integration in Settings.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(filteredDevices) { device ->
                    DeviceCard(
                        device = device,
                        onSelect = { onSelectDevice(device.deviceId) },
                        onGeneratePairing = {
                            selectedDeviceForPairing = device.deviceId
                            viewModel.generatePairingCode(device.deviceId) { code ->
                                if (code == null) {
                                    Toast.makeText(context, "Failed to connect to backend", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        // Floating Action Button to Register Device Manually
        FloatingActionButton(
            onClick = { showRegisterDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Register Device")
        }
    }

    // Register Device Dialog
    if (showRegisterDialog) {
        RegisterDeviceDialog(
            onDismiss = { showRegisterDialog = false },
            onConfirm = { id, name, ip, mac ->
                viewModel.registerDevice(id, name, ip, mac) { success, msg ->
                    showRegisterDialog = false
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Pairing Code Dialog
    if (generatedPairingCode != null && selectedDeviceForPairing != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPairingDialog() },
            icon = { Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFF59E0B)) },
            title = { Text("Chrome Extension Pairing Code") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter this one-time code in the Chrome extension popup for $selectedDeviceForPairing:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = generatedPairingCode ?: "",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Code valid for 24h. No password or cookie data is ever shared.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Pairing Code", generatedPairingCode)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied pairing code to clipboard", Toast.LENGTH_SHORT).show()
                        viewModel.clearPairingDialog()
                    }
                ) {
                    Text("Copy & Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearPairingDialog() }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun DeviceCard(
    device: Device,
    onSelect: () -> Unit,
    onGeneratePairing: () -> Unit
) {
    val isOnline = device.status.equals("online", ignoreCase = true)
    val isOffline = device.status.equals("offline", ignoreCase = true)

    val statusColor = when {
        isOnline -> Color(0xFF10B981)
        isOffline -> MaterialTheme.colorScheme.outline
        else -> Color(0xFFF59E0B)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Laptop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = device.deviceName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ID: ${device.deviceId}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Status chip (ONLINE, OFFLINE, UNKNOWN)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = device.status.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tech specs row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("IP Address", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(device.ipAddress, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("MAC Address", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(device.macAddress ?: "None", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Last Seen", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        device.lastSeen.split(" ").getOrElse(1) { device.lastSeen },
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (device.pairedCode != null) {
                    Text(
                        text = "Paired: ${device.pairedCode}",
                        fontSize = 11.sp,
                        color = Color(0xFFF59E0B),
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = "No extension paired",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onGeneratePairing,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pair Code", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onSelect,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Details", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterDeviceDialog(
    onDismiss: () -> Unit,
    onConfirm: (id: String, name: String, ip: String, mac: String?) -> Unit
) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("192.168.") }
    var mac by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Authorized Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Add an authorized host to the network monitor repository.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it.trim().lowercase() },
                    label = { Text("Device ID * (e.g. dev-laptop-01)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name * (e.g. Primary Laptop)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address * (e.g. 192.168.1.55)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mac,
                    onValueChange = { mac = it },
                    label = { Text("MAC Address (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (id.isNotBlank() && name.isNotBlank() && ip.isNotBlank()) {
                        onConfirm(id, name, ip, if (mac.isBlank()) null else mac)
                    }
                },
                enabled = id.isNotBlank() && name.isNotBlank() && ip.isNotBlank()
            ) {
                Text("Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
