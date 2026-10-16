package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionStats
import com.example.data.model.DynamicServerConfig
import com.example.data.model.UserSession
import com.example.data.model.VpnStatus
import com.example.ui.components.MetricTile
import com.example.ui.components.QuotaGauge
import com.example.ui.components.VpnPowerButton
import com.example.utils.FormatUtils
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusDarkBg
import com.example.ui.theme.NexusGlowCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextMuted
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import java.util.Locale

@Composable
fun DashboardScreen(
    session: UserSession,
    vpnStatus: VpnStatus,
    stats: ConnectionStats,
    serverConfig: DynamicServerConfig? = null,
    isSyncing: Boolean = false,
    onToggleVpn: () -> Unit,
    onSyncAccount: () -> Unit,
    onImportVless: ((String) -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusDarkBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .testTag("dashboard_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP BAR WITH THREE DOTS MENU
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NexusSurfaceVariant)
                        .border(1.dp, NexusCardBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = NexusCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = session.countryFlag,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = session.fullName,
                            color = NexusTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Vérifié",
                            tint = NexusCyan,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "ID : ${FormatUtils.formatMaskedUid(session.uid)}",
                        color = NexusCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("dashboard_more_menu_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = NexusCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = NexusTextPrimary
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(NexusSurface)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = NexusCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Actualiser le compte", color = NexusTextPrimary, fontSize = 14.sp)
                            }
                        },
                        onClick = {
                            showMenu = false
                            onSyncAccount()
                        },
                        modifier = Modifier.testTag("menu_sync_account")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SERVER TARGET & SNI INFO CHIP
        val sAddress = serverConfig?.serverAddress ?: "192.179.18.100"
        val sPort = serverConfig?.serverPort ?: 443
        val sSni = serverConfig?.sni ?: "crashlyticsreports-pa.googleapis.com"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(NexusSurfaceVariant.copy(alpha = 0.6f))
                .border(1.dp, NexusCardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VLESS: $sAddress:$sPort",
                    color = NexusCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "SNI: ${if (sSni.length > 20) sSni.take(17) + "..." else sSni}",
                    color = NexusTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // REAL-TIME DATA QUOTA GAUGE
        QuotaGauge(
            usedBytes = session.quotaUsedBytes,
            totalBytes = session.quotaTotalBytes,
            expirationTimestamp = session.expirationTimestamp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // MAIN HERO VPN CONNECT / DISCONNECT BUTTON
        VpnPowerButton(
            status = vpnStatus,
            onClick = onToggleVpn
        )

        Spacer(modifier = Modifier.height(24.dp))

        // LIVE REAL METRICS (Download, Upload, Ping, Duration)
        val downloadSpeedFormatted = if (vpnStatus == VpnStatus.CONNECTED) {
            FormatUtils.formatSpeed(stats.downloadSpeedBps)
        } else "0.0 Ko/s"

        val uploadSpeedFormatted = if (vpnStatus == VpnStatus.CONNECTED) {
            FormatUtils.formatSpeed(stats.uploadSpeedBps)
        } else "0.0 Ko/s"

        val pingFormatted = if (vpnStatus == VpnStatus.CONNECTED) "${stats.pingMs} ms" else "-- ms"
        val durationFormatted = formatDuration(stats.connectedDurationSeconds)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                icon = Icons.Default.ArrowDownward,
                title = "Téléchargement",
                value = downloadSpeedFormatted,
                accentColor = NexusCyan,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                icon = Icons.Default.ArrowUpward,
                title = "Envoi",
                value = uploadSpeedFormatted,
                accentColor = NexusGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                icon = Icons.Default.Speed,
                title = "Ping / Latence",
                value = pingFormatted,
                accentColor = if (stats.pingMs in 1..60) NexusGreen else NexusCyan,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                icon = Icons.Default.Timer,
                title = "Durée Session",
                value = durationFormatted,
                accentColor = NexusTextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}
