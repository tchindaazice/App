package com.example

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ApprovalStatus
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.PendingApprovalScreen
import com.example.ui.screens.RegistrationScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusDarkBg
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusTextMuted
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.viewmodel.MainTab
import com.example.viewmodel.NexusViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NexusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }

                // Android VpnService.prepare launcher
                val vpnPrepareLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        viewModel.toggleVpnConnection(context)
                    } else {
                        Toast.makeText(context, "Permission VPN requise pour continuer.", Toast.LENGTH_SHORT).show()
                    }
                }

                // Show feedback message if any
                LaunchedEffect(uiState.feedbackMessage) {
                    uiState.feedbackMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.dismissFeedback()
                    }
                }

                val onToggleVpnWithPermission = {
                    val prepareIntent = VpnService.prepare(context)
                    if (prepareIntent != null) {
                        vpnPrepareLauncher.launch(prepareIntent)
                    } else {
                        viewModel.toggleVpnConnection(context)
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        // Show bottom navigation only when member is approved
                        if (uiState.approvalStatus == ApprovalStatus.APPROVED && uiState.userSession != null) {
                            NavigationBar(
                                containerColor = NexusSurface,
                                tonalElevation = 8.dp,
                                modifier = Modifier
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .testTag("main_bottom_nav")
                            ) {
                                NavigationBarItem(
                                    selected = uiState.activeTab == MainTab.DASHBOARD,
                                    onClick = { viewModel.selectTab(MainTab.DASHBOARD) },
                                    icon = {
                                        Icon(
                                            Icons.Default.Security,
                                            contentDescription = "Accueil VPN"
                                        )
                                    },
                                    label = { Text("VPN", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = NexusCyan,
                                        indicatorColor = NexusCyan,
                                        unselectedIconColor = NexusTextSecondary,
                                        unselectedTextColor = NexusTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_vpn")
                                )

                                NavigationBarItem(
                                    selected = uiState.activeTab == MainTab.LOGS,
                                    onClick = { viewModel.selectTab(MainTab.LOGS) },
                                    icon = {
                                        Icon(
                                            Icons.Default.Terminal,
                                            contentDescription = "Logs"
                                        )
                                    },
                                    label = { Text("Logs", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = NexusCyan,
                                        indicatorColor = NexusCyan,
                                        unselectedIconColor = NexusTextSecondary,
                                        unselectedTextColor = NexusTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_logs")
                                )

                                NavigationBarItem(
                                    selected = uiState.activeTab == MainTab.SETTINGS,
                                    onClick = { viewModel.selectTab(MainTab.SETTINGS) },
                                    icon = {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Paramètres"
                                        )
                                    },
                                    label = { Text("Paramètres", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = NexusCyan,
                                        indicatorColor = NexusCyan,
                                        unselectedIconColor = NexusTextSecondary,
                                        unselectedTextColor = NexusTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_tab_settings")
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NexusDarkBg)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when {
                            // 1. User has an approved session -> Show Active VPN Experience
                            uiState.approvalStatus == ApprovalStatus.APPROVED && uiState.userSession != null -> {
                                when (uiState.activeTab) {
                                    MainTab.DASHBOARD -> DashboardScreen(
                                        session = uiState.userSession!!,
                                        vpnStatus = uiState.vpnStatus,
                                        stats = uiState.stats,
                                        config = uiState.config,
                                        onToggleVpn = onToggleVpnWithPermission
                                    )
                                    MainTab.LOGS -> LogsScreen(
                                        logs = uiState.logs,
                                        onClearLogs = { viewModel.clearLogs() }
                                    )
                                    MainTab.SETTINGS -> SettingsScreen(
                                        config = uiState.config,
                                        session = uiState.userSession,
                                        deviceInfo = uiState.deviceInfo,
                                        onSaveConfig = { viewModel.updateConfig(it) },
                                        onLogout = { viewModel.logout() }
                                    )
                                }
                            }

                            // 2. Request submitted and pending approval
                            uiState.approvalStatus == ApprovalStatus.PENDING -> {
                                PendingApprovalScreen(
                                    request = uiState.savedRegistration,
                                    deviceInfo = uiState.deviceInfo,
                                    approvalStatus = uiState.approvalStatus,
                                    isChecking = uiState.isCheckingStatus,
                                    webPanelUrl = uiState.config.webPanelUrl,
                                    onCheckStatus = { viewModel.checkApprovalStatus() },
                                    onActivateToken = { viewModel.activateToken(it) },
                                    onSimulateAdminApproval = { viewModel.simulateAdminApproval() },
                                    onEditRequest = { viewModel.logout() }
                                )
                            }

                            // 3. Initial state: Registration form with African countries dropdown, channels, device bind
                            else -> {
                                RegistrationScreen(
                                    deviceInfo = uiState.deviceInfo,
                                    isSubmitting = uiState.isSubmitting,
                                    onSubmit = { name, country, channel, contact ->
                                        viewModel.submitRegistration(name, country, channel, contact)
                                    },
                                    onActivateToken = { viewModel.activateToken(it) },
                                    onSimulateAdminApproval = { viewModel.simulateAdminApproval() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
