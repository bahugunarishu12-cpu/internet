package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ActivityViewModel

@Composable
fun ChromeSearchesScreen(
    viewModel: ActivityViewModel,
    onSelectDevice: (String) -> Unit
) {
    val searches by viewModel.chromeSearches.collectAsState()
    val searchQuery by viewModel.chromeSearchFilter.collectAsState()
    var selectedEngine by remember { mutableStateOf<String?>(null) }

    val engines = listOf("All", "Google", "Bing", "DuckDuckGo", "Yahoo")

    val filtered = searches.filter {
        val matchesQuery = it.searchQuery.contains(searchQuery, ignoreCase = true) ||
                it.deviceId.contains(searchQuery, ignoreCase = true)
        val matchesEngine = selectedEngine == null || selectedEngine == "All" || it.searchEngine.equals(selectedEngine, ignoreCase = true)
        matchesQuery && matchesEngine
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Privacy pill
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AUTHORIZED EXTENSION ONLY: Search keywords extracted from supported search-engine result URLs. Never captures passwords, cookies, or page contents.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setChromeSearchFilter(it) },
                label = { Text("Filter search queries or device...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setChromeSearchFilter("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Engine Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(engines) { eng ->
                    val isSelected = (selectedEngine == null && eng == "All") || selectedEngine == eng
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedEngine = if (eng == "All") null else eng },
                        label = { Text(eng, fontSize = 12.sp) }
                    )
                }
            }
        }

        item {
            Text(
                text = "Authorized Searches (${filtered.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Chrome search records match criteria", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            items(filtered) { item ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "\"${item.searchQuery}\"",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = item.searchEngine,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Device: ${item.deviceId}  •  Browser: ${item.browser}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = item.timestamp.split(" ").getOrElse(1) { item.timestamp },
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
