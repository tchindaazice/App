package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceInfo
import com.example.data.model.UserSession
import com.example.data.model.VpnConfig
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusDarkBg
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextMuted
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary

@Composable
fun SettingsScreen(
    config: VpnConfig,
    session: UserSession?,
    deviceInfo: DeviceInfo,
    onSaveConfig: (VpnConfig) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var webPanelUrl by remember(config.webPanelUrl) { mutableStateOf(config.webPanelUrl) }
    var transport by remember(config.transport) { mutableStateOf(config.transport) }
    var customSni by remember(config.customSni) { mutableStateOf(config.customSni) }
    var dnsPrimary by remember(config.dnsPrimary) { mutableStateOf(config.dnsPrimary) }
    var dnsSecondary by remember(config.dnsSecondary) { mutableStateOf(config.dnsSecondary) }
    var bypassLan by remember(config.bypassLan) { mutableStateOf(config.bypassLan) }
    var killSwitch by remember(config.killSwitchEnabled) { mutableStateOf(config.killSwitchEnabled) }
    var enableMux by remember(config.enableMux) { mutableStateOf(config.enableMux) }

    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusDarkBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("settings_screen")
    ) {
        // Settings Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Paramètres & Sécurité",
                color = NexusTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: LIAISON WEB PANEL
        SettingsSectionHeader(title = "LIAISON WEB PANEL DOCKER", icon = Icons.Default.Cloud)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NexusSurface)
                .border(1.dp, NexusCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "URL du Gestionnaire VLESS :",
                    color = NexusTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = webPanelUrl,
                    onValueChange = { webPanelUrl = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NexusSurfaceVariant,
                        unfocusedContainerColor = NexusSurfaceVariant,
                        focusedBorderColor = NexusCyan,
                        unfocusedBorderColor = NexusCardBorder,
                        focusedTextColor = NexusTextPrimary,
                        unfocusedTextColor = NexusTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("web_panel_url_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            webPanelUrl = "https://nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio"
                        }
                    ) {
                        Text("Rétablir par défaut", color = NexusCyan, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 2: PROTOCOLE VLESS & SÉCURITÉ
        SettingsSectionHeader(title = "OPTIONS PROTOCOLE VLESS", icon = Icons.Default.Security)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NexusSurface)
                .border(1.dp, NexusCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                // Transport Mode
                Text(text = "Mode de Transport", color = NexusTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("WebSocket + TLS", "gRPC + TLS", "TCP + XTLS").forEach { mode ->
                        val isSelected = transport == mode
                        Surface(
                            onClick = { transport = mode },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) NexusCyan else NexusSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NexusCyan else NexusCardBorder
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = mode,
                                color = if (isSelected) Color.Black else NexusTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom SNI / Host
                Text(text = "SNI / Host Spoofing", color = NexusTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customSni,
                    onValueChange = { customSni = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NexusSurfaceVariant,
                        unfocusedContainerColor = NexusSurfaceVariant,
                        focusedBorderColor = NexusCyan,
                        unfocusedBorderColor = NexusCardBorder,
                        focusedTextColor = NexusTextPrimary,
                        unfocusedTextColor = NexusTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // DNS settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "DNS Principal", color = NexusTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = dnsPrimary,
                            onValueChange = { dnsPrimary = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NexusSurfaceVariant,
                                unfocusedContainerColor = NexusSurfaceVariant,
                                focusedBorderColor = NexusCyan,
                                unfocusedBorderColor = NexusCardBorder,
                                focusedTextColor = NexusTextPrimary,
                                unfocusedTextColor = NexusTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "DNS Secondaire", color = NexusTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = dnsSecondary,
                            onValueChange = { dnsSecondary = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NexusSurfaceVariant,
                                unfocusedContainerColor = NexusSurfaceVariant,
                                focusedBorderColor = NexusCyan,
                                unfocusedBorderColor = NexusCardBorder,
                                focusedTextColor = NexusTextPrimary,
                                unfocusedTextColor = NexusTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Kill Switch Toggle
                SettingSwitchRow(
                    title = "Kill-Switch (Blocage de Fuite)",
                    subtitle = "Couper tout le trafic si le VPN se déconnecte",
                    checked = killSwitch,
                    onCheckedChange = { killSwitch = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bypass LAN Toggle
                SettingSwitchRow(
                    title = "Contourner le Réseau Local (LAN)",
                    subtitle = "Permettre l'accès aux imprimantes et routeurs locaux",
                    checked = bypassLan,
                    onCheckedChange = { bypassLan = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mux Toggle
                SettingSwitchRow(
                    title = "Multiplexage Connexions (Mux)",
                    subtitle = "Optimise le ping et la bande passante",
                    checked = enableMux,
                    onCheckedChange = { enableMux = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Config Button
        Button(
            onClick = {
                val newConfig = config.copy(
                    webPanelUrl = webPanelUrl.trim(),
                    transport = transport,
                    customSni = customSni.trim(),
                    dnsPrimary = dnsPrimary.trim(),
                    dnsSecondary = dnsSecondary.trim(),
                    killSwitchEnabled = killSwitch,
                    bypassLan = bypassLan,
                    enableMux = enableMux
                )
                onSaveConfig(newConfig)
                Toast.makeText(context, "Paramètres VLESS enregistrés !", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NexusCyan, contentColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_settings_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ENREGISTRER LA CONFIGURATION", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: PROFIL & APPAREIL LIÉ
        SettingsSectionHeader(title = "PROFIL & MATÉRIEL SÉCURISÉ", icon = Icons.Default.Smartphone)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NexusSurface)
                .border(1.dp, NexusCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                ProfileInfoRow(label = "Nom", value = session?.fullName ?: "Non défini")
                ProfileInfoRow(label = "Pays", value = "${session?.countryFlag ?: "🌍"} ${session?.countryName ?: "Afrique"}")
                ProfileInfoRow(label = session?.channel?.label ?: "Contact", value = session?.contactDetail ?: "Non défini")
                ProfileInfoRow(label = "UID Attribué", value = session?.uid ?: "Non attribué", isMono = true)
                ProfileInfoRow(label = "Empreinte ID Appareil", value = deviceInfo.deviceId, isMono = true)
                ProfileInfoRow(label = "Modèle Appareil", value = "${deviceInfo.manufacturer} ${deviceInfo.model}")
                ProfileInfoRow(label = "Système", value = deviceInfo.androidVersion)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Logout / Reset
        OutlinedButton(
            onClick = { showLogoutDialog = true },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, NexusRed.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_button")
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = NexusRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Réinitialiser le profil / Déconnexion", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = NexusSurface,
            title = {
                Text("Déconnexion & Réinitialisation", color = NexusTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Voulez-vous réinitialiser votre session VPN ? Vous devrez à nouveau renseigner votre adhésion ou activer un token.",
                    color = NexusTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusRed, contentColor = Color.White)
                ) {
                    Text("Oui, réinitialiser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Annuler", color = NexusTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = NexusCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = NexusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = NexusTextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = NexusCyan,
                uncheckedThumbColor = NexusTextMuted,
                uncheckedTrackColor = NexusSurfaceVariant
            )
        )
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = NexusTextSecondary, fontSize = 12.sp)
        Text(
            text = value,
            color = NexusTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default
        )
    }
}
