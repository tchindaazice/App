package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AfricanCountries
import com.example.data.model.CommunicationChannel
import com.example.data.model.Country
import com.example.data.model.DeviceInfo
import com.example.ui.components.CountrySelector
import com.example.ui.theme.NexusAmber
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    deviceInfo: DeviceInfo,
    isSubmitting: Boolean,
    onSubmit: (fullName: String, country: Country, channel: CommunicationChannel, contact: String) -> Unit,
    onActivateToken: (String) -> Unit,
    onSimulateAdminApproval: () -> Unit
) {
    var selectedCountry by remember { mutableStateOf(AfricanCountries.default()) }
    var selectedChannel by remember { mutableStateOf(CommunicationChannel.WHATSAPP) }
    var channelDropdownExpanded by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var contactDetail by remember { mutableStateOf("") }

    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Cyber Header Brand
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(NexusCyan.copy(alpha = 0.25f), NexusSurfaceVariant)
                    )
                )
                .border(2.dp, NexusCyan, RoundedCornerShape(22.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = NexusCyan,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "NEXUS TUNNEL VPN",
            color = NexusCyan,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
        )

        Text(
            text = "Protocole VLESS Pro • Accès Sécurisé Afrique",
            color = NexusTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Device Auto-binding Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NexusSurface)
                .border(1.dp, NexusCardBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = NexusCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Appareil détecté & sécurisé",
                        color = NexusTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${deviceInfo.manufacturer} ${deviceInfo.model} • ${deviceInfo.androidVersion}",
                        color = NexusTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "ID: ${deviceInfo.deviceId}",
                        color = NexusCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = NexusGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // FORM FIELDS
        // 1. Choix du pays actuel (Obligatoire)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🌍", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Choisir son pays actuel (Afrique) *",
                    color = NexusTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            CountrySelector(
                selectedCountry = selectedCountry,
                onCountrySelected = { selectedCountry = it }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Titre Moyen de communication (Liste déroulante WhatsApp / Telegram)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📡", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Moyen de communication *",
                    color = NexusTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = channelDropdownExpanded,
                onExpandedChange = { channelDropdownExpanded = !channelDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "${selectedChannel.iconEmoji}  ${selectedChannel.label}",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = channelDropdownExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NexusSurfaceVariant,
                        unfocusedContainerColor = NexusSurfaceVariant,
                        focusedBorderColor = NexusCyan,
                        unfocusedBorderColor = NexusCardBorder,
                        focusedTextColor = NexusTextPrimary,
                        unfocusedTextColor = NexusTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("channel_dropdown_field")
                )

                ExposedDropdownMenu(
                    expanded = channelDropdownExpanded,
                    onDismissRequest = { channelDropdownExpanded = false },
                    modifier = Modifier.background(NexusSurface)
                ) {
                    CommunicationChannel.entries.forEach { channel ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(channel.iconEmoji, fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
                                    Text(channel.label, color = NexusTextPrimary, fontSize = 14.sp)
                                }
                            },
                            onClick = {
                                selectedChannel = channel
                                channelDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Contact Input (WhatsApp number or Telegram username)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (selectedChannel == CommunicationChannel.WHATSAPP) "💬" else "✈️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedChannel == CommunicationChannel.WHATSAPP) "Numéro WhatsApp *" else "Nom d'utilisateur Telegram (@username) *",
                    color = NexusTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = contactDetail,
                onValueChange = { contactDetail = it },
                placeholder = {
                    Text(
                        text = if (selectedChannel == CommunicationChannel.WHATSAPP)
                            "Ex: ${selectedCountry.dialCode} 6XXXXXXXX"
                        else
                            "@mon_pseudo_telegram",
                        color = NexusTextMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (selectedChannel == CommunicationChannel.WHATSAPP) Icons.Default.Phone else Icons.Default.Person,
                        contentDescription = null,
                        tint = NexusCyan
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (selectedChannel == CommunicationChannel.WHATSAPP) KeyboardType.Phone else KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NexusSurfaceVariant,
                    unfocusedContainerColor = NexusSurfaceVariant,
                    focusedBorderColor = NexusCyan,
                    unfocusedBorderColor = NexusCardBorder,
                    focusedTextColor = NexusTextPrimary,
                    unfocusedTextColor = NexusTextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_detail_input")
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Nom complet d'utilisateur
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "👤", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nom complet d'utilisateur *",
                    color = NexusTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = { Text("Ex: Jean Dupont", color = NexusTextMuted, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NexusCyan) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NexusSurfaceVariant,
                    unfocusedContainerColor = NexusSurfaceVariant,
                    focusedBorderColor = NexusCyan,
                    unfocusedBorderColor = NexusCardBorder,
                    focusedTextColor = NexusTextPrimary,
                    unfocusedTextColor = NexusTextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("full_name_input")
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Bouton Soumettre la demande
        Button(
            onClick = {
                onSubmit(fullName, selectedCountry, selectedChannel, contactDetail)
            },
            enabled = !isSubmitting && fullName.isNotBlank() && contactDetail.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NexusCyan,
                contentColor = Color(0xFF070B14),
                disabledContainerColor = NexusCardBorder,
                disabledContentColor = NexusTextMuted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_registration_button")
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Transmission au Web Panel...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            } else {
                Text(
                    text = "SOUMETTRE LA DEMANDE D'ADHÉSION 🚀",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Direct Token input option
        OutlinedButton(
            onClick = { showTokenDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusCyan),
            border = androidx.compose.foundation.BorderStroke(1.dp, NexusCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("open_token_dialog_button")
        ) {
            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("J'ai déjà un Token Admin / Revendeur", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick simulation mode for testing
        TextButton(
            onClick = onSimulateAdminApproval,
            modifier = Modifier.testTag("simulate_approval_button")
        ) {
            Text(
                text = "⚡ Tester en mode Démo (Approbation Immédiate)",
                color = NexusAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Modal Dialog for Manual Token Activation
    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            containerColor = NexusSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = NexusCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Activer un Token d'accès",
                        color = NexusTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Entrez le token personnalisé généré par l'administrateur ou revendeur du Web Panel Nexus :",
                        color = NexusTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = { Text("Ex: NX-VLESS-829A-PRO", color = NexusTextMuted, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexusCyan,
                            unfocusedBorderColor = NexusCardBorder,
                            focusedTextColor = NexusTextPrimary,
                            unfocusedTextColor = NexusTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_token_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tokenInput.isNotBlank()) {
                            onActivateToken(tokenInput)
                            showTokenDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexusCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Activer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) {
                    Text("Annuler", color = NexusTextSecondary)
                }
            }
        )
    }
}
