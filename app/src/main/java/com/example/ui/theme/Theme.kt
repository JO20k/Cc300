package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TeleVegasDarkColorScheme = darkColorScheme(
    primary = TeleCyan,
    onPrimary = Color(0xFF001E2F),
    primaryContainer = Color(0xFF0A3450),
    onPrimaryContainer = Color(0xFFB3E5FC),
    secondary = NeonEmerald,
    onSecondary = Color(0xFF00210E),
    secondaryContainer = Color(0xFF063D22),
    onSecondaryContainer = Color(0xFFB9F6CA),
    tertiary = VegasGold,
    onTertiary = Color(0xFF261900),
    tertiaryContainer = Color(0xFF422C00),
    onTertiaryContainer = Color(0xFFFFE082),
    error = CrimsonAlert,
    onError = Color.White,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TeleVegasDarkColorScheme,
        typography = Typography,
        content = content
    )
}
