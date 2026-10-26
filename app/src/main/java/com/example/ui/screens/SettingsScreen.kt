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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import com.example.data.model.DeviceInfo
import com.example.data.model.DynamicServerConfig
import com.example.data.model.UserSession
import com.example.data.model.VpnPreferences
import com.example.utils.FormatUtils
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
    preferences: VpnPreferences,
    session: UserSession?,
    serverConfig: DynamicServerConfig? = null,
    serverUrl: String = "https://nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio/",
    deviceInfo: DeviceInfo,
    isSyncing: Boolean = false,
    onSavePreferences: (VpnPreferences) -> Unit,
    onSaveServerConfig: ((DynamicServerConfig) -> Unit)? = null,
    onUpdateServerUrl: ((String) -> Unit)? = null,
    onImportVless: ((String) -> Unit)? = null,
    onSyncAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var webPanelUrlText by remember(serverUrl) { mutableStateOf(serverUrl) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importInputText by remember { mutableStateOf("") }
    var showEditUuidDialog by remember { mutableStateOf(false) }
    var editUuidText by remember { mutableStateOf("") }
    var showXrayJsonDialog by remember { mutableStateOf(false) }

    var dnsPrimary by remember(preferences.dnsPrimary) { mutableStateOf(preferences.dnsPrimary) }
    var dnsSecondary by remember(preferences.dnsSecondary) { mutableStateOf(preferences.dnsSecondary) }
    var mtuText by remember(preferences.mtu) { mutableStateOf(preferences.mtu.toString()) }
    var bypassLan by remember(preferences.bypassLan) { mutableStateOf(preferences.bypassLan) }
    var killSwitch by remember(preferences.killSwitchEnabled) { mutableStateOf(preferences.killSwitchEnabled) }
    var autoBoot by remember(preferences.autoConnectOnBoot) { mutableStateOf(preferences.autoConnectOnBoot) }
    var showNotifStats by remember(preferences.showNotificationStats) { mutableStateOf(preferences.showNotificationStats) }

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
                text = "Paramètres & Options Sécurisées",
                color = NexusTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 1: SERVEUR WEB PANEL
        SectionHeader(title = "SERVEUR WEB PANEL NEXUS TUNNEL", icon = Icons.Default.Cloud)
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
                    text = "URL de l'API Web Panel (Cloud Run / VPS)",
                    color = NexusTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = webPanelUrlText,
                    onValueChange = { webPanelUrlText = it },
                    singleLine = true,
                    placeholder = { Text("https://...", fontSize = 12.sp, color = NexusTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NexusSurfaceVariant,
                        unfocusedContainerColor = NexusSurfaceVariant,
                        focusedBorderColor = NexusCyan,
                        unfocusedBorderColor = NexusCardBorder,
                        focusedTextColor = NexusTextPrimary,
                        unfocusedTextColor = NexusTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("web_panel_url_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (webPanelUrlText.isNotBlank()) {
                                onUpdateServerUrl?.invoke(webPanelUrlText)
                                Toast.makeText(context, "URL du Web Panel mise à jour !", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NexusCyan, contentColor = Color.Black),
                        modifier = Modifier.weight(1f).testTag("save_server_url_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enregistrer l'URL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onSyncAccount,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexusCyan),
                        modifier = Modifier.weight(1f).testTag("sync_panel_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(color = NexusCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Synchroniser", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 2: PARAMÈTRES PROTOCOLE VLESS / XRAY
        SectionHeader(title = "SERVEUR CIBLE VLESS & XRAY CORE", icon = Icons.Default.Router)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NexusSurface)
                .border(1.dp, NexusCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            val sAddress = serverConfig?.serverAddress ?: "192.179.18.100"
            val sPort = serverConfig?.serverPort ?: 443
            val sSni = serverConfig?.sni ?: "crashlyticsreports-pa.googleapis.com"
            val sHost = serverConfig?.headerHost ?: "nexus-tunnel-d-ploiement-gestionnaire-vless.ai.studio"
            val sPath = serverConfig?.wsPath ?: "/nexustunnel"
            val sUuid = serverConfig?.uuid?.takeIf { it.isNotBlank() } ?: session?.uid ?: "83d5a4b2-a45b-48c1-9012-3456789abcde"

            Column {
                InfoRow(label = "Serveur Cible VLESS", value = "$sAddress:$sPort", isMono = true)
                InfoRow(label = "UID / UUID VLESS", value = sUuid, isMono = true)
                InfoRow(label = "SNI TLS", value = sSni, isMono = true)
                InfoRow(label = "Host Header", value = sHost, isMono = true)
                InfoRow(label = "Chemin WebSocket", value = sPath, isMono = true)
                InfoRow(label = "Protocole Tunnel", value = "VLESS • WebSocket • TLS • Mux")

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            editUuidText = sUuid
                            showEditUuidDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexusCyan),
                        modifier = Modifier.weight(1f).testTag("edit_vless_uuid_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Modifier l'UID", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexusCyan),
                        modifier = Modifier.weight(1f).testTag("import_vless_button")
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importer Lien", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showXrayJsonDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NexusCyan),
                    modifier = Modifier.fillMaxWidth().testTag("view_xray_json_button")
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Configuration JSON Xray-Core Officielle", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 3: COMPTE & SYNCHRONISATION
        SectionHeader(title = "COMPTE & IDENTIFIANT MEMBRE", icon = Icons.Default.Person)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NexusSurface)
                .border(1.dp, NexusCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session?.fullName ?: "Non défini",
                            color = NexusTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "UID : ${FormatUtils.formatMaskedUid(session?.uid)}",
                            color = NexusCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Pays : ${session?.countryFlag ?: "🌍"} ${session?.countryName ?: "---"} (${session?.countryCode ?: "--"})",
                            color = NexusTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 2: CONFIGURATION DU TUNNEL & RÉSEAU
        SectionHeader(title = "SÉCURITÉ DU TUNNEL & DNS", icon = Icons.Default.Security)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NexusSurface)
                .border(1.dp, NexusCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                // DNS Settings
                Text(text = "Serveurs DNS Sécurisés", color = NexusTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = dnsPrimary,
                        onValueChange = { dnsPrimary = it },
                        label = { Text("DNS 1", fontSize = 11.sp) },
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
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dnsSecondary,
                        onValueChange = { dnsSecondary = it },
                        label = { Text("DNS 2", fontSize = 11.sp) },
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
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // MTU
                Text(text = "Taille MTU (Paquets réseau)", color = NexusTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = mtuText,
                    onValueChange = { mtuText = it.filter { char -> char.isDigit() } },
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

                Spacer(modifier = Modifier.height(16.dp))

                // Kill Switch Toggle
                SettingSwitch(
                    title = "Kill-Switch (Bloqueur de fuite)",
                    subtitle = "Couper Internet si le tunnel sécurisé est interrompu",
                    checked = killSwitch,
                    onCheckedChange = { killSwitch = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bypass LAN Toggle
                SettingSwitch(
                    title = "Contourner le Réseau Local (LAN)",
                    subtitle = "Accéder aux appareils et imprimantes locales directement",
                    checked = bypassLan,
                    onCheckedChange = { bypassLan = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Auto Connect on Boot
                SettingSwitch(
                    title = "Connexion Automatique au Démarrage",
                    subtitle = "Démarrer la protection dès l'allumage du téléphone",
                    checked = autoBoot,
                    onCheckedChange = { autoBoot = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Status Notification Stats
                SettingSwitch(
                    title = "Statistiques dans la Notification",
                    subtitle = "Afficher les mégaoctets dans la barre supérieure",
                    checked = showNotifStats,
                    onCheckedChange = { showNotifStats = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Preferences Button
        Button(
            onClick = {
                val parsedMtu = mtuText.toIntOrNull() ?: 1500
                val newPrefs = preferences.copy(
                    dnsPrimary = dnsPrimary.trim(),
                    dnsSecondary = dnsSecondary.trim(),
                    mtu = parsedMtu.coerceIn(1280, 1500),
                    killSwitchEnabled = killSwitch,
                    bypassLan = bypassLan,
                    autoConnectOnBoot = autoBoot,
                    showNotificationStats = showNotifStats
                )
                onSavePreferences(newPrefs)
                Toast.makeText(context, "Préférences enregistrées avec succès.", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NexusCyan, contentColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_preferences_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ENREGISTRER LES PARAMÈTRES", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: APPAREIL & SÉCURITÉ MATÉRIELLE
        SectionHeader(title = "IDENTIFICATION MATÉRIELLE SÉCURISÉE", icon = Icons.Default.Fingerprint)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NexusSurface)
                .border(1.dp, NexusCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                InfoRow(label = "Empreinte Matérielle", value = deviceInfo.deviceId, isMono = true)
                InfoRow(label = "Modèle Téléphone", value = "${deviceInfo.manufacturer} ${deviceInfo.model}")
                InfoRow(label = "Version Android", value = deviceInfo.androidVersion)
                InfoRow(label = "Chiffrement Stockage", value = "AES-256-GCM (Android KeyStore)", isMono = true)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Logout
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
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = NexusRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Réinitialiser le profil / Déconnexion", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = NexusSurface,
            title = {
                Text("Importer un lien VLESS", color = NexusTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Collez ci-dessous votre lien vless:// ou votre configuration JSON (VLESS/Xray) :",
                        color = NexusTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        placeholder = { Text("vless://...", fontSize = 11.sp, color = NexusTextMuted) },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("vless_dialog_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NexusSurfaceVariant,
                            unfocusedContainerColor = NexusSurfaceVariant,
                            focusedBorderColor = NexusCyan,
                            unfocusedBorderColor = NexusCardBorder,
                            focusedTextColor = NexusTextPrimary,
                            unfocusedTextColor = NexusTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importInputText.isNotBlank()) {
                            onImportVless?.invoke(importInputText.trim())
                            showImportDialog = false
                            importInputText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusCyan, contentColor = Color.Black)
                ) {
                    Text("Importer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Annuler", color = NexusTextSecondary)
                }
            }
        )
    }

    if (showEditUuidDialog) {
        AlertDialog(
            onDismissRequest = { showEditUuidDialog = false },
            containerColor = NexusSurface,
            title = {
                Text(
                    text = "Modifier l'UID VLESS",
                    color = NexusTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Entrez l'UID / UUID de votre compte attribué par le Web Panel pour le handshake VLESS :",
                        color = NexusTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editUuidText,
                        onValueChange = { editUuidText = it },
                        placeholder = { Text("Ex: 83d5a4b2-a45b-48c1-9012-3456789abcde", fontSize = 12.sp, color = NexusTextMuted) },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("edit_uuid_dialog_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NexusSurfaceVariant,
                            unfocusedContainerColor = NexusSurfaceVariant,
                            focusedBorderColor = NexusCyan,
                            unfocusedBorderColor = NexusCardBorder,
                            focusedTextColor = NexusTextPrimary,
                            unfocusedTextColor = NexusTextPrimary
                        )
                    )
                    if (session?.uid != null && session.uid != editUuidText) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { editUuidText = session.uid },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Utiliser l'UID du compte", color = NexusCyan, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editUuidText.trim()
                        if (trimmed.isNotBlank()) {
                            val cur = serverConfig ?: DynamicServerConfig()
                            val updated = cur.copy(uuid = trimmed)
                            onSaveServerConfig?.invoke(updated)
                            Toast.makeText(context, "UID VLESS mis à jour !", Toast.LENGTH_SHORT).show()
                            showEditUuidDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusCyan, contentColor = Color.Black)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUuidDialog = false }) {
                    Text("Annuler", color = NexusTextSecondary)
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = NexusSurface,
            title = {
                Text("Confirmation de Réinitialisation", color = NexusTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Voulez-vous supprimer les identifiants chiffrés de cette session ? Vous devrez saisir un nouveau token pour vous reconnecter.",
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

    if (showXrayJsonDialog) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val effectiveConfig = serverConfig ?: DynamicServerConfig(uuid = session?.uid)
        val xrayJson = com.example.vpn.VlessConfigParser.generateXrayJsonConfig(effectiveConfig)

        AlertDialog(
            onDismissRequest = { showXrayJsonDialog = false },
            containerColor = NexusSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = NexusCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configuration Xray-Core", color = NexusTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        "JSON officiel standard Xray-core / v2ray (Inbounds socks 10808 + http 10809, Outbound VLESS TLS WS Mux) :",
                        color = NexusTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NexusSurfaceVariant)
                            .border(1.dp, NexusCardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        val jsonScroll = rememberScrollState()
                        Text(
                            text = xrayJson,
                            color = NexusCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.verticalScroll(jsonScroll)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(xrayJson))
                        Toast.makeText(context, "JSON Xray copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusCyan, contentColor = Color.Black)
                ) {
                    Text("Copier JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showXrayJsonDialog = false }) {
                    Text("Fermer", color = NexusTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
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
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text = title, color = NexusTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = NexusTextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
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
private fun InfoRow(label: String, value: String, isMono: Boolean = false) {
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
