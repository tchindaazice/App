package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexusColorScheme = darkColorScheme(
    primary = NexusCyan,
    onPrimary = Color(0xFF04101E),
    primaryContainer = Color(0xFF0C2E46),
    onPrimaryContainer = Color(0xFFB3F5FF),

    secondary = NexusTeal,
    onSecondary = Color(0xFF03261C),
    secondaryContainer = Color(0xFF0D3D30),
    onSecondaryContainer = Color(0xFFA7F3D0),

    tertiary = NexusPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2E1C5A),
    onTertiaryContainer = Color(0xFFE9D8FD),

    background = NexusDarkBg,
    onBackground = NexusTextPrimary,

    surface = NexusSurface,
    onSurface = NexusTextPrimary,
    surfaceVariant = NexusSurfaceVariant,
    onSurfaceVariant = NexusTextSecondary,

    outline = NexusCardBorder,
    outlineVariant = Color(0xFF1B2844),

    error = NexusRed,
    onError = Color.White,
    errorContainer = Color(0xFF4C1014),
    onErrorContainer = Color(0xFFFFD2D4)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NexusColorScheme,
        typography = Typography,
        content = content
    )
}
