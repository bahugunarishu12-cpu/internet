package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ActivityViewModel
import com.example.ui.viewmodel.AppTab

class MainActivity : ComponentActivity() {

    private val viewModel: ActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: ActivityViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedDeviceId by viewModel.selectedDeviceId.collectAsState()

    // If a device is selected, show detail screen
    if (selectedDeviceId != null) {
        DeviceDetailScreen(
            deviceId = selectedDeviceId!!,
            viewModel = viewModel,
            onBack = { viewModel.selectDevice(null) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Net & Chrome Monitor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "Private Network • Consent Enforced",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationItem(
                    tab = AppTab.DASHBOARD,
                    currentTab = currentTab,
                    icon = Icons.Default.Dashboard,
                    label = "Overview",
                    onClick = { viewModel.selectTab(AppTab.DASHBOARD) }
                )
                NavigationItem(
                    tab = AppTab.DEVICES,
                    currentTab = currentTab,
                    icon = Icons.Default.Laptop,
                    label = "Devices",
                    onClick = { viewModel.selectTab(AppTab.DEVICES) }
                )
                NavigationItem(
                    tab = AppTab.NETWORK,
                    currentTab = currentTab,
                    icon = Icons.Default.Public,
                    label = "Domains",
                    onClick = { viewModel.selectTab(AppTab.NETWORK) }
                )
                NavigationItem(
                    tab = AppTab.SEARCHES,
                    currentTab = currentTab,
                    icon = Icons.Default.Search,
                    label = "Searches",
                    onClick = { viewModel.selectTab(AppTab.SEARCHES) }
                )
                NavigationItem(
                    tab = AppTab.SETTINGS,
                    currentTab = currentTab,
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    onClick = { viewModel.selectTab(AppTab.SETTINGS) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToDevice = { deviceId -> viewModel.selectDevice(deviceId) }
                )
                AppTab.DEVICES -> DevicesScreen(
                    viewModel = viewModel,
                    onSelectDevice = { deviceId -> viewModel.selectDevice(deviceId) }
                )
                AppTab.NETWORK -> NetworkActivityScreen(
                    viewModel = viewModel,
                    onSelectDevice = { deviceId -> viewModel.selectDevice(deviceId) }
                )
                AppTab.SEARCHES -> ChromeSearchesScreen(
                    viewModel = viewModel,
                    onSelectDevice = { deviceId -> viewModel.selectDevice(deviceId) }
                )
                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RowScope.NavigationItem(
    tab: AppTab,
    currentTab: AppTab,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = currentTab == tab,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = label) },
        label = { Text(label, fontSize = 11.sp) }
    )
}
