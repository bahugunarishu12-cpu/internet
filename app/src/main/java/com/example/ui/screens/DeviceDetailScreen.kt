package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.viewmodel.ActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceId: String,
    viewModel: ActivityViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val devices by viewModel.devices.collectAsState()
    val allNetwork by viewModel.networkActivities.collectAsState()
    val allSearches by viewModel.chromeSearches.collectAsState()
    val generatedPairingCode by viewModel.newPairingCode.collectAsState()

    val device = devices.find { it.deviceId == deviceId }
    val deviceNetwork = allNetwork.filter { it.deviceId == deviceId }
    val deviceSearches = allSearches.filter { it.deviceId == deviceId }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    if (device == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Device not found in current inventory")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onBack) { Text("Go Back") }
            }
        }
        return
    }

    val isOnline = device.status.equals("online", ignoreCase = true)
    val statusColor = if (isOnline) Color(0xFF10B981) else MaterialTheme.colorScheme.outline

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device.deviceName, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // Device Hardware & Network Header Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = device.deviceName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Device ID: ${device.deviceId}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = statusColor.copy(alpha = 0.2f)
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

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("IP Address", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Text(device.ipAddress, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("MAC Address", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Text(device.macAddress ?: "None", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Last Seen", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Text(device.lastSeen.split(" ").getOrElse(1) { device.lastSeen }, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Extension pairing status & action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Chrome Extension Status", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = if (device.pairedCode != null) "Paired (${device.pairedCode})" else "Not Paired",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (device.pairedCode != null) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    viewModel.generatePairingCode(device.deviceId) { code ->
                                        if (code != null) {
                                            Toast.makeText(context, "Pairing code generated: $code", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to connect to backend", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Generate Code", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Tab bar for Domain vs Search activity
            item {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Network Domains (${deviceNetwork.size})") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Chrome Searches (${deviceSearches.size})") }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                if (deviceNetwork.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No domain activity recorded in database for this device.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    items(deviceNetwork) { net ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = net.domain,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = net.timestamp.split(" ").getOrElse(1) { net.timestamp },
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            } else {
                if (deviceSearches.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No Chrome search query activity recorded for this device.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    items(deviceSearches) { search ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "\"${search.searchQuery}\"",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${search.searchEngine} • ${search.browser}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Text(
                                    text = search.timestamp.split(" ").getOrElse(1) { search.timestamp },
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Pairing dialog if triggered
    if (generatedPairingCode != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPairingDialog() },
            title = { Text("Extension Pairing Code") },
            text = {
                Text("Pairing Code: $generatedPairingCode\nEnter this in the extension to bind $deviceId.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Pairing Code", generatedPairingCode)
                        clipboard.setPrimaryClip(clip)
                        viewModel.clearPairingDialog()
                        Toast.makeText(context, "Copied code", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Copy & Dismiss") }
            }
        )
    }
}
