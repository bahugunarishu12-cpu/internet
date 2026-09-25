package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ActivityViewModel
import com.example.ui.viewmodel.AppTab

@Composable
fun DashboardScreen(
    viewModel: ActivityViewModel,
    onNavigateToDevice: (String) -> Unit
) {
    val devices by viewModel.devices.collectAsState()
    val networkActivities by viewModel.networkActivities.collectAsState()
    val chromeSearches by viewModel.chromeSearches.collectAsState()
    val systemStatus by viewModel.systemStatus.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val totalDevices = devices.size
    val onlineDevices = devices.count { it.status == "online" }
    val offlineDevices = devices.count { it.status == "offline" }
    val domainCount = networkActivities.size
    val searchCount = chromeSearches.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Backend / Database Error Banner
        if (!systemStatus.databaseConnected || systemStatus.databaseError != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Database Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Database connection unavailable.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Ensure MongoDB is running on the backend host.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Router Integration Error Banner
        if (systemStatus.discoveryError != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = "Router Warning",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Unable to discover devices. Router/network integration is not configured.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = systemStatus.discoveryError ?: "",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }

        // Connection Error to Backend
        if (errorMessage != null && systemStatus.databaseConnected) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(text = errorMessage ?: "", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Metrics Section Header with Refresh Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Network Status",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = { viewModel.refresh() }, enabled = !isRefreshing) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Total",
                    value = totalDevices.toString(),
                    subtitle = "Discovered",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Online",
                    value = onlineDevices.toString(),
                    subtitle = "Detected",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Offline",
                    value = offlineDevices.toString(),
                    subtitle = "Absent",
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Network Events",
                    value = domainCount.toString(),
                    subtitle = "Domain queries",
                    color = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Chrome Searches",
                    value = searchCount.toString(),
                    subtitle = "Extension searches",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Network Activity Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                    Text("Recent Domain Activity", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                TextButton(onClick = { viewModel.selectTab(AppTab.NETWORK) }) {
                    Text("View All", fontSize = 12.sp)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }

        if (networkActivities.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No domain activity recorded in database yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(networkActivities.take(4)) { net ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToDevice(net.deviceId) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0284C7)))
                            Column {
                                Text(
                                    text = net.domain,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${net.deviceId} • IP: ${net.ipAddress}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
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

        // Recent Chrome Searches Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    Text("Authorized Chrome Searches", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                TextButton(onClick = { viewModel.selectTab(AppTab.SEARCHES) }) {
                    Text("View All", fontSize = 12.sp)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }

        if (chromeSearches.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No Chrome searches received from extension yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(chromeSearches.take(4)) { search ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToDevice(search.deviceId) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                            Column {
                                Text(
                                    text = "\"${search.searchQuery}\"",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${search.searchEngine} • ${search.deviceId}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
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

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
