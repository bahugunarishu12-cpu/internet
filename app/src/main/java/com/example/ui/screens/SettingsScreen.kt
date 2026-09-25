package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ActivityViewModel

@Composable
fun SettingsScreen(viewModel: ActivityViewModel) {
    val context = LocalContext.current
    val currentServerUrl by viewModel.serverUrl.collectAsState()
    val systemStatus by viewModel.systemStatus.collectAsState()
    var inputServerUrl by remember(currentServerUrl) { mutableStateOf(currentServerUrl) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Backend & Router Configuration",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Admin client connection parameters and network discovery diagnostics",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Backend Connection Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("FastAPI Server Connection", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "Specify the IP or hostname of the machine running the FastAPI backend. For emulator use http://10.0.2.2:8000; for physical phone/tablet use your computer's LAN IP (e.g. http://192.168.1.100:8000).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    OutlinedTextField(
                        value = inputServerUrl,
                        onValueChange = { inputServerUrl = it },
                        label = { Text("FastAPI Server Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.updateServerUrl(inputServerUrl)
                            Toast.makeText(context, "Server URL updated and saved", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Connect")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Live Status indicators
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MongoDB Connection", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = if (systemStatus.databaseConnected) "Connected" else "Database connection unavailable.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (systemStatus.databaseConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Discovery Adapter", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = if (systemStatus.discoveryConfigured && systemStatus.discoveryError == null) "Active" else "Integration required",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (systemStatus.discoveryConfigured && systemStatus.discoveryError == null) Color(0xFF10B981) else Color(0xFFD97706)
                        )
                    }
                }
            }
        }

        // Router Integration & Jio Fiber Diagnostics
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Router, contentDescription = null, tint = Color(0xFF0284C7))
                        Text("Jio Router & Network Discovery Guide", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Text(
                        text = "JioFiber Home Gateways (typically at 192.168.29.1) do not expose an open, unauthenticated REST API for connected DHCP leases. To automatically discover devices:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("1. Run the FastAPI backend on any device on your Wi-Fi (PC, Raspberry Pi, server).", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text("2. The backend uses the host ARP table & network sweeps (ROUTER_TYPE=arp_subnet) to automatically discover connected IPs and MAC addresses without modifying router firmware.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text("3. For Jio direct gateway login, set JIO_ROUTER_PASSWORD in backend/.env.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text("4. Discovered devices are saved persistently in MongoDB and sync every 10 seconds.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        // Privacy Guarantees Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981))
                        Text(
                            text = "Privacy Safeguards & No Mock Data Policy",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "• Real Data Only: The app does not display mock, dummy, or randomly generated devices.\n" +
                                "• Zero HTTPS Inspection: Domain queries record host addresses only. Payloads are never decrypted.\n" +
                                "• Consent-Based Search Capture: Chrome search keywords are strictly received from authorized extension pairing.\n" +
                                "• Persistent State: Devices, first_seen, last_seen, and pairing codes survive all application restarts in MongoDB.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
