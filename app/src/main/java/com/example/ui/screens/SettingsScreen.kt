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
import com.example.data.model.DeviceInfo
import com.example.data.model.UserSession
import com.example.data.model.VpnPreferences
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
    deviceInfo: DeviceInfo,
    isSyncing: Boolean,
    onSavePreferences: (VpnPreferences) -> Unit,
    onSyncAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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

        // SECTION 1: GESTION DU COMPTE & SYNCHRONISATION
        SectionHeader(title = "COMPTE & SYNCHRONISATION WEB PANEL", icon = Icons.Default.Person)
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
                            text = "UID : ${session?.uid ?: "---"}",
                            color = NexusCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Pays : ${session?.countryName ?: "---"} (${session?.countryCode ?: "--"})",
                            color = NexusTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onSyncAccount,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexusCyan.copy(alpha = 0.5f))
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(color = NexusCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Actualiser", fontSize = 12.sp)
                        }
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
