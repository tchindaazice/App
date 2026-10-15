package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AfricanCountries
import com.example.data.model.Country
import com.example.data.model.VpnStatus
import com.example.ui.theme.NexusAmber
import com.example.ui.theme.NexusCardBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGlowCyan
import com.example.ui.theme.NexusGlowGreen
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextMuted
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CountrySelector(
    selectedCountry: Country,
    onCountrySelected: (Country) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            AfricanCountries.list
        } else {
            AfricanCountries.list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.dialCode.contains(searchQuery) ||
                it.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NexusSurfaceVariant)
            .border(1.dp, NexusCardBorder, RoundedCornerShape(12.dp))
            .testTag("country_selector_trigger")
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Drapeau National Pro & Coloré
                Text(
                    text = selectedCountry.flagEmoji,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = selectedCountry.name,
                        color = NexusTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Indicatif : ${selectedCountry.dialCode} • ${selectedCountry.code}",
                        color = NexusTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Changer de pays",
                tint = NexusCyan
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                searchQuery = ""
            },
            containerColor = NexusSurface,
            title = {
                Column {
                    Text(
                        text = "Sélectionner votre pays",
                        color = NexusTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Rechercher...", color = NexusTextMuted, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NexusCyan) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Effacer", tint = NexusTextSecondary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexusCyan,
                            unfocusedBorderColor = NexusCardBorder,
                            focusedTextColor = NexusTextPrimary,
                            unfocusedTextColor = NexusTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("country_search_input")
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    items(filteredCountries, key = { it.code }) { country ->
                        val isSelected = country.code == selectedCountry.code
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NexusSurfaceVariant else Color.Transparent)
                                .clickable {
                                    onCountrySelected(country)
                                    showDialog = false
                                    searchQuery = ""
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = country.flagEmoji,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = country.name,
                                    color = if (isSelected) NexusCyan else NexusTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "${country.dialCode} • ${country.code}",
                                    color = NexusTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NexusCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    searchQuery = ""
                }) {
                    Text("Fermer", color = NexusCyan)
                }
            }
        )
    }
}

@Composable
fun QuotaGauge(
    usedBytes: Long,
    totalBytes: Long,
    expirationTimestamp: Long,
    modifier: Modifier = Modifier
) {
    val progress = if (totalBytes > 0) {
        (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val remainingBytes = (totalBytes - usedBytes).coerceAtLeast(0L)
    val usedGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
    val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
    val remainingGb = remainingBytes / (1024.0 * 1024.0 * 1024.0)
    val percentage = (progress * 100).toInt()

    val expiryDateFormat = SimpleDateFormat("d MMM yyyy", Locale.FRENCH)
    val formattedExpiry = expiryDateFormat.format(Date(expirationTimestamp))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(NexusSurfaceVariant, NexusSurface)
                )
            )
            .border(1.dp, NexusCardBorder, RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (remainingBytes > 0) NexusGreen else NexusRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONSOMMATION DE DONNÉES",
                        color = NexusTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    color = NexusGlowCyan,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Expire le $formattedExpiry",
                        color = NexusCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Radial gauge ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(175.dp)
            ) {
                Canvas(modifier = Modifier.size(165.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    drawCircle(
                        color = Color(0xFF162540),
                        style = Stroke(width = strokeWidth)
                    )
                    val sweepAngle = progress * 360f
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(NexusCyan, NexusGreen, NexusCyan)
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.US, "%.1f Go", remainingGb),
                        color = NexusTextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Restants",
                        color = NexusCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f Go utilisés sur %.0f Go", usedGb, totalGb),
                        color = NexusTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuotaSummaryColumn(
                    label = "Utilisé",
                    value = String.format(Locale.US, "%.2f Go", usedGb),
                    color = NexusTextPrimary
                )
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(NexusCardBorder)
                )
                QuotaSummaryColumn(
                    label = "Forfait Total",
                    value = String.format(Locale.US, "%.0f Go", totalGb),
                    color = NexusCyan
                )
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(NexusCardBorder)
                )
                QuotaSummaryColumn(
                    label = "Consommé",
                    value = "$percentage%",
                    color = if (percentage < 85) NexusGreen else NexusAmber
                )
            }
        }
    }
}

@Composable
private fun QuotaSummaryColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = NexusTextSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun VpnPowerButton(
    status: VpnStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val (glowColor, buttonColor, statusText) = when (status) {
        VpnStatus.CONNECTED -> Triple(NexusGlowGreen, NexusGreen, "Connecté")
        VpnStatus.CONNECTING -> Triple(NexusGlowCyan, NexusCyan, "Vérification...")
        VpnStatus.DISCONNECTING -> Triple(NexusGlowCyan, NexusAmber, "Déconnexion...")
        VpnStatus.ERROR -> Triple(Color(0x33EF4444), NexusRed, "Erreur")
        VpnStatus.DISCONNECTED -> Triple(Color.Transparent, Color(0xFF1E2D4A), "Déconnecté")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(150.dp)
        ) {
            if (status == VpnStatus.CONNECTED || status == VpnStatus.CONNECTING) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(glowColor)
                )
            }

            Box(
                modifier = Modifier
                    .size(126.dp)
                    .clip(CircleShape)
                    .background(NexusSurfaceVariant)
                    .border(3.dp, buttonColor.copy(alpha = 0.8f), CircleShape)
            )

            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = if (status == VpnStatus.CONNECTED) NexusGreen else NexusSurface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(110.dp)
                    .testTag("vpn_power_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (status == VpnStatus.CONNECTING) {
                        CircularProgressIndicator(
                            color = NexusCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(54.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Activer VPN",
                            tint = if (status == VpnStatus.CONNECTED) Color(0xFF03261C) else buttonColor,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            color = buttonColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, buttonColor.copy(alpha = 0.4f))
        ) {
            Text(
                text = statusText,
                color = buttonColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun MetricTile(
    icon: ImageVector,
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(NexusSurfaceVariant)
            .border(1.dp, NexusCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    color = NexusTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = accentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
